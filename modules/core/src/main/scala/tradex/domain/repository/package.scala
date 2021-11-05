package tradex.domain

import zio._
import java.time.LocalDate

import model.account._

package object repository {
  type AccountRepository = Has[AccountRepository.Service]

  def query(no: AccountNo): RIO[AccountRepository, Option[Account]] =
    ZIO.accessM(_.get.query(no))

  def all: RIO[AccountRepository, List[Account]] =
    ZIO.accessM(_.get.all)

  def store(a: Account): RIO[AccountRepository, Account] =
    ZIO.accessM(_.get.store(a))

  def store(as: List[Account]): RIO[AccountRepository, Unit] =
    ZIO.accessM(_.get.store(as))

  def query(openedOn: LocalDate): RIO[AccountRepository, List[Account]] =
    ZIO.accessM(_.get.query(openedOn))

  def allClosed(closeDate: Option[LocalDate]): RIO[AccountRepository, List[Account]] =
    ZIO.accessM(_.get.allClosed(closeDate))

  def allAccountsOfType(accountType: AccountType): RIO[AccountRepository, List[Account]] =
    ZIO.accessM(_.get.allAccountsOfType(accountType))
}
