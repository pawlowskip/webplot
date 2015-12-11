package util

import java.util.concurrent.{ForkJoinWorkerThread, Executor}

import play.api.Play.current
import play.api.libs.concurrent.Akka
import scala.concurrent.ExecutionContext
import scala.concurrent.forkjoin.ForkJoinPool
import play.api.libs.concurrent.Execution.Implicits._

object MyExecutionContext {
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(new ForkJoinPool(1))
}
