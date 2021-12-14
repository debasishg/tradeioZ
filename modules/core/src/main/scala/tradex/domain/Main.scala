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
import model.instrument._
import zio.query.DataSource
import zio.query.Request
import zio.query.ZQuery

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
    (for {
      trades   <- generateTrade(frontOfficeInput, userId)
      balances <- postBalance(trades)
    } yield (trades, balances)).provideLayer(Application.prod.appLayer)
  }
}

object QueryProgram {
  def queryTrades(
      forDate: LocalDate,
      isins: Set[ISINCode]
  ): ZQuery[Has[TradingService], TradingError, List[Trade]] = {
    case class GetTradesByInstruments(date: LocalDate, isins: NonEmptyChunk[ISINCode])
        extends Request[TradingError, List[Trade]]

    val ds: DataSource[Has[TradingService], GetTradesByInstruments] =
      DataSource.fromFunctionBatchedM(
        "TradesByInstruments"
      )(requests => ZIO.foreach(requests)(request => getTradesByISINCodes(request.date, request.isins.toSet)))
    ZQuery.fromRequest(GetTradesByInstruments(forDate, NonEmptyChunk(isins.head, isins.tail.toSeq: _*)))(ds)
  }
}
