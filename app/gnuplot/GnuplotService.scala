package gnuplot

import java.io.File
import java.net.URI
import java.nio.file.Paths
import models.{Graph, Project, Page}
import modules.Gnuplot
import util.{Files, ProcessRunner}

import scala.concurrent.Future
import scala.io.{Source}
import play.api.libs.concurrent.Execution.Implicits._

class GnuplotService extends Gnuplot{

  //        val source = Source.fromFile(output, "UTF-8")
  //        val res = source.map( _.toByte ).toArray
  //        source.close
  //        res

  def singleRender(page: Page, graphs: List[Graph], script: File, output: File): Future[Array[Byte]] = {
    singleRender0(page, graphs, script, output).map{
      x => java.nio.file.Files.readAllBytes(Paths.get(x.getAbsolutePath))
    }
  }

  def singleRender0(page: Page, graphs: List[Graph], script: File, output: File): Future[File] = {
    val commands = ScriptGenerator.generateScript(page, graphs ,output.getAbsolutePath)
    println (commands)
    Files.writeToFile(script, commands)
    ProcessRunner.run(s"""gnuplot "${script.getAbsolutePath}"""").map( x => output )
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
