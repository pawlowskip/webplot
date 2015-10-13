package util

import scala.concurrent.{Promise, Future}
import scala.sys.process.Process
import play.api.libs.concurrent.Execution.Implicits._

object ProcessRunner {

  def run(command: String): Future[Int] = {
    val p = Promise[Int]()
    //val logger = ProcessLogger( println(_) , err => p.failure(throw new Error(err)) )
    Future{
      val process = Process(command).run()
      // 0 - ok, 1- error in  script,
      process.exitValue match{
        case 0 => p.success(0)
        case _ => p.failure(new Error("Syntax Exception"))
      }
    }
    p.future
  }
}
