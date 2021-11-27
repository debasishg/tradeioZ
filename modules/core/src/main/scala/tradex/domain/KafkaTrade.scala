package tradex.domain

import zio._
import zio.console.Console
import zio.kafka.serde._
import zio.clock.Clock
import zio.blocking.Blocking
import zio.kafka.consumer._
import zio.stream._

import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import model.trade._

object KafkaTrade extends zio.App {

  val foTradeSerde: Serde[Any, GenerateTradeFrontOfficeInput] =
    Serde.string.inmapM { foTradeAsString =>
      ZIO
        .fromEither(
          decode[GenerateTradeFrontOfficeInput](foTradeAsString).left
            .map(new RuntimeException(_))
        )
    } { matchAsObj =>
      ZIO.effect(matchAsObj.asJson.printWith(Printer.noSpaces))
    }

  val consumerSettings: ConsumerSettings =
    ConsumerSettings(List("localhost:9092"))
      .withGroupId("trades-consumer")

  val managedConsumer: RManaged[Clock with Blocking, Consumer] =
    Consumer.make(consumerSettings)

  val consumer: ZLayer[Clock with Blocking, Throwable, Has[Consumer]] =
    ZLayer.fromManaged(managedConsumer)

  val matchesStreams: ZIO[Console with Any with Has[Consumer] with Clock, Throwable, Unit] =
    Consumer
      .subscribeAnd(Subscription.topics("updates"))
      .plainStream(Serde.uuid, foTradeSerde)
      .map(cr => (cr.value, cr.offset))
      .tap { case (foTrade, _) => console.putStrLn(s"| $foTrade |") }
      .map { case (_, offset) => offset }
      .aggregateAsync(Consumer.offsetBatches)
      .run(ZSink.foreach(_.commit))

  def run(args: List[String]): zio.URIO[zio.ZEnv, zio.ExitCode] =
    matchesStreams.provideSomeLayer(consumer ++ zio.console.Console.live).exitCode
}
