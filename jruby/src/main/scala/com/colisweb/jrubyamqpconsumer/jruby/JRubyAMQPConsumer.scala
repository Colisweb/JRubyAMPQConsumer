package com.colisweb.jrubyamqpconsumer.jruby

import com.colisweb.jrubyamqpconsumer.core.AMQPConsumer

object JRubyAMQPConsumer {

  def pullMessages(
      config: Config,
      logger: AMQPConsumer.Logger,
      queueName: String,
      f: String => AMQPConsumer.AckBehavior
  ): Unit = {

    val amqpConsumerConfig = AMQPConsumer.Config(
      host = config.host,
      port = config.port,
      virtualHost = Option.apply(config.virtualHost),
      requestBufferSize = config.requestBufferSize,
      credentials = Option.apply(
        AMQPConsumer.Credentials(
          config.credentials.username,
          config.credentials.password,
        )
      )
    )
    AMQPConsumer.pullMessages(amqpConsumerConfig, logger, queueName)(f)
  }

  final case class Config(
      host: String,
      port: Int,
      virtualHost: String,
      requestBufferSize: Int,
      credentials: AMQPConsumer.Credentials
  )

  def credentials(username: String, password: String): AMQPConsumer.Credentials =
    AMQPConsumer.Credentials(username, password)

  def config(
      host: String,
      port: Int,
      virtualHost: String,
      requestBufferSize: Int,
      credentials: AMQPConsumer.Credentials
  ): Config =
    Config(
      host,
      port,
      virtualHost,
      requestBufferSize,
      credentials
    )

  def logger(info: String => Boolean, error: String => Boolean): AMQPConsumer.Logger = AMQPConsumer.Logger(
    info.andThen(_ => ()),
    error.andThen(_ => ())
  )

  def ack: AMQPConsumer.Ack.type     = AMQPConsumer.Ack
  def noAck: AMQPConsumer.NoAck.type = AMQPConsumer.NoAck
}
