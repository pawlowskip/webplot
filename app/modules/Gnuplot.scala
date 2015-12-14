package modules

import java.io.File
import models.{Project, Page, Graph}
import scala.concurrent.Future

trait Gnuplot{
  def multiPageRender(project: Project, scripts: List[File], files: List[File]): Future[List[File]]
  def renderSinglePage(page: Page, graphs: List[Graph], script: File, output: File): Future[Array[Byte]]
}
