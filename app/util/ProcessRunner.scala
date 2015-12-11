package util

import java.io.OutputStream
import java.nio.charset.Charset

import akka.actor.{ActorSystem, ActorLogging, Props, Actor}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{SyncVar, Promise, Future}
import scala.io.Source
import scala.sys.process.{ProcessIO, Process}
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
