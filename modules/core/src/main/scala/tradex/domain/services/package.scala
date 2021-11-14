package tradex.domain

import zio._
import zio.prelude._
import services.trading._
import TradingService._
import services.accounting._
import AccountingService._
import model.account._
import model.trade._
import model.order._
import model.market._
import model.execution._
import model.user._
import model.balance._
import java.time.LocalDate

package object services {
  type TradingService    = Has[TradingService.Service]
  type AccountingService = Has[AccountingService.Service]

  def getAccountsOpenedOn(openDate: LocalDate): ZIO[TradingService, TradingError, List[Account]] =
    ZIO.accessM(_.get.getAccountsOpenedOn(openDate))

  def getTrades(
      forAccountNo: AccountNo,
      forDate: Option[LocalDate] = None
  ): ZIO[TradingService, TradingError, List[Trade]] = ZIO.accessM(_.get.getTrades(forAccountNo, forDate))

  def orders(
      frontOfficeOrders: NonEmptyList[FrontOfficeOrder]
  ): ZIO[TradingService, TradingError, NonEmptyList[Order]] = ZIO.accessM(_.get.orders(frontOfficeOrders))

  def execute(
      orders: NonEmptyList[Order],
      market: Market,
      brokerAccountNo: AccountNo
  ): ZIO[TradingService, TradingError, NonEmptyList[Execution]] =
    ZIO.accessM(_.get.execute(orders, market, brokerAccountNo))

  def allocate(
      executions: NonEmptyList[Execution],
      clientAccounts: NonEmptyList[AccountNo],
      userId: UserId
  ): ZIO[TradingService, TradingError, NonEmptyList[Trade]] =
    ZIO.accessM(_.get.allocate(executions, clientAccounts, userId))

  def postBalance(trade: Trade): ZIO[AccountingService, AccountingError, Balance] =
    ZIO.accessM(_.get.postBalance(trade))

  def postBalance(trades: NonEmptyList[Trade]): ZIO[AccountingService, AccountingError, NonEmptyList[Balance]] =
    ZIO.accessM(_.get.postBalance(trades))

  def getBalance(accountNo: AccountNo): ZIO[AccountingService, AccountingError, Option[Balance]] =
    ZIO.accessM(_.get.getBalance(accountNo))

  def getBalanceByDate(date: LocalDate): ZIO[AccountingService, AccountingError, List[Balance]] =
    ZIO.accessM(_.get.getBalanceByDate(date))
}
