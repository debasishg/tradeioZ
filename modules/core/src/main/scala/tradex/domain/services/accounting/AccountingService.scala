package tradex.domain
package services.accounting

import zio._
import zio.prelude._
import model.account._
import model.trade._
import model.balance._
import java.time.LocalDate

trait AccountingService {
  def postBalance(trade: Trade): IO[AccountingError, Balance]
  def postBalance(trades: NonEmptyList[Trade]): IO[AccountingError, NonEmptyList[Balance]]
  def getBalance(accountNo: AccountNo): IO[AccountingError, Option[Balance]]
  def getBalanceByDate(date: LocalDate): IO[AccountingError, List[Balance]]
}

object AccountingService {
  def postBalance(trade: Trade): ZIO[Has[AccountingService], AccountingError, Balance] =
    ZIO.serviceWith[AccountingService](_.postBalance(trade))

  def postBalance(trades: NonEmptyList[Trade]): ZIO[Has[AccountingService], AccountingError, NonEmptyList[Balance]] =
    ZIO.serviceWith[AccountingService](_.postBalance(trades))

  def getBalance(accountNo: AccountNo): ZIO[Has[AccountingService], AccountingError, Option[Balance]] =
    ZIO.serviceWith[AccountingService](_.getBalance(accountNo))

  def getBalanceByDate(date: LocalDate): ZIO[Has[AccountingService], AccountingError, List[Balance]] =
    ZIO.serviceWith[AccountingService](_.getBalanceByDate(date))
}
