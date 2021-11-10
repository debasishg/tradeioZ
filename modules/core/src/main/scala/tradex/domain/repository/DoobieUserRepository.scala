package tradex.domain
package repository

import zio._
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.postgres.implicits._
import model.user._
import codecs._

final class DoobieUserRepository(xa: Transactor[Task]) {
  val userRepository: UserRepository.Service = new UserRepository.Service {
    def queryByUserName(username: UserName): Task[Option[User]] = ???

    def store(username: UserName, password: EncryptedPassword): Task[UserId] = ???
  }
}

object DoobieUserRepository {
  object SQL {

    // when writing we have a valid `Account` - hence we can use
    // Scala data types
    implicit val accountWrite: Write[User] =
      Write[
        (
            UserId,
            UserName,
            EncryptedPassword
        )
      ].contramap(user =>
        (
          user.userId,
          user.userName,
          user.password
        )
      )
  }
}
