package tradex.domain

import zio.ZLayer
import zio.blocking.Blocking

import config._
import services._
import services.trading._
import services.accounting._
import repository._
import repository.doobie._

object Application {

  // define all layers of the architecture
  type InfraLayerEnv =
    ConfigProvider with Blocking

  type ConfigLayerEnv =
    InfraLayerEnv with AppConfigProvider with DbConfigProvider

  type RepositoryLayerEnv =
    ConfigLayerEnv
      with AccountRepository
      with OrderRepository
      with ExecutionRepository
      with TradeRepository
      with BalanceRepository

  type ServiceLayerEnv =
    RepositoryLayerEnv with TradingService with AccountingService

  type AppEnv = ServiceLayerEnv

  // compose layers for prod
  object prod {

    val infraLayer: ZLayer[Blocking, Throwable, InfraLayerEnv] =
      Blocking.any ++ ConfigProvider.live

    val configLayer: ZLayer[InfraLayerEnv, Throwable, ConfigLayerEnv] =
      AppConfigProvider.fromConfig ++ DbConfigProvider.fromConfig ++ ZLayer.identity

    val repositoryLayer: ZLayer[ConfigLayerEnv, Throwable, RepositoryLayerEnv] =
      DoobieAccountRepository.layer ++ DoobieOrderRepository.layer ++ DoobieExecutionRepository.layer ++ DoobieTradeRepository.layer ++ DoobieBalanceRepository.layer ++ ZLayer.identity

    val serviceLayer: ZLayer[RepositoryLayerEnv, Throwable, ServiceLayerEnv] =
      TradingService.live ++ AccountingService.live ++ ZLayer.identity

    // final application layer for prod
    val appLayer: ZLayer[Blocking, Throwable, AppEnv] =
      infraLayer >+>
        configLayer >+>
        repositoryLayer >+>
        serviceLayer
  }
}
