package tradex.domain

import zio._
import zio.test._
import zio.test.Assertion._

import generators._
import repository._
import repository.inmemory._
import services.trading._
import java.time._

object TradingServiceSpec extends DefaultRunnableSpec {
  val localDateZERO = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC)
  val spec = suite("Trading and Accounting Service")(
    testM("successfully invoke a service") {
      checkM(Gen.listOfN(5)(accountGen)) { accounts =>
        for {
          _ <- AccountRepository.store(
            accounts.map(_.copy(dateOfOpen = localDateZERO))
          )
          fetched <- TradingService.getAccountsOpenedOn(localDateZERO.toLocalDate())
        } yield assert(
          fetched.forall(_.dateOfOpen.toLocalDate() == localDateZERO.toLocalDate())
        )(
          equalTo(true)
        )
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
