package com.colisweb.jrubyamqpconsumer.core

import akka.Done
import akka.stream.alpakka.amqp.IncomingMessage
import akka.stream.alpakka.amqp.scaladsl.CommittableIncomingMessage
import akka.util.ByteString
import com.colisweb.jrubyamqpconsumer.core.AMQPConsumer.{AckBehavior, Logger}
import org.mockito.integrations.scalatest.MockitoFixture
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import com.colisweb.jrubyamqpconsumer.core.AMQPConsumer._

import scala.concurrent.Future

class MessageHandlerTest extends WordSpec with Matchers with MockitoFixture with ScalaFutures {

  "A handle call" when {

    "call commitableMessage.ack" in {

      val (commitableMessage: CommittableIncomingMessage, logger: Logger, queueName: String) = mockSetup
      val processMessage: String => AckBehavior                                              = _ => AMQPConsumer.Ack

      when(commitableMessage.ack(anyBoolean)) thenReturn Future.successful(Done)

      whenReady(handle(logger, queueName, processMessage)(commitableMessage)) { res =>
        verify(commitableMessage, times(1)).ack(anyBoolean)
        verify(commitableMessage, times(0)).nack(anyBoolean, anyBoolean)
        res shouldBe Done
      }
    }

    "call commitableMessage.nack without requeue" in {

      val (commitableMessage: CommittableIncomingMessage, logger: Logger, queueName: String) = mockSetup
      val processMessage: String => AckBehavior                                              = _ => AMQPConsumer.NackWithoutRequeue

      when(commitableMessage.nack(anyBoolean, eqTo(false))) thenReturn Future.successful(Done)

      whenReady(handle(logger, queueName, processMessage)(commitableMessage)) { res =>
        verify(commitableMessage, times(1)).nack(anyBoolean, eqTo(false))
        verify(commitableMessage, times(0)).ack(anyBoolean)
        res shouldBe Done
      }
    }

    "call commitableMessage.nack with requeue" in {

      val (commitableMessage: CommittableIncomingMessage, logger: Logger, queueName: String) = mockSetup
      val processMessage: String => AckBehavior                                              = _ => AMQPConsumer.NackWithRequeue

      when(commitableMessage.nack(anyBoolean, eqTo(true))) thenReturn Future.successful(Done)

      whenReady(handle(logger, queueName, processMessage)(commitableMessage)) { res =>
        verify(commitableMessage, times(1)).nack(anyBoolean, eqTo(true))
        verify(commitableMessage, times(0)).ack(anyBoolean)
        res shouldBe Done
      }
    }
  }

  private final def mockSetup = {
    val incommingMessage = mock[IncomingMessage]

    val commitableMessage = mock[CommittableIncomingMessage]

    when(incommingMessage.bytes) thenReturn ByteString("test")
    when(incommingMessage.envelope) thenReturn null
    when(commitableMessage.message) thenReturn incommingMessage

    val logger    = Logger(_ => (), _ => ())
    val queueName = "test-queue"
    (commitableMessage, logger, queueName)
  }
}
