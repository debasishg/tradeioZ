package tradex.domain
package repository

import java.time.LocalDate
import zio._
import model.account._

trait AccountRepository {

  /** query by account number */
  def queryByAccountNo(no: AccountNo): Task[Option[Account]]

  /** store */
  def store(a: Account): Task[Account]

  /** store many */
  def store(as: List[Account]): Task[Unit]

  /** query by opened date */
  def allOpenedOn(openedOnDate: LocalDate): Task[List[Account]]

  /** all accounts */
  def all: Task[List[Account]]

  /** all closed accounts, if date supplied then all closed after that date */
  def allClosed(closeDate: Option[LocalDate]): Task[List[Account]]

  /** all accounts trading / settlement / both */
  def allAccountsOfType(accountType: AccountType): Task[List[Account]]
}

object AccountRepository {
  def queryByAccountNo(no: AccountNo): RIO[Has[AccountRepository], Option[Account]] =
    ZIO.serviceWith[AccountRepository](_.queryByAccountNo(no))

  def all: RIO[Has[AccountRepository], List[Account]] =
    ZIO.serviceWith[AccountRepository](_.all)

  def store(a: Account): RIO[Has[AccountRepository], Account] =
    ZIO.serviceWith[AccountRepository](_.store(a))

  def store(as: List[Account]): RIO[Has[AccountRepository], Unit] =
    ZIO.serviceWith[AccountRepository](_.store(as))

  def allOpenedOn(openedOn: LocalDate): RIO[Has[AccountRepository], List[Account]] =
    ZIO.serviceWith[AccountRepository](_.allOpenedOn(openedOn))

  def allClosed(closeDate: Option[LocalDate]): RIO[Has[AccountRepository], List[Account]] =
    ZIO.serviceWith[AccountRepository](_.allClosed(closeDate))

  def allAccountsOfType(accountType: AccountType): RIO[Has[AccountRepository], List[Account]] =
    ZIO.serviceWith[AccountRepository](_.allAccountsOfType(accountType))
}
