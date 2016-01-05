package util

import java.util.concurrent.TimeoutException

import akka.actor.ActorSystem
import akka.pattern.after
import util.MyExecutionContext._

import scala.concurrent.duration._
import scala.concurrent.{Future, blocking}
import scala.language.postfixOps
import scala.sys.process.Process
import scala.util.Success

class ProcessRunner(system: ActorSystem) {

  private def withTimeout[T](f: Future[T])(implicit duration: FiniteDuration, system: ActorSystem): Future[T] = {
    val timeout = after(duration, system.scheduler)(Future.failed(new TimeoutException))
    Future firstCompletedOf Seq(f, timeout)
  }

  def run(command: String): Future[Int] = {
    val process = Process(command).run()
    val result: Future[Int] = Future {
      blocking {
        process.exitValue()
      }
    }(GnuplotExecutionContext.ec)

    implicit val timeout = 200 millis
    implicit val sys = system

    val combinedFuture = withTimeout(result)

    combinedFuture onFailure  {
      case e: TimeoutException => process.destroy()
    }
    combinedFuture
  }

}
