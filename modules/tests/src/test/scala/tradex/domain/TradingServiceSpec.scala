package tradex.domain

import zio._
import zio.prelude._
import zio.test._
import zio.test.Assertion._
import zio.test.environment.TestClock
import zio.duration._

import generators._
import repository._
import model.account._
import repository.inmemory._
import services.trading._
import java.time._

object TradingServiceSpec extends DefaultRunnableSpec {
  val localDateZERO = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC)
  val spec = suite("Trading Service")(
    testM("successfully invoke getAccountsOpenedOn") {
      checkM(Gen.listOfN(5)(accountGen)) { accounts =>
        for {
          _   <- TestClock.adjust(1.day)
          now <- clock.instant
          dt = LocalDateTime.ofInstant(now, ZoneOffset.UTC)
          _ <- AccountRepository.store(
            accounts.map(_.copy(dateOfOpen = dt))
          )
          fetched <- TradingService.getAccountsOpenedOn(dt.toLocalDate())
        } yield assert(
          fetched.forall(_.dateOfOpen.toLocalDate() == dt.toLocalDate())
        )(
          equalTo(true)
        )
      }
    }.provideCustomLayer(
      (AccountRepositoryInMemory.layer ++
        OrderRepositoryInMemory.layer ++
        ExecutionRepositoryInMemory.layer ++
        TradeRepositoryInMemory.layer) >+> TradingServiceTest.layer
    ),
    testM("successfully invoke getTrades") {
      checkM(accountGen) { account =>
        for {
          stored <- AccountRepository.store(account)
        } yield assert(stored.accountType)(
          isOneOf(AccountType.values)
        )
      } *>
        checkM(Gen.listOfN(5)(tradeGen)) { trades =>
          for {
            accs <- AccountRepository.all
            _    <- TestClock.adjust(1.day)
            now  <- clock.instant
            dt                    = LocalDateTime.ofInstant(now, ZoneOffset.UTC)
            tradesTodayForAccount = trades.map(_.copy(accountNo = accs.head.no, tradeDate = dt))
            _ <- TradeRepository.storeNTrades(
              NonEmptyList(tradesTodayForAccount.head, tradesTodayForAccount.tail: _*)
            )
            fetched <- TradingService.getTrades(accs.head.no, Some(dt.toLocalDate()))
          } yield assert(
            fetched.forall(_.tradeDate.toLocalDate() == dt.toLocalDate())
          )(
            equalTo(true)
          )
        }
    }.provideCustomLayer(
      (AccountRepositoryInMemory.layer ++
        OrderRepositoryInMemory.layer ++
        ExecutionRepositoryInMemory.layer ++
        TradeRepositoryInMemory.layer) >+> TradingServiceTest.layer
    ),
    testM("successfully invoke orders") {
      checkM(Gen.listOfN(5)(frontOfficeOrderGen)) { foOrders =>
        for {
          os <- TradingService.orders(NonEmptyList(foOrders.head, foOrders.tail: _*))
        } yield assert(
          os.size > 0
        )(
          equalTo(true)
        )
      }
    }.provideCustomLayer(
      (AccountRepositoryInMemory.layer ++
        OrderRepositoryInMemory.layer ++
        ExecutionRepositoryInMemory.layer ++
        TradeRepositoryInMemory.layer) >+> TradingServiceTest.layer
    ),
    testM("successfully generate trades from front office input") {
      checkM(tradeGnerationInputGen) { case (account, isin, userId) =>
        checkM(generateTradeFrontOfficeInputGenWithAccountAndInstrument(List(account.no), List(isin))) { foInput =>
          for {
            trades <- TradingService.generateTrade(foInput, userId)
          } yield assert(trades.size > 0)(equalTo(true))
        }
      }
    }.provideCustomLayer(
      (AccountRepositoryInMemory.layer ++
        OrderRepositoryInMemory.layer ++
        ExecutionRepositoryInMemory.layer ++
        TradeRepositoryInMemory.layer) >+> TradingServiceTest.layer
    )
  )
}

object TradingServiceTest {
  val layer: URLayer[Has[AccountRepository] with Has[
    OrderRepository
  ] with Has[
    ExecutionRepository
  ] with Has[
    TradeRepository
  ], Has[TradingService]] = {
    (TradingServiceLive(_, _, _, _)).toLayer
  }
}
