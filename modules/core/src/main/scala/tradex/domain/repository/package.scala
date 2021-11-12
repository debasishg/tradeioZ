package tradex.domain

import zio._
import zio.prelude.NonEmptyList
import java.time.LocalDate

import model.account._
import model.instrument._
import model.order._
import model.user._
import model.balance._

package object repository {
  type AccountRepository    = Has[AccountRepository.Service]
  type InstrumentRepository = Has[InstrumentRepository.Service]
  type OrderRepository      = Has[OrderRepository.Service]
  type UserRepository       = Has[UserRepository.Service]
  type BalanceRepository    = Has[BalanceRepository.Service]

  /** AccountRepository */
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

  /** InstrumentRepository */
  def queryByISINCode(isin: ISINCode): RIO[InstrumentRepository, Option[Instrument]] =
    ZIO.accessM(_.get.queryByISINCode(isin))

  def queryByInstrumentType(instrumentType: InstrumentType): RIO[InstrumentRepository, List[Instrument]] =
    ZIO.accessM(_.get.queryByInstrumentType(instrumentType))

  def store(ins: Instrument): RIO[InstrumentRepository, Instrument] =
    ZIO.accessM(_.get.store(ins))

  /** OrderRepository */
  def queryByOrderNo(no: OrderNo): RIO[OrderRepository, Option[Order]] =
    ZIO.accessM(_.get.queryByOrderNo(no))

  def queryByOrderDate(date: LocalDate): RIO[OrderRepository, List[Order]] =
    ZIO.accessM(_.get.queryByOrderDate(date))

  def store(ord: Order): RIO[OrderRepository, Order] =
    ZIO.accessM(_.get.store(ord))

  def store(orders: NonEmptyList[Order]): RIO[OrderRepository, Unit] =
    ZIO.accessM(_.get.store(orders))

  /** UserRepository */
  def queryByUserName(username: UserName): RIO[UserRepository, Option[User]] =
    ZIO.accessM(_.get.queryByUserName(username))

  def store(username: UserName, password: EncryptedPassword): RIO[UserRepository, UserId] =
    ZIO.accessM(_.get.store(username, password))

  /** BalanceRepository */
  def queryBalanceByAccountNo(no: AccountNo): RIO[BalanceRepository, Option[Balance]] =
    ZIO.accessM(_.get.queryBalanceByAccountNo(no))

  def store(b: Balance): RIO[BalanceRepository, Balance] =
    ZIO.accessM(_.get.store(b))

  def queryBalanceAsOf(date: LocalDate): RIO[BalanceRepository, List[Balance]] =
    ZIO.accessM(_.get.queryBalanceAsOf(date))

  def allBalances: RIO[BalanceRepository, List[Balance]] =
    ZIO.accessM(_.get.allBalances)
}
