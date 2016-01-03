package gnuplot

import java.io.File
import java.nio.file.Paths

import akka.actor.ActorSystem
import models.{Graph, Page, Project}
import modules.Gnuplot
import util.MyExecutionContext._
import util.{Files, ProcessRunner}

import scala.concurrent.Future

class GnuplotService(val system: ActorSystem) extends Gnuplot{

  sealed trait OS
  object Windows extends OS
  object Unix extends OS

  private [this] val os = if (System.getProperty("os.name").startsWith("Windows")) Windows else Unix
  private [this] val symbol = os match {
    case Unix => ""
    case Windows => "\""
  }

  def renderSinglePage(page: Page,
                       graphs: List[Graph],
                       script: File,
                       output: File): Future[Array[Byte]] = {

    renderPageAndGetResultFile(page, graphs, script, output).map {
      x => java.nio.file.Files.readAllBytes(Paths.get(x.getAbsolutePath))
    }
  }

  def renderPageAndGetResultFile(page: Page,
                                 graphs: List[Graph],
                                 script: File,
                                 output: File): Future[File] = {
    
    val commands = ScriptGenerator.generateScript(page, graphs ,output.getAbsolutePath)
    Files.writeToFile(script, commands)
    val command = s"""gnuplot $symbol${script.getAbsolutePath}$symbol"""
    ProcessRunner.run(command).map(x => output)
  }

  def multiPageRender(project: Project, 
                      scripts: List[File], 
                      files: List[File]): Future[List[File]] = {
    
    require(project.pages.length == scripts.length &&
            project.pages.length == files.length,
            s"Invalid number of elements! ${project.pages.length}, ${scripts.length}, ${files.length}")

    val futuresList: List[Future[File]] = (project.pages, scripts, files).zipped.map {
      case (page, script, output) =>
        renderPageAndGetResultFile(page, project.graphs, script, output)
    }
    Future.sequence(futuresList)
  }

}
