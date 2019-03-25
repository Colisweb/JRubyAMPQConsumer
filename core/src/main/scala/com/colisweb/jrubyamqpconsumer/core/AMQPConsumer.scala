package com.colisweb.jrubyamqpconsumer.core

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.amqp._
import akka.stream.alpakka.amqp.scaladsl._
import akka.stream.scaladsl.{Sink, Source}
import akka.{Done, NotUsed}
import com.rabbitmq.client.impl.StrictExceptionHandler

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

object AMQPConsumer {

  final def pullMessages(config: Config, logger: Logger, queueName: String)(f: String => AckBehavior): Unit = {
    val messageHandler = handle(logger, queueName, f)
    run(config, logger, queueName, messageHandler)
  }

  final def handle(
      logger: Logger,
      queueName: String,
      processMessage: String => AckBehavior
  ): CommittableIncomingMessage => Future[Done] =
    commitableMessage => {
      val message: String = commitableMessage.message.bytes.utf8String

      logger.info(
        s"Consuming message ${commitableMessage.message.envelope} from $queueName : ${message}"
      )

      processMessage(message) match {
        case Ack =>
          logger.info(s"ack message ${commitableMessage.message.envelope} from $queueName")
          commitableMessage.ack()
        case NackWithRequeue =>
          logger.info(s"nack with requeue for message ${commitableMessage.message.envelope} from $queueName")
          commitableMessage.nack(requeue = true)
        case NackWithoutRequeue =>
          logger.info(s"nack without requeue for message ${commitableMessage.message.envelope} from $queueName")
          commitableMessage.nack(requeue = false)
      }
    }

  final def run(
      config: Config,
      logger: Logger,
      queueName: String,
      messageHandler: CommittableIncomingMessage => Future[Done]
  ): Unit = {

    val system: ActorSystem                      = ActorSystem("AmqpConnector")
    implicit val materializer: ActorMaterializer = ActorMaterializer()(system)

    val connectionProvider: AmqpConnectionProvider =
      new AmqpDetailsConnectionProvider(
        hostAndPortList = List(config.host -> config.port),
        credentials = config.credentials.map(c => AmqpCredentials(c.username, c.password)),
        virtualHost = config.virtualHost,
        exceptionHandler = Some(new StrictExceptionHandler)
      )

    logger.info(s"Connection with : ${connectionProvider.toString}")

    def requestSource: Source[CommittableIncomingMessage, NotUsed] = {
      AmqpSource.committableSource(
        NamedQueueSourceSettings(connectionProvider, queueName)
          .withDeclarations(QueueDeclaration(name = queueName, durable = true)),
        bufferSize = config.requestBufferSize
      )
    }

    def runAsync: Future[Done] = {
      requestSource
        .mapAsync(1) { messageHandler }
        .mapError {
          case NonFatal(error) =>
            val stacktrace = error.getStackTrace.mkString("\n")
            logger.error(s"Error handling message from $queueName : $error\n$stacktrace")
            error
        }
        .runWith(Sink.ignore)
    }

    Await.result(runAsync, Duration.Inf)
    ()
  }

  final case class Config(
      host: String,
      port: Int,
      virtualHost: Option[String],
      requestBufferSize: Int,
      credentials: Option[Credentials]
  )

  final case class Credentials(username: String, password: String)

  final case class Logger(info: String => Unit, error: String => Unit)

  sealed trait AckBehavior
  case object Ack                extends AckBehavior
  case object NackWithRequeue    extends AckBehavior
  case object NackWithoutRequeue extends AckBehavior
}
