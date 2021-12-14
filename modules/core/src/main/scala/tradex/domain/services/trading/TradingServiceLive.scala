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
import TradingServiceError._

final case class TradingServiceLive(
    ar: AccountRepository,
    or: OrderRepository,
    er: ExecutionRepository,
    tr: TradeRepository
) extends TradingService {

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

  def getTradesByISINCodes(forDate: LocalDate, isins: Set[model.instrument.ISINCode]): IO[TradingError, List[Trade]] =
    ???

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

object TradingServiceLive {

  val layer: ZLayer[Has[AccountRepository] with Has[OrderRepository] with Has[ExecutionRepository] with Has[
    TradeRepository
  ], Throwable, Has[TradingService]] = {
    (TradingServiceLive(_, _, _, _)).toLayer
  }
}
