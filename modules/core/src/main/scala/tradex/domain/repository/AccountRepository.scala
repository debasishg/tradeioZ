package tradex.domain
package repository

import java.time.LocalDate
import zio._
import model.account._

object AccountRepository {
  trait Service {

    /** query by account number */
    def query(no: AccountNo): Task[Option[Account]]

    /** store */
    def store(a: Account): Task[Account]

    /** query by opened date */
    def query(openedOnDate: LocalDate): Task[List[Account]]

    /** all accounts */
    def all: Task[List[Account]]

    /** all closed accounts, if date supplied then all closed after that date */
    def allClosed(closeDate: Option[LocalDate]): Task[List[Account]]

    /** all accounts trading / settlement / both */
    def allAccountsOfType(accountType: AccountType): Task[List[Account]]
  }
}
