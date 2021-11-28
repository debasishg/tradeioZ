package tradex.domain

import org.apache.kafka.clients.producer._
import java.util.UUID

import zio._
import zio.console.Console
import zio.kafka.serde.Serde
import zio.clock.Clock
import zio.blocking.Blocking
import zio.kafka.consumer._
import zio.kafka.producer.{ Producer, ProducerSettings }
import zio.stream._
import zio.duration.durationInt

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

  val foTradesStreams: ZIO[Console with Any with Has[Consumer] with Clock, Throwable, Unit] =
    Consumer
      .subscribeAnd(Subscription.topics("updates"))
      .plainStream(Serde.uuid, foTradeSerde)
      .map(cr => (cr.value, cr.offset))
      .tap { case (foTrade, _) => console.putStrLn(s"| $foTrade |") }
      .map { case (_, offset) => offset }
      .aggregateAsync(Consumer.offsetBatches)
      .run(ZSink.foreach(_.commit))

  val producerSettings: ProducerSettings = ProducerSettings(List("localhost:9092"))

  val producer: ZLayer[Blocking, Throwable, Has[Producer]] =
    ZLayer.fromManaged(
      Producer.make(producerSettings)
    )

  val foTrade: GenerateTradeFrontOfficeInput = null
  val messagesToSend: ProducerRecord[UUID, GenerateTradeFrontOfficeInput] =
    new ProducerRecord(
      "updates",
      UUID.fromString("b91a7348-f9f0-4100-989a-cbdd2a198096"),
      foTrade
    )

  val producerEffect: RIO[Any with Has[Producer], RecordMetadata] =
    Producer.produce[Any, UUID, GenerateTradeFrontOfficeInput](messagesToSend, Serde.uuid, foTradeSerde)

  def run(args: List[String]): zio.URIO[zio.ZEnv, zio.ExitCode] = {
    val program = for {
      _ <- foTradesStreams.provideSomeLayer(consumer ++ zio.console.Console.live).fork
      _ <- producerEffect.provideSomeLayer(producer) *> ZIO.sleep(5.seconds)
    } yield ()
    program.exitCode
  }
}
