package tradex.domain

import zio._
import java.time.LocalDate

import model.account._

package object repository {
  type AccountRepository = Has[AccountRepository.Service]

  def query(no: String): RIO[AccountRepository, Option[Account]] =
    ZIO.accessM(_.get.query(no))

  def all: RIO[AccountRepository, Seq[Account]] =
    ZIO.accessM(_.get.all)

  def store(a: Account): RIO[AccountRepository, Account] =
    ZIO.accessM(_.get.store(a))

  def query(openedOn: LocalDate): RIO[AccountRepository, Seq[Account]] =
    ZIO.accessM(_.get.query(openedOn))
}
