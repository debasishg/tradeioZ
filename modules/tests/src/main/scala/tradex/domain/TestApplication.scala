package tradex.domain

import zio._
import zio.random.Random

import services.trading._
import services.accounting._
import repository._
import repository.inmemory._

object TestApplication {

  // define all layers of the architecture

  type RepositoryLayerEnv =
    Has[AccountRepository]
      with Has[OrderRepository]
      with Has[ExecutionRepository]
      with Has[TradeRepository]
      with Has[BalanceRepository]
      with Has[Random.Service]

  type ServiceLayerEnv =
    RepositoryLayerEnv with Has[TradingService] with Has[AccountingService]

  type TestAppEnv = ServiceLayerEnv

  // compose layers for test
  object test {

    val repositoryLayer: ZLayer[Random, Throwable, RepositoryLayerEnv] =
      Random.any ++ AccountRepositoryInMemory.layer ++
        OrderRepositoryInMemory.layer ++
        ExecutionRepositoryInMemory.layer ++
        TradeRepositoryInMemory.layer ++
        BalanceRepositoryInMemory.layer // ++ ZLayer.identity

    val serviceLayer: ZLayer[RepositoryLayerEnv, Throwable, ServiceLayerEnv] =
      // val serviceLayer: ULayer[ServiceLayerEnv] =
      TradingServiceLive.layer ++ AccountingServiceLive.layer ++ ZLayer.identity

    // final application layer for test
    val testAppLayer: ZLayer[Random, Throwable, TestAppEnv] =
      // val testAppLayer: ULayer[TestAppEnv] =
      repositoryLayer >+> serviceLayer
  }
}
