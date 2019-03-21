package com.colisweb.jrubyamqpconsumer.core

import akka.Done
import akka.stream.alpakka.amqp.scaladsl.CommittableIncomingMessage
import com.colisweb.jrubyamqpconsumer.core.AMQPConsumer._

import scala.concurrent.Future

object MessageHandler {
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
}
