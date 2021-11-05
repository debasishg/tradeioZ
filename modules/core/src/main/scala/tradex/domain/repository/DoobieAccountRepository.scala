package tradex.domain
package repository

import java.time.{ LocalDate, LocalDateTime }
import squants.market._
import zio._
import zio.blocking.Blocking
import zio.interop.catz._

import doobie._
import doobie.hikari._
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.postgres.implicits._

import config._
import model.account._

final class DoobieAccountRepository(xa: Transactor[Task]) {
  import DoobieAccountRepository.SQL

  val accountRepository: AccountRepository.Service = new AccountRepository.Service {

    def all: Task[List[Account]] =
      SQL.getAll
        .to[List]
        .transact(xa)
        .orDie

    def query(no: AccountNo): Task[Option[Account]] =
      SQL
        .get(no.value.value)
        .option
        .transact(xa)
        .orDie

    def store(a: Account): Task[Account] =
      SQL
        .upsert(a)
        .run
        .transact(xa)
        .map(_ => a)
        .orDie

    def query(openedOnDate: LocalDate): Task[List[Account]] =
      SQL
        .getByDateOfOpen(openedOnDate)
        .to[List]
        .transact(xa)
        .orDie

    def allClosed(closeDate: Option[LocalDate]): Task[List[Account]] =
      closeDate
        .map { cd =>
          SQL.getAllClosedAfter(cd).to[List].transact(xa).orDie
        }
        .getOrElse(SQL.getAllClosed.to[List].transact(xa).orDie)

    def allAccountsOfType(accountType: AccountType): Task[List[Account]] =
      SQL
        .getByType(accountType.entryName)
        .to[List]
        .transact(xa)
        .orDie
  }
}

object DoobieAccountRepository {
  def layer: ZLayer[DbConfigProvider with Blocking, Throwable, AccountRepository] = {
    import zio.interop.catz.implicits._

    implicit val zioRuntime: zio.Runtime[zio.ZEnv] = zio.Runtime.default

    implicit val dispatcher: cats.effect.std.Dispatcher[zio.Task] =
      zioRuntime
        .unsafeRun(
          cats.effect.std
            .Dispatcher[zio.Task]
            .allocated
        )
        ._1

    def mkTransactor(cfg: DBConfig): ZManaged[Blocking, Throwable, HikariTransactor[Task]] =
      for {
        rt <- ZIO.runtime[Any].toManaged_
        xa <-
          HikariTransactor
            .newHikariTransactor[Task](
              cfg.driver,
              cfg.url,
              cfg.user,
              cfg.password,
              rt.platform.executor.asEC
            )
            .toManaged
      } yield xa

    ZLayer.fromManaged {
      for {
        cfg        <- ZIO.access[DbConfigProvider](_.get).toManaged_
        transactor <- mkTransactor(cfg)
      } yield new DoobieAccountRepository(transactor).accountRepository
    }
  }

  object SQL {
    def upsert(account: Account): Update0 = {
      sql"""
        INSERT INTO accounts
        VALUES (
          ${account.no.value.value}, 
          ${account.name.value.value}, 
          ${account.dateOfOpen}, 
          ${account.dateOfClose}, 
          ${account.accountType.entryName}, 
          ${account.baseCurrency.name}, 
          ${account.tradingCurrency.map(_.name)}, 
          ${account.settlementCurrency.map(_.name)}
        )
        ON CONFLICT(no) DO UPDATE SET
          name                 = EXCLUDED.name,
          type                 = EXCLUDED.type,
          dateOfOpen           = EXCLUDED.dateOfOpen,
          dateOfClose          = EXCLUDED.dateOfClose,
          baseCurrency         = EXCLUDED.baseCurrency,
          tradingCurrency      = EXCLUDED.tradingCurrency,
          settlementCurrency   = EXCLUDED.settlementCurrency
       """.update
    }

    implicit val accountRead: Read[Account] =
      Read[
        (
            String,
            String,
            LocalDateTime,
            Option[LocalDateTime],
            String,
            String,
            Option[String],
            Option[String]
        )
      ].map { case (no, nm, openDt, closeDt, acType, bc, tc, sc) =>
        AccountType.withName(acType) match {
          case AccountType.Trading =>
            Account
              .tradingAccount(
                no,
                nm,
                Some(openDt),
                closeDt,
                Currency.apply(bc).get,
                tc.map(c => Currency.apply(c).get).getOrElse(USD)
              )
              .fold(exs => throw new Exception(exs.toList.mkString("/")), identity)
          case AccountType.Settlement =>
            Account
              .settlementAccount(
                no,
                nm,
                Some(openDt),
                closeDt,
                Currency.apply(bc).get,
                sc.map(c => Currency.apply(c).get).getOrElse(USD)
              )
              .fold(exs => throw new Exception(exs.toList.mkString("/")), identity)
          case AccountType.Both =>
            Account
              .tradingAndSettlementAccount(
                no,
                nm,
                Some(openDt),
                closeDt,
                Currency.apply(bc).get,
                tc.map(c => Currency.apply(c).get).getOrElse(USD),
                sc.map(c => Currency.apply(c).get).getOrElse(USD)
              )
              .fold(exs => throw new Exception(exs.toList.mkString("/")), identity)
        }
      }

    def get(no: String): Query0[Account] = sql"""
      select * from accounts where no = $no
      """.query[Account]

    def getAll: Query0[Account] = sql"""
      select * from accounts
      """.query[Account]

    def getByDateOfOpen(openDate: LocalDate): Query0[Account] = sql"""
      select * from accounts where dateOfOpen = $openDate
      """.query[Account]

    def getAllClosed: Query0[Account] = sql"""
      select * from accounts where dateOfClose is not null
      """.query[Account]

    def getAllClosedAfter(date: LocalDate): Query0[Account] = sql"""
      select * from accounts where dateOfClose > $date
      """.query[Account]

    def getByType(accountType: String): Query0[Account] = sql"""
      select * from accounts where accountType = $accountType
      """.query[Account]
  }
}
