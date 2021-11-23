package tradex.domain
package services.trading

import zio._
import zio.prelude._
import java.time.LocalDate
import model.account._
import model.trade._
import model.order._
import model.market._
import model.execution._
import model.user._

import TradingServiceError._

trait TradingService {

  /** Find accounts opened on the date specified
    *
    * @param openDate
    *   the date when account was opened
    * @return
    *   a list of `Account` under the effect `F`
    */
  def getAccountsOpenedOn(openDate: LocalDate): IO[TradingError, List[Account]]

  /** Find the list of trades for the supplied client account no and (optionally) the trade date.
    *
    * @param forAccountNo
    *   the client accountNo
    * @param forDate
    *   the trade date
    * @return
    *   a list of `Trade` under the effect `F`
    */
  def getTrades(
      forAccountNo: AccountNo,
      forDate: Option[LocalDate] = None
  ): IO[TradingError, List[Trade]]

  /** Create a list of `Order` from client orders that come from the front office.
    *
    * @param frontOfficeOrders
    *   client order
    * @return
    *   a NonEmptyList of `Order` under the effect `F`
    */
  def orders(
      frontOfficeOrders: NonEmptyList[FrontOfficeOrder]
  ): IO[TradingError, NonEmptyList[Order]]

  /** Execute an `Order` in the `Market` and book the execution in the broker account supplied.
    *
    * @param orders
    *   the orders to execute
    * @param market
    *   the market of execution
    * @param brokerAccount
    *   the broker account where the execution will be booked
    * @return
    *   a List of `Execution` generated from the `Order`
    */
  def execute(
      orders: NonEmptyList[Order],
      market: Market,
      brokerAccountNo: AccountNo
  ): IO[TradingError, NonEmptyList[Execution]]

  /** Allocate the `Execution` equally between the client accounts generating a list of `Trade`s.
    *
    * @param executions
    *   the executions to allocate
    * @param clientAccounts
    *   the client accounts for which `Trade` will be generated
    * @return
    *   a list of `Trade`
    */
  def allocate(
      executions: NonEmptyList[Execution],
      clientAccounts: NonEmptyList[AccountNo],
      userId: UserId
  ): IO[TradingError, NonEmptyList[Trade]]

  def generateTrade(
      frontOfficeInput: GenerateTradeFrontOfficeInput,
      userId: UserId
  ): IO[Throwable, NonEmptyList[Trade]] = {

    for {
      orders <- orders(frontOfficeInput.frontOfficeOrders)
      executions <- execute(
        orders,
        frontOfficeInput.market,
        frontOfficeInput.brokerAccountNo
      )
      trades <- allocate(executions, frontOfficeInput.clientAccountNos, userId)
    } yield trades
  }
}

object TradingServiceError {
  sealed trait TradingError extends Throwable {
    def cause: String
  }
  case class OrderingError(cause: String)        extends TradingError
  case class ExecutionError(cause: String)       extends TradingError
  case class AllocationError(cause: String)      extends TradingError
  case class TradeGenerationError(cause: String) extends TradingError
}

object TradingService {
  def getAccountsOpenedOn(openDate: LocalDate): ZIO[Has[TradingService], TradingError, List[Account]] =
    ZIO.serviceWith[TradingService](_.getAccountsOpenedOn(openDate))

  def getTrades(
      forAccountNo: AccountNo,
      forDate: Option[LocalDate] = None
  ): ZIO[Has[TradingService], TradingError, List[Trade]] =
    ZIO.serviceWith[TradingService](_.getTrades(forAccountNo, forDate))

  def orders(
      frontOfficeOrders: NonEmptyList[FrontOfficeOrder]
  ): ZIO[Has[TradingService], TradingError, NonEmptyList[Order]] =
    ZIO.serviceWith[TradingService](_.orders(frontOfficeOrders))

  def execute(
      orders: NonEmptyList[Order],
      market: Market,
      brokerAccountNo: AccountNo
  ): ZIO[Has[TradingService], TradingError, NonEmptyList[Execution]] =
    ZIO.serviceWith[TradingService](_.execute(orders, market, brokerAccountNo))

  def allocate(
      executions: NonEmptyList[Execution],
      clientAccounts: NonEmptyList[AccountNo],
      userId: UserId
  ): ZIO[Has[TradingService], TradingError, NonEmptyList[Trade]] =
    ZIO.serviceWith[TradingService](_.allocate(executions, clientAccounts, userId))

  def generateTrade(
      frontOfficeInput: GenerateTradeFrontOfficeInput,
      userId: UserId
  ): ZIO[Has[TradingService], Throwable, NonEmptyList[Trade]] =
    ZIO.serviceWith[TradingService](_.generateTrade(frontOfficeInput, userId))
}
