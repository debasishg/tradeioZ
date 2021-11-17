package tradex.domain

import pureconfig.{ ConfigConvert, ConfigSource }
import pureconfig.generic.semiauto._
import zio._

object config {
  final case class Config(
      appConfig: AppConfig,
      dbConfig: DBConfig
  )

  object Config {
    implicit val convert: ConfigConvert[Config] = deriveConvert
  }

  type ConfigProvider = Has[Config]

  object ConfigProvider {

    val live: ZLayer[Any, IllegalStateException, Has[Config]] =
      ZLayer.fromEffect {
        ZIO
          .fromEither(ConfigSource.default.load[Config])
          .mapError(failures =>
            new IllegalStateException(
              s"Error loading configuration: $failures"
            )
          )
      }
  }

  type DbConfigProvider  = Has[DBConfig]
  type AppConfigProvider = Has[AppConfig]

  object DbConfigProvider {

    val fromConfig: ZLayer[ConfigProvider, Nothing, DbConfigProvider] =
      ZLayer.fromService(_.dbConfig)
  }

  object AppConfigProvider {

    val fromConfig: ZLayer[ConfigProvider, Nothing, AppConfigProvider] =
      ZLayer.fromService(_.appConfig)
  }

  // accessor to get dbconfig out
  def getDbConfig: ZIO[Has[Config], Throwable, DBConfig] =
    ZIO.access(_.get.dbConfig)

  // accessor to get appconfig out
  def getAppConfig: ZIO[Has[Config], Throwable, AppConfig] =
    ZIO.access(_.get.appConfig)

  case class AppConfig(
      maxAccountNoLength: Int,
      minAccountNoLength: Int,
      zeroBalanceAllowed: Boolean
  )

  object AppConfig {
    implicit val convert: ConfigConvert[AppConfig] = deriveConvert
  }

  final case class DBConfig(
      url: String,
      driver: String,
      user: String,
      password: String
  )
  object DBConfig {
    implicit val convert: ConfigConvert[DBConfig] = deriveConvert
  }
}
