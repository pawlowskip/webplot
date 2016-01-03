package util

import util.MyExecutionContext._
import scala.concurrent.{Future, Promise, blocking}
import scala.sys.process.Process

object ProcessRunner {

  def run(command: String): Future[Int] = Future {
      blocking {
        val process = Process(command).run()
        process.exitValue match {
          case 0 => 0
          case _ => throw new Error("Syntax Exception")
        }
      }
    }

}
