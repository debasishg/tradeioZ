package tradex.domain
package repository

import java.time.LocalDate
import zio._
import model.account.AccountNo
import model.balance._

object BalanceRepository {
  trait Service {

    /** query by account number */
    def queryBalanceByAccountNo(no: AccountNo): Task[Option[Balance]]

    /** store */
    def store(a: Balance): Task[Balance]

    /** query all balances that have amount as of this date */
    /** asOf date <= this date */
    def queryBalanceAsOf(date: LocalDate): Task[List[Balance]]

    /** all balances */
    def allBalances: Task[List[Balance]]
  }
}
