package controllers

import java.util.concurrent.TimeoutException
import dao.NoSuchProjectException
import jp.t2v.lab.play2.auth.AuthElement
import models.{NormalUser, Project}
import modules.{AccountsDao, Gnuplot}
import play.Logger
import play.api.libs.Files.TemporaryFile
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{Controller, MultipartFormData}
import util.{Files, Zip}

import scala.concurrent.Future
import scala.util._


class AppController (val accountsDao: AccountsDao,
                     val gnuplot: Gnuplot)
  extends Controller
  with AuthElement
  with AuthConfigImpl {

  val extensionToMIME = Map(
      "svg" -> "image/svg+xml; charset=UTF-8",
      "png" -> "image/png",
      "svg" -> "application/pdf",
      "eps" -> "application/eps")

  val messageOk = Json.toJson(Map("status" -> "ok"))
  val messageError = Json.toJson(Map("status" -> "error"))
  val messageInvalidJson = Json.toJson(Map("status" -> "invalid json"))
  val messageCreated = Json.toJson(Map("status" -> "created"))
  val messageDeleted = Json.toJson(Map("status" -> "deleted"))
  val messageMissingFiles = Json.toJson(Map("status" -> "missing files"))
  val messageNoSuchProject = Json.toJson(Map("status" -> "noProject"))

  def index = StackAction(AuthorityKey -> NormalUser) { implicit request =>
    Logger.debug("Serving index page ...")
    Ok(views.html.gnuplot())
  }

  def filesPage = StackAction(AuthorityKey -> NormalUser) { implicit request =>
    Logger.debug("Serving files page ...")
    Ok(views.html.files())
  }

  def upload = AsyncStack(parse.multipartFormData, AuthorityKey -> NormalUser) {
    implicit request =>
      def createUserFile(file: MultipartFormData.FilePart[TemporaryFile]) =
        file.ref.moveTo(Files.getOrCreateUserFile(loggedIn, file.filename))

      val uploadedFile = for {
        parsed <- Future.fromTry(Try(request.body.file("files[]").get))
        file <- Future(createUserFile(parsed))
      } yield file

      uploadedFile.andThen{
        case Success(f) => Logger.debug(s"Uploaded file ${f.getName} !")
        case Failure(e) => Logger.debug(s"Upload failed! Error:$e")
      } map {
        x => Ok(messageOk)
      } recover {
        case _ => BadRequest(messageMissingFiles)
      }
  }

  def userFiles = AsyncStack(AuthorityKey -> NormalUser) {
    implicit request =>
      Logger.debug(s"Serving user files")
      Files.getAllUserDataFiles(loggedIn).map { userDataFiles =>
        Ok(Json.toJson(userDataFiles))
      } fallbackTo {
        Future.successful {
          BadRequest(messageError)
        }
      }
  }

  def deleteFile(fileName: String) = AsyncStack(AuthorityKey -> NormalUser){
    implicit request =>
      Logger.debug(s"Deleting user file: $fileName.")

      val deleting = for {
        done <- Files.deleteFile(loggedIn, fileName)
        _ = Logger.debug(s"Deleted file: $fileName.")
      } yield done

      deleting.map { x =>
        Ok(Json.toJson(Map("status" -> "deleted")))
      } recover {
        case _ => BadRequest(messageError)
      }
  }

  def downloadFile(fileName: String) = AsyncStack(AuthorityKey -> NormalUser){
    implicit request =>
      Logger.debug(s"Downloading user file: $fileName.")
      Files.getUserFile(loggedIn, fileName).map{ file =>
        Logger.debug(s"Download started - file: $fileName.")
        Ok.sendFile(file)
      } fallbackTo {
        Future.successful{
          BadRequest(messageError)
        }
      }
  }

  def drawSinglePlot = AsyncStack(parse.json, AuthorityKey -> NormalUser){
    implicit request =>
      Logger.debug("Request for graph plot ...")
      Logger.debug(Json.prettyPrint(request.body))
      val result = for {
        project <- Future.fromTry(Try(request.body.validate[Project].get))
        transformed <- Files.transformFilePaths(loggedIn, project)
        rendered <- gnuplot.renderSinglePage(transformed.pages.head, transformed.graphs,
                                      Files.getUserScriptFile(loggedIn.username),
                                      Files.getUserOutputFile(loggedIn.username, transformed.pages.head.terminal))
      } yield Ok(rendered).as(extensionToMIME(transformed.pages.head.terminal))

      result.andThen{
        case Success(_) => Logger.debug("Graph created correctly.")
        case Failure(e: TimeoutException) => Logger.info("Cannot create graph - Timeout")
        case Failure(e) => Logger.error(s"Cannot create graph: ${e.getStackTrace.mkString("\n")}")
      } fallbackTo {
        Future.successful(BadRequest(messageInvalidJson))
      }
  }

  def generateProject = AsyncStack(parse.json, AuthorityKey -> NormalUser) {
    implicit request =>
      Logger.debug("Request for project plot ...")
      Logger.debug(Json.prettyPrint(request.body))

      val user = loggedIn
      val result = for {
        project <- Future.fromTry(Try(request.body.validate[Project].get))
        transformed <- Files.transformFilePaths(user, project)
        rendered <- gnuplot.multiPageRender(transformed,
                                            Files.getUserScripts(user.username, transformed.pages.length),
                                            Files.getUserOutputs(user.username, transformed))
        zippedFiles <- Zip.zipFiles(Files.getUserZipFilePath(user.username), rendered)
      } yield Ok(messageOk)

      result.andThen {
        case Success(_) => Logger.debug("Graph created correctly.")
        case Failure(e: TimeoutException) => Logger.info("Cannot create graph - Timeout")
        case Failure(e) => Logger.error(s"Cannot create graph: ${e.getStackTrace.mkString("\n")}")
      } fallbackTo {
        Future.successful(BadRequest(messageInvalidJson))
      }
  }

  def getProject = AsyncStack(AuthorityKey -> NormalUser) {
    implicit request =>
      Logger.debug(s"Downloading user project:")
      Future {
        val file = new java.io.File(Files.getUserZipFilePath(loggedIn.username))
        Logger.debug(s"Download started - file: ${file.getName}.")
        Ok.sendFile(file)
      } fallbackTo {
        Future.successful {
          BadRequest(messageError)
        }
      }
  }

  def getUserProjects = AsyncStack(AuthorityKey -> NormalUser) {
    implicit request =>
      Logger.debug("Get user projects")

      val projectsFuture = for {
        projects <- accountsDao.getProjects(loggedIn.username)
      } yield projects

      projectsFuture.map { ps =>
        Ok(Json.toJson(ps))
      } fallbackTo {
        Future.successful {
          BadRequest(messageError)
        }
      }
  }

  def saveProject = AsyncStack(parse.json, AuthorityKey -> NormalUser) {
    implicit request =>
      val user = loggedIn
      Logger.debug("Save user project")
      val saving = for {
        project <- Future{request.body.validate[Project].get}
        projectWithAuthor = project.copy(author = user.username)
        res <- accountsDao.saveOrUpdateProject(projectWithAuthor)
      } yield res

      saving.map {
        x => Ok(messageCreated)
      } fallbackTo {
        Future.successful {
          BadRequest(messageError)
        }
      }
  }

  def deleteProject(projectName: String) = AsyncStack(AuthorityKey -> NormalUser) {
    implicit request =>
      val user = loggedIn
      Logger.debug(s"Delete project: $projectName. Owner: ${user.username} project")
      accountsDao.deleteProject(user.username, projectName).map {
        x => Ok(messageDeleted)
      } recover {
        case NoSuchProjectException(e) => BadRequest(messageNoSuchProject)
        case _ => BadRequest(messageError)
      }
  }

}
