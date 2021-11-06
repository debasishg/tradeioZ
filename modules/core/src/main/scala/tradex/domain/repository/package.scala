package tradex.domain

import zio._
import java.time.LocalDate

import model.account._
import model.instrument._

package object repository {
  type AccountRepository    = Has[AccountRepository.Service]
  type InstrumentRepository = Has[InstrumentRepository.Service]

  def queryByAccountNo(no: AccountNo): RIO[AccountRepository, Option[Account]] =
    ZIO.accessM(_.get.queryByAccountNo(no))

  def all: RIO[AccountRepository, List[Account]] =
    ZIO.accessM(_.get.all)

  def store(a: Account): RIO[AccountRepository, Account] =
    ZIO.accessM(_.get.store(a))

  def store(as: List[Account]): RIO[AccountRepository, Unit] =
    ZIO.accessM(_.get.store(as))

  def allOpenedOn(openedOn: LocalDate): RIO[AccountRepository, List[Account]] =
    ZIO.accessM(_.get.allOpenedOn(openedOn))

  def allClosed(closeDate: Option[LocalDate]): RIO[AccountRepository, List[Account]] =
    ZIO.accessM(_.get.allClosed(closeDate))

  def allAccountsOfType(accountType: AccountType): RIO[AccountRepository, List[Account]] =
    ZIO.accessM(_.get.allAccountsOfType(accountType))

  def queryByISINCode(isin: ISINCode): RIO[InstrumentRepository, Option[Instrument]] =
    ZIO.accessM(_.get.queryByISINCode(isin))

  def queryByInstrumentType(instrumentType: InstrumentType): RIO[InstrumentRepository, List[Instrument]] =
    ZIO.accessM(_.get.queryByInstrumentType(instrumentType))

  def store(ins: Instrument): RIO[InstrumentRepository, Instrument] =
    ZIO.accessM(_.get.store(ins))
}
