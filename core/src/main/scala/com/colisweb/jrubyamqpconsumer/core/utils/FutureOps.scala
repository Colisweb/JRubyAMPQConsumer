package com.colisweb.jrubyamqpconsumer.core.utils
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

object FutureOps {

  implicit final class FutureOps[T](val future: Future[T]) extends AnyVal {
    def await(duration: Duration): T = Await.result(future, duration)
    def fold[R](onSuccess: T => R, onFailure: Throwable => R)(implicit ec: ExecutionContext): Future[R] =
      future.map(onSuccess).recover { case e => onFailure(e) }
  }

}
