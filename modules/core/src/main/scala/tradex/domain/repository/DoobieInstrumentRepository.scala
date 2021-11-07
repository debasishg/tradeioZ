package tradex.domain
package repository

import java.time.LocalDateTime
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
import model.instrument._

final class DoobieInstrumentRepository(xa: Transactor[Task]) {
  import DoobieInstrumentRepository.SQL

  val instrumentRepository: InstrumentRepository.Service = new InstrumentRepository.Service {

    def queryByISINCode(isin: ISINCode): Task[Option[Instrument]] =
      SQL
        .get(isin.value.value)
        .option
        .transact(xa)
        .orDie

    def queryByInstrumentType(instrumentType: InstrumentType): Task[List[Instrument]] =
      SQL
        .getByType(instrumentType.entryName)
        .to[List]
        .transact(xa)
        .orDie

    def store(ins: Instrument): Task[Instrument] =
      SQL
        .upsert(ins)
        .run
        .transact(xa)
        .map(_ => ins)
        .orDie
  }
}

object DoobieInstrumentRepository {
  def layer: ZLayer[DbConfigProvider with Blocking, Throwable, InstrumentRepository] = {
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
      } yield new DoobieInstrumentRepository(transactor).instrumentRepository
    }
  }

  object SQL {
    def upsert(instrument: Instrument): Update0 = {
      sql"""
        INSERT INTO instruments
        VALUES (
          ${instrument.isinCode.value.value},
          ${instrument.name.value.value},
          ${instrument.dateOfIssue},
          ${instrument.dateOfMaturity},
          ${instrument.lotSize.value.value},
          ${instrument.unitPrice.map(_.value.value)},
          ${instrument.couponRate.map(_.amount)},
          ${instrument.couponFrequency}
        )
        ON CONFLICT(isinCode) DO UPDATE SET
          name                 = EXCLUDED.name,
          type                 = EXCLUDED.type,
          dateOfIssue          = EXCLUDED.dateOfIssue,
          dateOfMaturity       = EXCLUDED.dateOfMaturity,
          lotSize              = EXCLUDED.lotSize,
          unitPrice            = EXCLUDED.unitPrice,
          couponRate           = EXCLUDED.couponRate,
          couponFrequency      = EXCLUDED.couponFrequency
       """.update
    }

    implicit val instrumentWrite: Write[Instrument] =
      Write[
        (
            String,
            String,
            String,
            Option[LocalDateTime],
            Option[LocalDateTime],
            Int,
            Option[BigDecimal],
            Option[BigDecimal],
            Option[BigDecimal]
        )
      ].contramap(instrument =>
        (
          instrument.isinCode.value.value,
          instrument.name.value.value,
          instrument.instrumentType.entryName,
          instrument.dateOfIssue,
          instrument.dateOfMaturity,
          instrument.lotSize.value.value,
          instrument.unitPrice.map(_.value.value),
          instrument.couponRate.map(_.amount),
          instrument.couponFrequency
        )
      )

    implicit val instrumentRead: Read[Instrument] =
      Read[
        (
            String,
            String,
            String,
            Option[LocalDateTime],
            Option[LocalDateTime],
            Int,
            Option[BigDecimal],
            Option[BigDecimal],
            Option[BigDecimal]
        )
      ].map { case (isin, nm, insType, issueDt, matDt, lot, up, cpnRt, cpnFreq) =>
        Instrument
          .instrument(
            isin,
            nm,
            InstrumentType.withName(insType),
            issueDt,
            matDt,
            Some(lot),
            up,
            cpnRt.map(m => USD(m)),
            cpnFreq
          )
          .fold(exs => throw new Exception(exs.toList.mkString("/")), identity)
      }

    def get(isin: String): Query0[Instrument] = sql"""
      select * from instruments where isinCode = $isin
      """.query[Instrument]

    def getByType(instrumentType: String): Query0[Instrument] = sql"""
      select * from instruments where instrumentType = $instrumentType
      """.query[Instrument]
  }
}
