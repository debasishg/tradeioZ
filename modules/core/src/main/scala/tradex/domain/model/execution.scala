package tradex.domain
package model

import java.util.UUID
import zio._
import zio.random._
import zio.prelude._

import account._
import instrument._
import order._
import market._
import java.time.LocalDateTime
import io.estatico.newtype.macros.newtype

object execution {
  @newtype
  case class ExecutionReferenceNo(value: UUID)

  // primary domain entity for execution from exchange
  final case class Execution private (
      executionRefNo: ExecutionReferenceNo,
      accountNo: AccountNo,
      orderNo: OrderNo,
      isin: ISINCode,
      market: Market,
      buySell: BuySell,
      unitPrice: UnitPrice,
      quantity: Quantity,
      dateOfExecution: LocalDateTime,
      exchangeExecutionRefNo: Option[String] = None
  )

  // as per layout obtained from exchange
  private[domain] final case class ExchangeExecution private (
      executionRefNo: String,
      accountNo: String,
      orderNo: String,
      isin: String,
      market: String,
      buySell: String,
      unitPrice: BigDecimal,
      quantity: BigDecimal,
      dateOfExecution: LocalDateTime
  )

  object Execution {
    def execution(
        accountNo: AccountNo,
        orderNo: OrderNo,
        isin: ISINCode,
        market: Market,
        buySell: BuySell,
        unitPrice: UnitPrice,
        quantity: Quantity,
        dateOfExecution: LocalDateTime
    ): URIO[Random, Execution] = {
      zio.random.nextUUID
        .map { refNo =>
          Execution(
            ExecutionReferenceNo(refNo),
            accountNo,
            orderNo,
            isin,
            market,
            buySell,
            unitPrice,
            quantity,
            dateOfExecution,
            None
          )
        }
    }

    def execution(
        accountNo: String,
        orderNo: String,
        isin: String,
        market: String,
        buySell: String,
        unitPrice: BigDecimal,
        quantity: BigDecimal,
        dateOfExecution: LocalDateTime
    ): Validation[String, URIO[Random, Execution]] = {
      Validation
        .validateWith(
          Account.validateAccountNo(accountNo),
          Order.validateOrderNo(orderNo),
          Instrument.validateISINCode(isin),
          validateMarket(market),
          Order.validateBuySell(buySell),
          Order.validateUnitPrice(unitPrice),
          Order.validateQuantity(quantity)
        ) { (ano, ono, isin, m, bs, up, qty) =>
          zio.random.nextUUID
            .map { id =>
              Execution(
                ExecutionReferenceNo(id),
                ano,
                ono,
                isin,
                m,
                BuySell.withName(bs),
                up,
                qty,
                dateOfExecution
              )
            }
        }
    }

    // smart constructor from data received from exchange
    private[domain] def createExecution(
        eex: ExchangeExecution
    ): Validation[String, URIO[Random, Execution]] = {
      Validation.validateWith(
        Account.validateAccountNo(eex.accountNo),
        Instrument.validateISINCode(eex.isin),
        Order.validateBuySell(eex.buySell),
        Order.validateUnitPrice(eex.unitPrice),
        Order.validateQuantity(eex.quantity),
        Order.validateOrderNo(eex.orderNo)
      ) { (ano, ins, bs, up, q, ono) =>
        zio.random.nextUUID
          .map { id =>
            Execution(
              ExecutionReferenceNo(id),
              ano,
              ono,
              ins,
              Market.withName(eex.market),
              BuySell.withName(bs),
              up,
              q,
              eex.dateOfExecution,
              if (eex.executionRefNo.isEmpty()) None
              else Some(eex.executionRefNo)
            )
          }
      }
    }
  }

  private[model] def validateMarket(m: String): Validation[String, Market] = {
    Validation
      .fromEither(Market.withNameEither(m))
      .mapError(_ => s"Invalid market $m")
  }
}