package tradex.domain
package model

import java.time.LocalDateTime
import zio._
import zio.prelude._

import squants.market._

import NewtypeRefinedOps._

import enumeratum._

import io.estatico.newtype.macros.newtype

import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection._
import eu.timepit.refined.boolean.AllOf
import eu.timepit.refined.types.string.NonEmptyString
import eu.timepit.refined.auto._

import _root_.shapeless.::
import _root_.shapeless.HNil

object account {
  type AccountNoString = String Refined AllOf[
    MaxSize[W.`12`.T] ::
      MinSize[W.`5`.T] ::
      HNil
  ]

  @newtype case class AccountNo(value: AccountNoString)

  @newtype case class AccountName(value: NonEmptyString)

  sealed abstract class AccountType(override val entryName: String) extends EnumEntry

  object AccountType extends Enum[AccountType] {
    case object Trading    extends AccountType("Trading")
    case object Settlement extends AccountType("Settlement")
    case object Both       extends AccountType("Both")

    val values = findValues
  }

  final case class Account private (
      no: AccountNo,
      name: AccountName,
      dateOfOpen: LocalDateTime,
      dateOfClose: Option[LocalDateTime],
      accountType: AccountType,
      baseCurrency: Currency,
      tradingCurrency: Option[Currency],
      settlementCurrency: Option[Currency]
  )

  case class CreateAccount(
      no: String,
      name: String,
      openDate: Option[LocalDateTime],
      closeDate: Option[LocalDateTime],
      baseCcy: Currency,
      tradingCcy: Option[Currency],
      settlementCcy: Option[Currency],
      accountType: AccountType
  ) {
    def toDomain = {
      accountType match {
        case AccountType.Trading =>
          Account.tradingAccount(
            no,
            name,
            openDate,
            closeDate,
            baseCcy,
            tradingCcy.getOrElse(USD)
          )
        case AccountType.Settlement =>
          Account.settlementAccount(
            no,
            name,
            openDate,
            closeDate,
            baseCcy,
            settlementCcy.getOrElse(USD)
          )
        case AccountType.Both =>
          Account.tradingAndSettlementAccount(
            no,
            name,
            openDate,
            closeDate,
            baseCcy,
            tradingCcy.getOrElse(USD),
            settlementCcy.getOrElse(USD)
          )
      }
    }
  }

  object Account {
    def tradingAccount(
        no: String,
        name: String,
        openDate: Option[LocalDateTime],
        closeDate: Option[LocalDateTime],
        baseCcy: Currency,
        tradingCcy: Currency
    ): Validation[String, Account] = {
      Validation.validateWith(
        validateAccountNo(no),
        validateAccountName(name),
        validateOpenCloseDate(openDate.getOrElse(today), closeDate)
      ) { (n, nm, d) =>
        Account(
          n,
          nm,
          d._1,
          d._2,
          AccountType.Trading,
          baseCcy,
          Some(tradingCcy),
          None
        )
      }
    }

    def settlementAccount(
        no: String,
        name: String,
        openDate: Option[LocalDateTime],
        closeDate: Option[LocalDateTime],
        baseCcy: Currency,
        settlementCcy: Currency
    ): Validation[String, Account] = {
      Validation.validateWith(
        validateAccountNo(no),
        validateAccountName(name),
        validateOpenCloseDate(openDate.getOrElse(today), closeDate)
      ) { (n, nm, d) =>
        Account(
          n,
          nm,
          d._1,
          d._2,
          AccountType.Settlement,
          baseCcy,
          None,
          Some(settlementCcy)
        )
      }
    }

    def tradingAndSettlementAccount(
        no: String,
        name: String,
        openDate: Option[LocalDateTime],
        closeDate: Option[LocalDateTime],
        baseCcy: Currency,
        tradingCcy: Currency,
        settlementCcy: Currency
    ): Validation[String, Account] = {
      Validation.validateWith(
        validateAccountNo(no),
        validateAccountName(name),
        validateOpenCloseDate(openDate.getOrElse(today), closeDate)
      ) { (n, nm, d) =>
        Account(
          n,
          nm,
          d._1,
          d._2,
          AccountType.Both,
          baseCcy,
          Some(tradingCcy),
          Some(settlementCcy)
        )
      }
    }

    private[model] def validateAccountNo(no: String): Validation[String, AccountNo] =
      validate[AccountNo](no)
        .map(Validation.succeed(_))
        .getOrElse(Validation.fail(s"Account No has to be at least 5 characters long: found $no"))

    private[model] def validateAccountName(name: String): Validation[String, AccountName] =
      validate[AccountName](name)
        .map(Validation.succeed(_))
        .getOrElse(Validation.fail(s"Account Name cannot be blank"))

    private def validateOpenCloseDate(
        od: LocalDateTime,
        cd: Option[LocalDateTime]
    ): Validation[String, (LocalDateTime, Option[LocalDateTime])] =
      cd.map { c =>
        if (c isBefore od)
          Validation.fail(s"Close date [$c] cannot be earlier than open date [$od]")
        else Validation.succeed((od, cd))
      }.getOrElse { Validation.succeed((od, cd)) }

    private def validateAccountAlreadyClosed(
        a: Account
    ): Validation[String, Account] = {
      if (a.dateOfClose.isDefined)
        Validation.fail(s"Account ${a.no} is already closed")
      else Validation.succeed(a)
    }

    private def validateCloseDate(
        a: Account,
        cd: LocalDateTime
    ): Validation[String, LocalDateTime] = {
      if (cd isBefore a.dateOfOpen)
        Validation.fail(s"Close date [$cd] cannot be earlier than open date [${a.dateOfOpen}]")
      else Validation.succeed(cd)
    }

    def close(
        a: Account,
        closeDate: LocalDateTime
    ): Validation[String, Account] = {
      Validation.validateWith(
        validateAccountAlreadyClosed(a),
        validateCloseDate(a, closeDate)
      )((acc, _) => acc.copy(dateOfClose = Some(closeDate)))
    }
  }
}