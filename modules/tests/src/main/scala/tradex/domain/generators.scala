package tradex.domain

import java.time._
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric._
import eu.timepit.refined.types.string.NonEmptyString

import zio.random.Random
import zio.test._
import zio.test.Gen._
import zio.test.Gen.fromEffectSample

import squants.market.USD
import squants.market.JPY

import model.account._
import NewtypeRefinedOps._

object generators {
  val posIntGen =
    fromEffectSample(zio.random.nextIntBetween(0, Int.MaxValue).map(Sample.shrinkIntegral(0)))

  val nonEmptyStringGen: Gen[Random with Sized, String]  = Gen.alphaNumericStringBounded(21, 40)
  val accountNoStringGen: Gen[Random with Sized, String] = Gen.alphaNumericStringBounded(5, 12)
  val accountNoGen: Gen[Random with Sized, AccountNo] =
    accountNoStringGen.map(str =>
      validate[AccountNo](str)
        .fold(errs => throw new Exception(errs.toString), identity)
    )

  val accountNameGen: Gen[Random with Sized, AccountName] =
    nonEmptyStringGen
      .map(s => validate[AccountName](s))
      .map(_.fold(errs => throw new Exception(errs.toString), identity))

  def accountNameStartingPatternGen(pattern: String): Gen[Random with Sized, AccountName] =
    nonEmptyStringGen
      .map(s => validate[AccountName](s"$pattern$s"))
      .map(_.fold(errs => throw new Exception(errs.toString), identity))

  def openCloseDateGen = for {
    o <- Gen.fromIterable(List(LocalDateTime.now, LocalDateTime.now.plusDays(2)))
    c <- Gen.const(o.plusDays(100))
  } yield (o, c)

  def accountWithNamePatternGen(pattern: String) = for {
    no <- accountNoGen
    nm <- accountNameStartingPatternGen(pattern)
    oc <- openCloseDateGen
    tp <- Gen.fromIterable(AccountType.values)
    bc <- Gen.const(USD)
    tc <- Gen.fromIterable(List(USD, JPY)).map(Some(_))
    sc <- Gen.const(None)
  } yield Account(no, nm, oc._1, Some(oc._2), tp, bc, tc, sc)

  val tradingAccountGen: Gen[Random with Sized, Account] = for {
    no <- accountNoGen
    nm <- accountNameGen
    oc <- openCloseDateGen
    tp <- Gen.const(AccountType.Trading)
    bc <- Gen.const(USD)
    tc <- Gen.fromIterable(List(USD, JPY)).map(Some(_))
    sc <- Gen.const(None)
  } yield Account(no, nm, oc._1, Some(oc._2), tp, bc, tc, sc)

  val settlementAccountGen: Gen[Random with Sized, Account] = for {
    no <- accountNoGen
    nm <- accountNameGen
    oc <- openCloseDateGen
    tp <- Gen.const(AccountType.Settlement)
    bc <- Gen.const(USD)
    tc <- Gen.const(None)
    sc <- Gen.fromIterable(List(USD, JPY)).map(Some(_))
  } yield Account(no, nm, oc._1, Some(oc._2), tp, bc, tc, sc)

  val bothAccountGen: Gen[Random with Sized, Account] = for {
    no <- accountNoGen
    nm <- accountNameGen
    oc <- openCloseDateGen
    tp <- Gen.const(AccountType.Both)
    bc <- Gen.const(USD)
    tc <- Gen.fromIterable(List(USD, JPY)).map(Some(_))
    sc <- Gen.fromIterable(List(USD, JPY)).map(Some(_))
  } yield Account(no, nm, oc._1, Some(oc._2), tp, bc, tc, sc)

  def accountGen: Gen[Random with Sized, Account] =
    Gen.oneOf(tradingAccountGen, settlementAccountGen, bothAccountGen)
}
