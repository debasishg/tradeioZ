package tradex.domain

import zio._
import zio.console.putStrLn
import zio.blocking.Blocking
import zio.prelude.NonEmptyList

import services.trading.{ TradingService, TradingServiceError }
import TradingService._
import TradingServiceError._
import services.accounting.AccountingService
import AccountingService._
import java.time._
import model.account._
import model.trade._
import model.user._
import model.balance._

object Main extends zio.App {
  def run(args: List[String]): zio.URIO[zio.ZEnv, zio.ExitCode] = {
    val program = for {
      dbConf   <- config.getDbConfig
      _        <- FlywayMigration.migrate(dbConf)
      accounts <- makeProgram
    } yield accounts

    program
      .provideLayer(Application.prod.infraLayer) // needed for config
      .map(accs => println(accs.size.toString))
      .tapError(err => putStrLn(s"Execution failed with: ${err.getMessage}"))
      .exitCode
  }

  val module: ZIO[Has[TradingService], TradingError, List[Account]] = for {
    accounts <- getAccountsOpenedOn(LocalDate.EPOCH)
  } yield accounts

  val makeProgram = module.provideLayer(Application.prod.appLayer)
}

object Program {
  def generate(
      frontOfficeInput: GenerateTradeFrontOfficeInput,
      userId: UserId
  ): ZIO[Blocking, Throwable, (NonEmptyList[Trade], NonEmptyList[Balance])] = {

    val action = for {
      orders <- orders(frontOfficeInput.frontOfficeOrders)
      executions <- execute(
        orders,
        frontOfficeInput.market,
        frontOfficeInput.brokerAccountNo
      )
      trades   <- allocate(executions, frontOfficeInput.clientAccountNos, userId)
      balances <- postBalance(trades)
    } yield (trades, balances)

    action.provideLayer(Application.prod.appLayer)
  }
}
