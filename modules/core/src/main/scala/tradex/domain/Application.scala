package tradex.domain

import zio._
import zio.blocking.Blocking

import config._
import services.trading._
import services.accounting._
import repository._
import repository.doobie._

object Application {

  // define all layers of the architecture
  type InfraLayerEnv =
    Has[Config] with Has[Blocking.Service]

  type ConfigLayerEnv =
    InfraLayerEnv with Has[AppConfig] with Has[DBConfig]

  type RepositoryLayerEnv =
    ConfigLayerEnv
      with Has[AccountRepository]
      with Has[OrderRepository]
      with Has[ExecutionRepository]
      with Has[TradeRepository]
      with Has[BalanceRepository]

  type ServiceLayerEnv =
    RepositoryLayerEnv with Has[TradingService] with Has[AccountingService]

  type AppEnv = ServiceLayerEnv

  // compose layers for prod
  object prod {

    val infraLayer: ZLayer[Blocking, Throwable, InfraLayerEnv] =
      Blocking.any ++ ConfigProvider.live

    val configLayer: ZLayer[InfraLayerEnv, Throwable, ConfigLayerEnv] =
      AppConfigProvider.fromConfig ++ DbConfigProvider.fromConfig ++ ZLayer.identity

    val repositoryLayer: ZLayer[ConfigLayerEnv, Throwable, RepositoryLayerEnv] =
      AccountRepositoryLive.layer ++ OrderRepositoryLive.layer ++ ExecutionRepositoryLive.layer ++ TradeRepositoryLive.layer ++ BalanceRepositoryLive.layer ++ ZLayer.identity

    val serviceLayer: ZLayer[RepositoryLayerEnv, Throwable, ServiceLayerEnv] =
      TradingServiceLive.layer ++ AccountingServiceLive.layer ++ ZLayer.identity

    // final application layer for prod
    val appLayer: ZLayer[Blocking, Throwable, AppEnv] =
      infraLayer >+>
        configLayer >+>
        repositoryLayer >+>
        serviceLayer
  }
}
