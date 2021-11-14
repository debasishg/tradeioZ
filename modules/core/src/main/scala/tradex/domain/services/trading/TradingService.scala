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

object TradingService {
  trait Service {

    /** Find accounts opened on the date specified
      *
      * @param openDate
      *   the date when account was opened
      * @return
      *   a list of `Account` under the effect `F`
      */
    def getAccountsOpenedOn(openDate: LocalDate): Task[List[Account]]

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
    ): Task[List[Trade]]

    /** Create a list of `Order` from client orders read from a stream as a csv file.
      *
      * @param csvOrder
      *   client order in csv format
      * @return
      *   a List of `Order` under the effect `F`
      */
    def orders(csvOrder: String): Task[NonEmptyList[Order]]

    /** Create a list of `Order` from client orders that come from the front office.
      *
      * @param frontOfficeOrders
      *   client order
      * @return
      *   a NonEmptyList of `Order` under the effect `F`
      */
    def orders(
        frontOfficeOrders: NonEmptyList[FrontOfficeOrder]
    ): Task[NonEmptyList[Order]]

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
    ): Task[NonEmptyList[Execution]]

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
    ): Task[NonEmptyList[Trade]]
  }
}
