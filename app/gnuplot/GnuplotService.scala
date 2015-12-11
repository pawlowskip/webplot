package gnuplot

import java.io.File
import java.net.URI
import java.nio.file.Paths
import akka.actor.ActorSystem
import models.{Graph, Project, Page}
import modules.Gnuplot
import util.{Files, ProcessRunner}

import scala.concurrent.Future
import scala.io.{Source}
import util.MyExecutionContext._

class GnuplotService(val system: ActorSystem) extends Gnuplot{

  val processRunner = new ProcessRunner(system)

  sealed trait OS
  object Windows extends OS
  object Unix extends OS

  private [this] val os = if (System.getProperty("os.name").startsWith("Windows")) Windows else Unix
  private [this] val symbol = os match {
    case Unix => ""
    case Windows => "\""
  }

  def singleRender(page: Page, graphs: List[Graph], script: File, output: File): Future[Array[Byte]] = {
    singleRender0(page, graphs, script, output).map{
      x => java.nio.file.Files.readAllBytes(Paths.get(x.getAbsolutePath))
    }
  }

  def singleRender0(page: Page, graphs: List[Graph], script: File, output: File): Future[File] = {
    val commands = ScriptGenerator.generateScript(page, graphs ,output.getAbsolutePath)
    Files.writeToFile(script, commands)
    processRunner.run(s"""gnuplot $symbol${script.getAbsolutePath}$symbol""").map( x => output )
  }

  def multiPageRender(project: Project, scripts: List[File], files: List[File]): Future[List[File]] = {
    require( project.pages.length == scripts.length && project.pages.length == files.length,
      s"Invalid number of elements! ${project.pages.length}, ${scripts.length}, ${files.length}")
    val futuresList: List[Future[File]] = (project.pages, scripts, files).zipped.map{
        case (page, script, output) =>
          singleRender0(page, project.graphs, script, output)
    }
    Future.sequence(futuresList)
  }

}
