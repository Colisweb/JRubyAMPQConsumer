package com.colisweb.jrubyamqpconsumer.core
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.amqp._
import akka.stream.alpakka.amqp.scaladsl.{AmqpSource, CommittableIncomingMessage}
import akka.stream.scaladsl.{Sink, Source}
import com.rabbitmq.client.impl.StrictExceptionHandler

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.control.NonFatal

object AMQPConsumer {

  import com.colisweb.jrubyamqpconsumer.core.utils.FutureOps._

  private final val system: ActorSystem                      = ActorSystem("AmqpConnector")
  private implicit final val ec: ExecutionContext            = system.dispatcher
  private implicit final val materializer: ActorMaterializer = ActorMaterializer()(system)

  final case class Config(
      host: String,
      port: Int,
      requestBufferSize: Int,
      virtualHost: Option[String],
      credentials: Option[Credentials]
  )

  final case class Credentials(username: String, password: String)

  final case class Logger(info: String => Unit, error: String => Unit)

  final def createSource(
      config: Config,
      logger: Logger,
      queueName: String
  ): Source[CommittableIncomingMessage, NotUsed] = {
    val connectionProvider: AmqpConnectionProvider =
      new AmqpDetailsConnectionProvider(
        hostAndPortList = List(config.host -> config.port),
        credentials = config.credentials.map(c => AmqpCredentials(c.username, c.password)),
        virtualHost = config.virtualHost,
        exceptionHandler = Some(new StrictExceptionHandler)
      )

    logger.info(s"Connection with : ${connectionProvider.toString}")

    AmqpSource.committableSource(
      NamedQueueSourceSettings(connectionProvider, queueName)
        .withDeclarations(QueueDeclaration(name = queueName, durable = true)),
      bufferSize = config.requestBufferSize
    )
  }

  final def pullMessages(source: Source[CommittableIncomingMessage, NotUsed], logger: Logger)(
      f: (String, Int => Boolean, (Int, Boolean) => Boolean) => Unit
  ): Unit = {
    source
      .map { commitableMessage =>
        val message: String = commitableMessage.message.bytes.utf8String
        f(
          message,
          (timeout: Int) => ack(commitableMessage, timeout.seconds),
          (timeout: Int, requeue: Boolean) => nack(commitableMessage, requeue, timeout.seconds)
        )
      }
      .mapError {
        case NonFatal(error) =>
          val stacktrace = error.getStackTrace.mkString("\n")
          logger.error(s"Error handling message: $error\n$stacktrace")
          error
      }
      .runWith(Sink.ignore)
      .await(Duration.Inf)

    ()
  }

  private def ack(commitableMessage: CommittableIncomingMessage, timeout: FiniteDuration): Boolean =
    commitableMessage.ack().fold(_ => true, _ => false).await(timeout)

  private def nack(commitableMessage: CommittableIncomingMessage, requeue: Boolean, timeout: FiniteDuration): Boolean =
    commitableMessage.nack(requeue = requeue).fold(_ => true, _ => false).await(timeout)

}
