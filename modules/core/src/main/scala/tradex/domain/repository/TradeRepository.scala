package tradex.domain
package repository

import java.time.LocalDate
import zio._
import zio.prelude.NonEmptyList

import model.account._
import model.trade._
import model.market._

object TradeRepository {
  trait Service {

    /** query by account number and trade date (compares using the date part only) */
    def queryTradeByAccountNo(accountNo: AccountNo, date: LocalDate): Task[List[Trade]]

    /** query by market */
    def queryTradeByMarket(market: Market): Task[List[Trade]]

    /** query all trades */
    def allTrades: Task[List[Trade]]

    /** store */
    def store(trd: Trade): Task[Trade]

    /** store many trades */
    def storeNTrades(trades: NonEmptyList[Trade]): Task[Unit]
  }
}
