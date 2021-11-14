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
import repository._
import NewtypeRefinedOps._
import scala.util.control.NoStackTrace

object TradingService {
  trait Service {

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
  }

  sealed trait TradingError extends NoStackTrace {
    def cause: String
  }
  case class OrderingError(cause: String)        extends TradingError
  case class ExecutionError(cause: String)       extends TradingError
  case class AllocationError(cause: String)      extends TradingError
  case class TradeGenerationError(cause: String) extends TradingError

  val live = ZLayer.fromServices[
    AccountRepository.Service,
    OrderRepository.Service,
    ExecutionRepository.Service,
    TradeRepository.Service,
    TradingService.Service
  ] { (ar, or, er, tr) =>
    new Service {
      def getAccountsOpenedOn(openDate: LocalDate): IO[TradingError, List[Account]] =
        withTradingService(ar.allOpenedOn(openDate))

      def getTrades(
          forAccountNo: AccountNo,
          forDate: Option[LocalDate] = None
      ): IO[TradingError, List[Trade]] = {
        withTradingService(
          tr.queryTradeByAccountNo(
            forAccountNo,
            forDate.getOrElse(today.toLocalDate())
          )
        )
      }

      def orders(
          frontOfficeOrders: NonEmptyList[FrontOfficeOrder]
      ): IO[TradingError, NonEmptyList[Order]] = {
        withTradingService(
          Order
            .create(frontOfficeOrders)
            .fold(
              errs => IO.fail(OrderingError(errs.toList.mkString("/"))),
              orders =>
                for {
                  os <- IO.succeed(NonEmptyList.fromIterable(orders.head, orders.tail))
                  _  <- persistOrders(os)
                } yield os
            )
        )
      }

      def execute(
          orders: NonEmptyList[Order],
          market: Market,
          brokerAccountNo: AccountNo
      ): IO[TradingError, NonEmptyList[Execution]] = {
        val ois: NonEmptyList[(Order, model.order.LineItem)] = for {
          order <- orders
          item  <- order.items
        } yield (order, item)

        withTradingService {
          for {
            executions <- ois.forEach { case (order, lineItem) =>
              IO.succeed(
                Execution.execution(
                  brokerAccountNo,
                  order.no,
                  lineItem.instrument,
                  market,
                  lineItem.buySell,
                  lineItem.unitPrice,
                  lineItem.quantity,
                  today
                )
              )
            }
            _ <- persistExecutions(executions)
          } yield executions
        }
      }

      def allocate(
          executions: NonEmptyList[Execution],
          clientAccounts: NonEmptyList[AccountNo],
          userId: UserId
      ): IO[TradingError, NonEmptyList[Trade]] = {
        val anoExes: NonEmptyList[(AccountNo, Execution)] = for {
          execution <- executions
          accountNo <- clientAccounts
        } yield (accountNo, execution)

        withTradingService {
          for {
            tradesNoTaxFee <- anoExes.forEach { case (accountNo, execution) =>
              val q = execution.quantity.value.value / clientAccounts.size
              val qty = validate[Quantity](q)
                .fold(errs => throw new Exception(errs.toString), identity)

              Trade
                .trade(
                  accountNo,
                  execution.isin,
                  execution.market,
                  execution.buySell,
                  execution.unitPrice,
                  qty,
                  execution.dateOfExecution,
                  None,
                  userId = Some(userId)
                )
                .fold(errs => IO.fail(AllocationError(errs.toList.mkString("/"))), IO.succeed(_))
            }
            _ <- persistTrades(tradesNoTaxFee)

          } yield tradesNoTaxFee.map(t => Trade.withTaxFee(t))
        }
      }

      private def withTradingService[A](t: Task[A]): IO[TradingError, A] =
        t.foldM(
          error => IO.fail(TradeGenerationError(error.getMessage)),
          success => IO.succeed(success)
        )

      private def persistOrders(orders: NonEmptyList[Order]): IO[OrderingError, Unit] =
        or.store(orders)
          .foldM(
            error => IO.fail(OrderingError(error.getMessage)),
            success => IO.succeed(success)
          )

      private def persistExecutions(executions: NonEmptyList[Execution]): IO[ExecutionError, Unit] =
        er.storeMany(executions)
          .foldM(
            error => IO.fail(ExecutionError(error.getMessage)),
            success => IO.succeed(success)
          )

      private def persistTrades(trades: NonEmptyList[Trade]): IO[TradingError, Unit] =
        tr.storeNTrades(trades)
          .foldM(
            error => IO.fail(TradeGenerationError(error.getMessage)),
            success => IO.succeed(success)
          )
    }
  }
}
