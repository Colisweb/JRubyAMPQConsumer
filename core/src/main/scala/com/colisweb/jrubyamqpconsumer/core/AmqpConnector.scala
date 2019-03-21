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

  final def run(
      config: Config,
      logger: Logger,
      queueName: String,
      messageHandler: CommittableIncomingMessage => Future[Done]
  ): Unit = {

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
}

final class AmqpConnector
