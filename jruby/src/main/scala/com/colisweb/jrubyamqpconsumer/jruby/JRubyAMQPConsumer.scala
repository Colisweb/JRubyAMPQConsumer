package com.colisweb.jrubyamqpconsumer.jruby

import com.colisweb.jrubyamqpconsumer.core.AMQPConsumer
import com.colisweb.jrubyamqpconsumer.core.AMQPConsumer.{Config, Credentials, Logger}

object JRubyAMQPConsumer {

  final def config(
      host: String,
      port: Int,
      requestBufferSize: Int,
      virtualHost: String, // Could be nil
      userName: String, // Could be nil
      password: String // Could be nil
  ): Config =
    Config(
      host = host,
      port = port,
      requestBufferSize = requestBufferSize,
      virtualHost = Option(virtualHost),
      credentials = (userName, password) match {
        case (null, _) => None
        case (_, null) => None
        case _         => Some(Credentials(username = userName, password = password))
      }
    )

  final def pullMessages(
      config: Config,
      logger: Logger,
      queueName: String,
      f: (String, Int => Boolean, (Int, Boolean) => Boolean) => Unit
  ): Unit = AMQPConsumer.pullMessages(AMQPConsumer.createSource(config, logger, queueName), logger)(f)

}
