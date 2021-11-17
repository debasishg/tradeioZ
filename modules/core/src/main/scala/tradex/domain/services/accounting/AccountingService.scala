package tradex.domain
package services.accounting

import zio._
import zio.prelude._
import model.account._
import model.trade._
import model.balance._
import java.time.LocalDate
import tradex.domain.repository.BalanceRepository

object AccountingService {
  trait Service {
    def postBalance(trade: Trade): IO[AccountingError, Balance]
    def postBalance(trades: NonEmptyList[Trade]): IO[AccountingError, NonEmptyList[Balance]]
    def getBalance(accountNo: AccountNo): IO[AccountingError, Option[Balance]]
    def getBalanceByDate(date: LocalDate): IO[AccountingError, List[Balance]]
  }
  case class AccountingError(cause: String) extends Throwable

  val live = ZLayer.fromService[BalanceRepository.Service, AccountingService.Service] {
    (br: BalanceRepository.Service) =>
      new Service {
        def postBalance(trade: Trade): IO[AccountingError, Balance] = {
          withAccountingService(
            trade.netAmount
              .map { amt =>
                for {
                  balance <- br.store(
                    Balance(trade.accountNo, amt, amt.currency, today)
                  )
                } yield balance
              }
              .getOrElse(IO.fail(AccountingError(s"No net amount to post for $trade")))
          )
        }

        def postBalance(trades: NonEmptyList[Trade]): IO[AccountingError, NonEmptyList[Balance]] =
          trades.forEach(postBalance)

        def getBalance(accountNo: AccountNo): IO[AccountingError, Option[Balance]] =
          withAccountingService(br.queryBalanceByAccountNo(accountNo))

        def getBalanceByDate(date: LocalDate): IO[AccountingError, List[Balance]] =
          withAccountingService(br.queryBalanceAsOf(date))

        private def withAccountingService[A](t: Task[A]): IO[AccountingError, A] =
          t.foldM(
            error => IO.fail(AccountingError(error.getMessage)),
            success => IO.succeed(success)
          )
      }
  }
}
