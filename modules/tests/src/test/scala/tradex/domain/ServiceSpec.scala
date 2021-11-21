package tradex.domain

import zio._
import zio.test._
import zio.test.Assertion._

import generators._
import model.account._
import repository._
import repository.inmemory._
import services.trading._
import java.time._

object ServiceSpec extends DefaultRunnableSpec {
  val spec = suite("Trading and Accounting Service")(
    testM("successfully invoke a service") {
      for {
        accounts <- TradingService.getAccountsOpenedOn(LocalDate.EPOCH)
      } yield assert(accounts.forall(_.dateOfOpen.toLocalDate() == LocalDate.EPOCH))(
        equalTo(true)
      )
    }.provideLayer(
      (AccountRepositoryInMemory.layer ++
        OrderRepositoryInMemory.layer ++
        ExecutionRepositoryInMemory.layer ++
        TradeRepositoryInMemory.layer) >>> TradingServiceTest.layer
    )
  )
}

object TradingServiceTest {
  val layer: URLayer[Has[AccountRepository] with Has[OrderRepository] with Has[ExecutionRepository] with Has[
    TradeRepository
  ], Has[TradingService]] = {
    (TradingServiceLive(_, _, _, _)).toLayer
  }
}
