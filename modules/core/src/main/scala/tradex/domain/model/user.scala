package tradex.domain
package model

import java.util.UUID
import javax.crypto.Cipher
import scala.util.control.NoStackTrace
import zio.prelude._

import eu.timepit.refined.auto._
import eu.timepit.refined.types.string.NonEmptyString
import io.circe._
import io.estatico.newtype.macros.newtype

import NewtypeRefinedOps._

object user {
  @newtype
  case class UserId(value: UUID)

  @newtype
  case class UserName(value: NonEmptyString)

  @newtype
  case class Password(value: NonEmptyString)

  @newtype
  case class EncryptedPassword(value: NonEmptyString)

  @newtype
  case class EncryptCipher(value: Cipher)

  @newtype
  case class DecryptCipher(value: Cipher)

  case class UserNotFound(username: UserName)    extends NoStackTrace
  case class UserNameInUse(username: UserName)   extends NoStackTrace
  case class InvalidPassword(username: UserName) extends NoStackTrace
  case object UnsupportedOperation               extends NoStackTrace
  case object TokenNotFound                      extends NoStackTrace

  private[domain] final case class User private (
      userId: UserId,
      userName: UserName,
      password: EncryptedPassword
  )

  object User {
    def user(
        id: UUID,
        name: String,
        password: String
    ): Validation[String, User] = {
      (
        validateUserName(name),
        validatePassword(password)
      ).mapN { (nm, pd) =>
        User(UserId(id), nm, pd)
      }
    }

    private[model] def validateUserName(
        name: String
    ): Validation[String, UserName] =
      validate[UserName](name)
			  .mapError(s => s"User Name cannot be blank: (Root Cause: $s)")

    private[model] def validatePassword(
        name: String
    ): Validation[String, EncryptedPassword] =
      validate[EncryptedPassword](name)
			  .mapError(s => s"User Password cannot be blank: (Root cause: $s)")
  }

  // --------- user registration -----------

  @newtype
  case class UserNameParam(value: NonEmptyString) {
    def toDomain: UserName = UserName(value)
  }

  @newtype
  case class PasswordParam(value: NonEmptyString) {
    def toDomain: Password = Password(value)
  }

  case class CreateUser(
      username: UserNameParam,
      password: PasswordParam
  )

  // --------- user login -----------

  case class LoginUser(
      username: UserName,
      password: Password
  )

  // --------- admin auth -----------

  @newtype
  case class ClaimContent(uuid: UUID)

  object ClaimContent {
    implicit val jsonDecoder: Decoder[ClaimContent] =
      Decoder.forProduct1("uuid")(ClaimContent.apply)
  }
}