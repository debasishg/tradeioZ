package tradex.domain
package repository

import java.time.{ LocalDate, LocalDateTime }
import squants.market._
import zio._
import zio.blocking.Blocking
import zio.interop.catz._
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.postgres.implicits._
import model.account.AccountNo
import model.balance._
import codecs._
import config._

final class DoobieBalanceRepository(xa: Transactor[Task]) {
  import DoobieBalanceRepository.SQL

  val balanceRepository = new BalanceRepository.Service {
    def queryBalanceByAccountNo(no: AccountNo): Task[Option[Balance]] =
      SQL
        .get(no.value.value)
        .option
        .transact(xa)
        .orDie

    def store(b: Balance): Task[Balance] =
      SQL
        .upsert(b)
        .run
        .transact(xa)
        .map(_ => b)
        .orDie

    def queryBalanceAsOf(date: LocalDate): Task[List[Balance]] =
      SQL
        .getAsOf(date)
        .to[List]
        .transact(xa)
        .orDie

    def allBalances: Task[List[Balance]] =
      SQL.getAll
        .to[List]
        .transact(xa)
        .orDie
  }
}

object DoobieBalanceRepository {
  def layer: ZLayer[DbConfigProvider with Blocking, Throwable, BalanceRepository] = {
    import CatzInterop._

    ZLayer.fromManaged {
      for {
        cfg        <- ZIO.access[DbConfigProvider](_.get).toManaged_
        transactor <- mkTransactor(cfg)
      } yield new DoobieBalanceRepository(transactor).balanceRepository
    }
  }

  object SQL {

    // when writing we have a valid `Balance` - hence we can use
    // Scala data types
    implicit val balanceWrite: Write[Balance] =
      Write[
        (
            AccountNo,
            Money,
            Currency,
            LocalDateTime
        )
      ].contramap(balance =>
        (
          balance.accountNo,
          balance.amount,
          balance.currency,
          balance.asOf
        )
      )

    // when reading we can encounter invalid Scala types since
    // data might have been inserted into the database external to the
    // application. Hence we use raw types and a smart constructor that
    // validates the data types
    implicit val balanceRead: Read[Balance] =
      Read[
        (
            String,
            BigDecimal,
            String,
            LocalDateTime
        )
      ].map { case (ano, amt, ccy, asOf) =>
        Balance
          .balance(
            ano,
            Money(amt),
            Currency(ccy).get,
            asOf
          )
          .fold(exs => throw new Exception(exs.toList.mkString("/")), identity)
      }

    def upsert(balance: Balance): Update0 =
      sql"""
        INSERT INTO balance (accountNo, amount, asOf, currency)
        VALUES (
					${balance.accountNo},
					${balance.amount},
					${balance.currency},
					${balance.asOf}
				)
        ON CONFLICT(accountNo) DO UPDATE SET
          amount    = EXCLUDED.amount,
          asOf      = EXCLUDED.asOf,
          currency  = EXCLUDED.currency
       """.update

    def get(no: String): Query0[Balance] = sql"""
      select * from balances b 
			where b.accountNo = $no
      """.query[Balance]

    def getAll: Query0[Balance] = sql"""
      select * from balances
      """.query[Balance]

    def getAsOf(asOf: LocalDate): Query0[Balance] = sql"""
      select * from balances b 
			where DATE(b.asOf) <= $asOf
      """.query[Balance]

  }
}
