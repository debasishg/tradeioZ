package tradex.domain
package repository

import zio._
import model.instrument._

trait InstrumentRepository {

  /** query by account number */
  def queryByISINCode(isin: ISINCode): Task[Option[Instrument]]

  /** query by instrument type Equity / FI / CCY */
  def queryByInstrumentType(instrumentType: InstrumentType): Task[List[Instrument]]

  /** store */
  def store(ins: Instrument): Task[Instrument]
}

object InstrumentRepository {
  def queryByISINCode(isin: ISINCode): RIO[Has[InstrumentRepository], Option[Instrument]] =
    ZIO.serviceWith[InstrumentRepository](_.queryByISINCode(isin))

  def queryByInstrumentType(instrumentType: InstrumentType): RIO[Has[InstrumentRepository], List[Instrument]] =
    ZIO.serviceWith[InstrumentRepository](_.queryByInstrumentType(instrumentType))

  def store(ins: Instrument): RIO[Has[InstrumentRepository], Instrument] =
    ZIO.serviceWith[InstrumentRepository](_.store(ins))
}
