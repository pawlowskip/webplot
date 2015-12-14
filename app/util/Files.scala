package util

import java.io.{FileNotFoundException, File}
import java.nio.charset.StandardCharsets
import java.nio.file.{NoSuchFileException, Paths}

import models._
import scala.concurrent.{Future, Promise}
import util.MyExecutionContext._
import scala.util.{Try, Success, Failure}
import scala.io.Source

object Files {
  
  val rootScriptCatalog = new File(ServiceConfiguration.rootUserScriptCatalog)
  val rootOutputCatalog = new File(ServiceConfiguration.rootUserOutputCatalog)
  val rootFilesCatalog = new File(ServiceConfiguration.rootUserFilesCatalog)

  rootScriptCatalog.mkdir()
  rootOutputCatalog.mkdir()
  rootFilesCatalog.mkdir()
  println( s"created users directory ${rootFilesCatalog.getAbsolutePath}" )

  def userCatalog(account: Account) = new File(rootFilesCatalog, account.username)

  def getOrCreateUserFile(account: Account, fileName: String) = new File(userCatalog(account), fileName)

  def createUserCatalog(account: Account) = userCatalog(account).mkdir()

  def getUserFiles(account: Account): Future[List[File]] = Future{userCatalog(account).listFiles.toList}

  def getAllUserDataFiles(account: Account): Future[List[DataFile]] = {
    def toDataFile(file: File): Future[DataFile] = Future{
        val source = Source.fromFile(file)
        val text = source.getLines().take(5).mkString("", "\n", "...")
        source.close()
        DataFile(file.getName, file.length, text)
      }

    //getUserFiles(account).flatMap { list => 
    //  Future.traverse(list)(toDataFile)
    //}
    for {
      files <- getUserFiles(account)
      dataFiles <- Future.traverse(files)(toDataFile)
    } yield dataFiles
  }
  
  def getUserFile(account: Account, fileName: String): Future[File] = {
//    getUserFiles(account).map {
//      _.find( _.getName == fileName ) match {
//        case Some(file) => file
//        case None => throw new NoSuchFileException(s"File $fileName doesn't exist!")
//      }
//    }
    
    for {
      files <- getUserFiles(account)
    } yield files.find(_.getName == fileName) match {
      case Some(file) => file
      case None => throw new NoSuchFileException(s"File $fileName doesn't exist!")
    }
  }

  def deleteFile(account: Account, fileName: String): Future[Boolean] = {
//    for {
//      list <- getUserFiles(account)
//      file = list.find{_.getName == fileName }
//      result = file match {
//        case Some(file) => file.delete()
//        case None => throw new NoSuchFileException(s"File $fileName doesn't exist!")
//      }
//      if result == true
//    } yield ()

    for {
      list <- getUserFiles(account)
      file = list.find(_.getName == fileName)
    } yield file match {
      case Some(existing) => existing.delete()
      case None => throw new NoSuchFileException(s"File $fileName doesn't exist!")
    }
  }

  def getFileExtension(file: File): Option[String] = {
    import Parser.fileExtension
    file.getName match {
      case fileExtension(ext) => Some(ext)
      case _ => None
    }
  }

  def writeToFile(file: File, text: String): Future[Unit] = Future{
    java.nio.file.Files.write(Paths.get(file.toURI), text.getBytes(StandardCharsets.UTF_8))
  }

  def getUserScriptFile(usersLogin: String): File = {
    new File(rootScriptCatalog, s"${usersLogin}_script.gpl")
  }

  def getUserOutputFile(login: String, extension: String): File = {
    new File(rootOutputCatalog, s"${login}_out.${extension}")
  }

  def getUserScripts(usersLogin: String, countOfScripts: Int): List[File] = {
    (0 until countOfScripts).toList.map{
      x => new File(rootScriptCatalog, s"${usersLogin}_script_${x}.gpl")
    }
  }

  def getUserOutputs(user:  String, project: Project): List[File] = {
    project.pages.zipWithIndex.map {
      case (page, index) => new File(rootOutputCatalog, s"${user}_out_${index}.${page.terminal}")
    }
  }

  def getUserZipFilePath(user: String): String = {
    new File(rootOutputCatalog, s"${user}_output.zip").getAbsolutePath
  }

  def transformFilePaths(account: Account, project: Project): Future[Project] = {

//    getUserFiles(account).map{
//      files => {
//        val correctedGraphs = project.graphs.map {
//          graph => {
//            val newPlots = graph.plots.map{
//              plot => plot.plotDataType match {
//                case "f" => plot
//                case "d" => {
//                  val correctPath = files.find(_.getName == plot.dataFile)
//                  plot.copy(dataFile = correctPath.get.getAbsolutePath)
//                }
//              }
//            }
//            graph.copy(plots = newPlots)
//          }
//        }
//        project.copy(graphs = correctedGraphs)
//      }
//    }

    def transformPlotDataPath(plot: Plot, files: List[File]) = plot.plotDataType match {
      case "f" => plot
      case "d" => {
        val correctPath = files.find(_.getName == plot.dataFile)
        plot.copy(dataFile = correctPath.get.getAbsolutePath)
      }
    }

    getUserFiles(account).map { files =>
      val correctedGraphs = project.graphs.map { graph =>
        val newPlots = graph.plots.map(transformPlotDataPath(_, files))
        graph.copy(plots = newPlots)
      }
      project.copy(graphs = correctedGraphs)
    }
  }
  
}
