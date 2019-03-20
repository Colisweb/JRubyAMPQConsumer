package com.colisweb.jrubyamqpconsumer.core

object AMQPConsumer {

  final case class Config(
      host: String,
      port: Int,
      virtualHost: Option[String],
      requestBufferSize: Int,
      credentials: Option[Credentials]
  )

  final case class Credentials(username: String, password: String)

  final case class Logger(info: String => Unit, error: String => Unit)

  def pullMessages(config: Config, logger: Logger, queueName: String)(f: String => AckBehavior): Unit = {
    AmqpConnector.run(config, logger, queueName, f)
  }

  sealed trait AckBehavior
  case object Ack   extends AckBehavior
  case object NoAck extends AckBehavior
}
