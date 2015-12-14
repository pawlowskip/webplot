package util

import akka.actor.{ActorSystem}
import scala.concurrent.{Promise, Future}
import scala.sys.process.Process
import util.MyExecutionContext._
import scala.concurrent.blocking

class ProcessRunner(val actorSystem: ActorSystem) {

  def run(command: String): Future[Int] = {
    val p = Promise[Int]()

    Future{
      blocking{
        val process = Process(command).run()
        // 0 - ok, 1- error in  script,
        process.exitValue match{
          case 0 => p.success(0)
          case _ => p.failure(new Error("Syntax Exception"))
        }
      }
    }

    p.future
  }

}
