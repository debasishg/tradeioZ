package tradex.domain
package repository

import java.time.LocalDateTime
import squants.market._
import zio._
import zio.blocking.Blocking
import zio.interop.catz._
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.postgres.implicits._
import model.instrument._
import model.order._
import config._
import codecs._

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
    import CatzInterop._

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

    // when writing we have a valid `Instrument` - hence we can use
    // Scala data types
    implicit val instrumentWrite: Write[Instrument] =
      Write[
        (
            ISINCode,
            InstrumentName,
            InstrumentType,
            Option[LocalDateTime],
            Option[LocalDateTime],
            LotSize,
            Option[UnitPrice],
            Option[Money],
            Option[BigDecimal]
        )
      ].contramap(instrument =>
        (
          instrument.isinCode,
          instrument.name,
          instrument.instrumentType,
          instrument.dateOfIssue,
          instrument.dateOfMaturity,
          instrument.lotSize,
          instrument.unitPrice,
          instrument.couponRate,
          instrument.couponFrequency
        )
      )

    // when reading we can encounter invalid Scala types since
    // data might have been inserted into the database external to the
    // application. Hence we use raw types and a smart constructor that
    // validates the data types
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
