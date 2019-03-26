package com.colisweb.jrubyamqpconsumer.jruby

import com.colisweb.jrubyamqpconsumer.core.AMQPConsumer
import com.colisweb.jrubyamqpconsumer.core.AMQPConsumer._

object JRubyAMQPConsumer {

  def consumeAtLeastOnce(
      config: Config,
      logger: Logger,
      queueName: String,
      f: String => AckBehavior
  ): Unit = {
    AMQPConsumer.consumeAtLeastOnce(config, logger, queueName)(f)
  }

  def config(
      host: String,
      port: Int,
      virtualHost: String, // Could be nil
      requestBufferSize: Int,
      userName: String, // Could be nil
      password: String // Could be nil
  ): Config =
    Config(
      host = host,
      port = port,
      virtualHost = Option(virtualHost),
      requestBufferSize = requestBufferSize,
      credentials = (userName, password) match {
        case (null, _) => None
        case (_, null) => None
        case _         => Some(Credentials(username = userName, password = password))
      }
    )

  def logger(info: String => Boolean, error: String => Boolean): Logger = Logger(
    info.andThen(_ => ()),
    error.andThen(_ => ())
  )

  def ack: Ack.type                               = Ack
  def nackWithRequeue: NackWithRequeue.type       = NackWithRequeue
  def nackWithoutRequeue: NackWithoutRequeue.type = NackWithoutRequeue
}
