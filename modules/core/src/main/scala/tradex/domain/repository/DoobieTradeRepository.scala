package tradex.domain
package repository

import java.util.UUID
import java.time.{ LocalDate, LocalDateTime }
import zio._
import zio.prelude.NonEmptyList
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.postgres.implicits._

import squants.market._

import model.account._
import model.instrument._
import model.order._
import model.user._
import model.trade._
import model.market._
import codecs._

final class DoobieTradeRepository(xa: Transactor[Task]) {
  val tradeRepository = new TradeRepository.Service {

    def queryTradeByAccountNo(accountNo: AccountNo, date: LocalDate): Task[List[Trade]] = ???

    def queryTradeByMarket(market: Market): Task[List[Trade]] = ???

    def allTrades: Task[List[Trade]] = ???

    def store(trd: Trade): Task[Trade] = ???

    def storeNTrades(trades: NonEmptyList[Trade]): Task[Unit] = ???
  }
}

object DoobieTradeRepository {
  object SQL {

    // when writing we have a valid `Execution` - hence we can use
    // Scala data types
    implicit val tradeWrite: Write[Trade] =
      Write[
        (
            AccountNo,
            ISINCode,
            Market,
            BuySell,
            UnitPrice,
            Quantity,
            LocalDateTime,
            Option[LocalDateTime],
            Option[Money],
            Option[UserId]
        )
      ].contramap(trade =>
        (
          trade.accountNo,
          trade.isin,
          trade.market,
          trade.buySell,
          trade.unitPrice,
          trade.quantity,
          trade.tradeDate,
          trade.valueDate,
          trade.netAmount,
          trade.userId
        )
      )

    implicit val tradeTaxFeeWrite: Write[TradeTaxFee] =
      Write[
        (TaxFeeId, Money)
      ].contramap(tradeTaxFee => (tradeTaxFee.taxFeeId, tradeTaxFee.amount))

    implicit val tradeTaxFeeRead: Read[(TradeReferenceNo, Trade)] =
      Read[
        (
            TradeReferenceNo,
            AccountNo,
            ISINCode,
            Market,
            BuySell,
            UnitPrice,
            Quantity,
            LocalDateTime,
            Option[LocalDateTime],
            Option[Money],
            Option[UserId],
            TaxFeeId,
            Money
        )
      ].map { case (refNo, ano, isin, mkt, bs, up, qty, td, vd, netAmt, uid, tfid, amt) =>
        (refNo, Trade(ano, isin, mkt, bs, up, qty, td, vd, uid, List(TradeTaxFee(tfid, amt)), netAmt))
      }

    def insertTaxFees(refNo: TradeReferenceNo, taxFees: List[TradeTaxFee]): ConnectionIO[Int] = {
      val sql = s"""
        INSERT INTO tradeTaxFees 
				(
					tradeRefNo,
					taxFeeId,
					amount
				) VALUES (${refNo}, ?, ?)
			"""
      Update[TradeTaxFee](sql).updateMany(taxFees)
    }

    def insertTrade(trade: Trade): ConnectionIO[Int] = {
      val sql = sql"""
			  INSERT INTO trades
				(
          accountNo,
          isinCode,
          market,
          buySellFlag,
          unitPrice,
          quantity,
          tradeDate,
          valueDate,
          netAmount,
          userId
				) VALUES (
					${trade.accountNo},
					${trade.isin},
					${trade.market},
					${trade.buySell},
					${trade.unitPrice},
					${trade.quantity},
					${trade.tradeDate},
					${trade.valueDate},
					${trade.netAmount},
					${trade.userId}
				)
      """
      sql.update
        .withUniqueGeneratedKeys[UUID]("tradeRefNo")
        .flatMap { refNo =>
          insertTaxFees(TradeReferenceNo(refNo), trade.taxFees)
        }
    }
  }
}
