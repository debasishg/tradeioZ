package tradex.domain
package repository

import java.time.LocalDate
import zio._
import model.account._

object AccountRepository {
  trait Service {
    def query(no: String): Task[Option[Account]]
    def store(a: Account): Task[Account]
    def query(openedOnDate: LocalDate): Task[Seq[Account]]
    def all: Task[Seq[Account]]
  }
}
