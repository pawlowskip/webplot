package util

import scala.concurrent.ExecutionContext
import scala.concurrent.forkjoin.ForkJoinPool

object MyExecutionContext {
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(new ForkJoinPool(1))
}

object GnuplotExecutionContext {
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(new ForkJoinPool(1))
}
