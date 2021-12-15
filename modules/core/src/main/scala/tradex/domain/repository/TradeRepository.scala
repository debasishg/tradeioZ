package tradex.domain
package repository

import java.time.LocalDate
import zio._
import zio.prelude.NonEmptyList

import model.account._
import model.trade._
import model.market._
import model.instrument._

trait TradeRepository {

  /** query by account number and trade date (compares using the date part only) */
  def queryTradeByAccountNo(accountNo: AccountNo, date: LocalDate): Task[List[Trade]]

  /** query by market */
  def queryTradeByMarket(market: Market): Task[List[Trade]]

  /** Find the list of trades executed on the specified date and for the specified instruments. */
  def queryTradesByISINCodes(
      forDate: LocalDate,
      forIsins: Set[ISINCode]
  ): Task[List[Trade]]

  /** query all trades */
  def allTrades: Task[List[Trade]]

  /** store */
  def store(trd: Trade): Task[Trade]

  /** store many trades */
  def storeNTrades(trades: NonEmptyList[Trade]): Task[Unit]
}

object TradeRepository {
  def queryTradeByAccountNo(accountNo: AccountNo, date: LocalDate): RIO[Has[TradeRepository], List[Trade]] =
    ZIO.serviceWith[TradeRepository](_.queryTradeByAccountNo(accountNo, date))

  def queryTradeByMarket(market: Market): RIO[Has[TradeRepository], List[Trade]] =
    ZIO.serviceWith[TradeRepository](_.queryTradeByMarket(market))

  def queryTradesByISINCodes(
      forDate: LocalDate,
      forIsins: Set[ISINCode]
  ): RIO[Has[TradeRepository], List[Trade]] =
    ZIO.serviceWith[TradeRepository](_.queryTradesByISINCodes(forDate, forIsins))

  def allTrades: RIO[Has[TradeRepository], List[Trade]] =
    ZIO.serviceWith[TradeRepository](_.allTrades)

  def store(trd: Trade): RIO[Has[TradeRepository], Trade] =
    ZIO.serviceWith[TradeRepository](_.store(trd))

  def storeNTrades(trades: NonEmptyList[Trade]): RIO[Has[TradeRepository], Unit] =
    ZIO.serviceWith[TradeRepository](_.storeNTrades(trades))
}
