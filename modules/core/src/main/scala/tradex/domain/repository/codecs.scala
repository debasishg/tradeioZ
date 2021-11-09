package tradex.domain
package repository

import squants.market._
import doobie._
import NewtypeRefinedOps._
import model.account._
import model.instrument._
import model.order._

object codecs {
  implicit val accountNoGet: Get[AccountNo] =
    Get[String]
      .map(s => validate[AccountNo](s))
      .map(_.fold(errs => throw new Exception(errs.toString), identity))

  implicit val accountNoPut: Put[AccountNo] = Put[String].contramap(_.value.value)

  implicit val accountNameGet: Get[AccountName] =
    Get[String]
      .map(s => validate[AccountName](s))
      .map(_.fold(errs => throw new Exception(errs.toString), identity))

  implicit val accountNamePut: Put[AccountName] = Put[String].contramap(_.value.value)

  implicit val accountTypeGet: Get[AccountType] =
    Get[String]
      .map(s => AccountType.withName(s))

  implicit val accountTypePut: Put[AccountType] = Put[String].contramap(_.entryName)

  implicit val currencyGet: Get[Currency] =
    Get[String]
      .map(s => Currency(s).get)

  implicit val currencyPut: Put[Currency] = Put[String].contramap(_.name)

  implicit val isinCodeGet: Get[ISINCode] =
    Get[String]
      .map(s => validate[ISINCode](s))
      .map(_.fold(errs => throw new Exception(errs.toString), identity))

  implicit val isinCodePut: Put[ISINCode] = Put[String].contramap(_.value.value)

  implicit val instrumentNameGet: Get[InstrumentName] =
    Get[String]
      .map(s => validate[InstrumentName](s))
      .map(_.fold(errs => throw new Exception(errs.toString), identity))

  implicit val instrumentNamePut: Put[InstrumentName] = Put[String].contramap(_.value.value)

  implicit val instrumentTypeGet: Get[InstrumentType] =
    Get[String]
      .map(s => InstrumentType.withName(s))

  implicit val instrumentTypePut: Put[InstrumentType] = Put[String].contramap(_.entryName)

  implicit val lotSizeGet: Get[LotSize] =
    Get[Int]
      .map(s => validate[LotSize](s))
      .map(_.fold(errs => throw new Exception(errs.toString), identity))

  implicit val lotSizePut: Put[LotSize] = Put[Int].contramap(_.value.value)

  implicit val unitPriceGet: Get[UnitPrice] =
    Get[BigDecimal]
      .map(s => validate[UnitPrice](s))
      .map(_.fold(errs => throw new Exception(errs.toString), identity))

  implicit val unitPricePut: Put[UnitPrice] = Put[BigDecimal].contramap(_.value.value)

  implicit val moneyGet: Get[Money] =
    Get[BigDecimal]
      .map(s => USD(s))

  implicit val moneyPut: Put[Money] = Put[BigDecimal].contramap(_.amount)

  implicit val orderNoGet: Get[OrderNo] =
    Get[String]
      .map(s => validate[OrderNo](s))
      .map(_.fold(errs => throw new Exception(errs.toString), identity))

  implicit val orderNoPut: Put[OrderNo] = Put[String].contramap(_.value.value)

  implicit val quantityGet: Get[Quantity] =
    Get[BigDecimal]
      .map(s => validate[Quantity](s))
      .map(_.fold(errs => throw new Exception(errs.toString), identity))

  implicit val quantityPut: Put[Quantity] = Put[BigDecimal].contramap(_.value.value)

  implicit val buySellGet: Get[BuySell] =
    Get[String]
      .map(s => BuySell.withName(s))

  implicit val buySellPut: Put[BuySell] = Put[String].contramap(_.entryName)
}
