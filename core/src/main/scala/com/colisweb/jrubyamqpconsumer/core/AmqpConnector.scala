package com.colisweb.jrubyamqpconsumer.core

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.amqp._
import akka.stream.alpakka.amqp.scaladsl._
import akka.stream.scaladsl.{Sink, Source}
import akka.{Done, NotUsed}
import com.colisweb.jrubyamqpconsumer.core.AMQPConsumer._
import com.rabbitmq.client.impl.StrictExceptionHandler

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

object AmqpConnector {

  def run(config: Config, logger: Logger, queueName: String, processMessage: String => AckBehavior): Unit = {

    val system: ActorSystem                      = ActorSystem("AmqpConnector")
    implicit val materializer: ActorMaterializer = ActorMaterializer()(system)

    val connectionProvider: AmqpConnectionProvider = {
      val baseProvider = AmqpDetailsConnectionProvider(config.host, config.port)
        .withExceptionHandler(new StrictExceptionHandler)

      val configuredProvider = List(
        config.credentials.map(
          credentials =>
            (provider: AmqpDetailsConnectionProvider) =>
              provider.withCredentials(
                AmqpCredentials(credentials.username, credentials.password)
              )
        ),
        config.virtualHost.map(
          virtualHost => (provider: AmqpDetailsConnectionProvider) => provider.withVirtualHost(virtualHost)
        )
      ).flatten
        .foldLeft(baseProvider) { (provider, option) =>
          option(provider)
        }

      logger.info(s"Connection with : ${configuredProvider.toString}")
      configuredProvider
    }

    def requestSource(queueName: String): Source[CommittableIncomingMessage, NotUsed] = {
      AmqpSource.committableSource(
        NamedQueueSourceSettings(connectionProvider, queueName)
          .withDeclarations(QueueDeclaration(name = queueName, durable = true)),
        bufferSize = config.requestBufferSize
      )
    }

    def runAsync(queueName: String, processMessage: String => AckBehavior): Future[Done] = {
      requestSource(queueName)
        .mapAsync(1) { commitableMessage =>
          val message: String = commitableMessage.message.bytes.utf8String

          logger.info(
            s"Consuming message ${commitableMessage.message.envelope} from $queueName : ${message}"
          )

          processMessage(message) match {
            case Ack =>
              logger.info(s"ack message ${commitableMessage.message.envelope} from $queueName")
              commitableMessage.ack()
            case NoAck =>
              logger.info(s"no-ack message ${commitableMessage.message.envelope} from $queueName")
              Future.successful(Done)
          }
        }
        .mapError {
          case NonFatal(error) =>
            val stacktrace = error.getStackTrace.mkString("\n")
            logger.error(s"Error handling message from $queueName : $error\n$stacktrace")
            error
        }
        .runWith(Sink.ignore)
    }

    Await.result(runAsync(queueName, processMessage), Duration.Inf)
    ()
  }
}

class AmqpConnector
