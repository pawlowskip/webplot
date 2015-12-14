package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import dao.{NoSuchProjectException, NoSuchUserException}
import jp.t2v.lab.play2.auth.AuthElement
import modules.{AccountsDao, Gnuplot}
import play.api.libs.json.Json
import util.{Zip, Files}
import models.{Account, Project, NormalUser}
import play.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import scala.util.{Random, Success, Failure}

class AppController (val accountsDao: AccountsDao,
                     val gnuplot: Gnuplot,
                     val messagesApi: MessagesApi)
  extends Controller
  with I18nSupport
  with AuthElement
  with AuthConfigImpl {

  val extensionToMIME = Map(
      "svg" -> "image/svg+xml; charset=UTF-8",
      "png" -> "image/png",
      "svg" -> "application/pdf",
      "eps" -> "application/eps")

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
      request.body.file("files[]").map { file =>
        val filename = file.filename
        val toFile = Future {
          file.ref.moveTo(Files.getOrCreateUserFile(loggedIn, filename))
        }(util.MyExecutionContext.ec)
        val res = toFile.map( f => {
          Ok(Json.toJson(Map("status" -> "ok")))
        })
        res.onComplete{
          case Success(_) => Logger.debug(s"Uploaded file $filename !")
          case Failure(e) => Logger.debug(s"Upload failed! Error:$e")
        }
        res
      }.getOrElse {
        Future.successful(BadRequest("Missing files"))
      }
  }

  def userFiles = AsyncStack(AuthorityKey -> NormalUser) {
    implicit request =>
      import models.DataFile._
      Logger.debug(s"Serving user files")
      Files.getAllUserDataFiles(loggedIn).map { userDataFiles =>
        Ok(Json.toJson(userDataFiles))
      }.fallbackTo(Future.successful(BadRequest("Error!")))
  }

  def deleteFile(fileName: String) = AsyncStack(AuthorityKey -> NormalUser){
    implicit request =>
      Logger.debug(s"Deleting user file: $fileName.")
      Files.deleteFile(loggedIn, fileName).map { done =>
          Logger.debug(s"Deleted file: $fileName.")
          Ok(Json.toJson(Map("status" -> "deleted")))
      } fallbackTo {
        Future.successful{
          BadRequest(Json.toJson(Map("status" -> "error")))
        }
      }
  }

  def downloadFile(fileName: String) = AsyncStack(AuthorityKey -> NormalUser){
    implicit request =>
      val user = loggedIn
      Logger.debug(s"Downloading user file: $fileName.")
      Files.getUserFile(user, fileName).map{
        file =>
          Logger.debug(s"Download started - file: $fileName.")
          Ok.sendFile(file)
      }.fallbackTo{
        Future.successful{
          BadRequest(Json.toJson(Map("status" -> "error")))
        }
      }
  }

  def drawSinglePlot = AsyncStack(parse.json, AuthorityKey -> NormalUser){
    implicit request =>
      Logger.debug("Request for graph plot ...")
      Logger.debug(Json.prettyPrint(request.body))
      val user = loggedIn
      val res1 = for {
        project <- Future{request.body.validate[Project].get}(util.MyExecutionContext.ec)
        p <- Files.transformFilePaths(user, project)
        r <- gnuplot.renderSinglePage(p.pages.head, p.graphs,
                                  Files.getUserScriptFile(user.username),
                                  Files.getUserOutputFile(user.username, p.pages.head.terminal))
      } yield Ok(r).as(extensionToMIME(p.pages.head.terminal))

      res1.onComplete{
        case Success(_) => Logger.debug("Graph created correctly.")
        case Failure(e) => Logger.error(s"Cannot create graph: ${e.getStackTrace.mkString("\n")}")
      }
      res1.fallbackTo{
        Future.successful(BadRequest("invalid json"))
      }
  }

  def generateProject = AsyncStack(parse.json, AuthorityKey -> NormalUser) {
    implicit request =>
      Logger.debug("Request for project plot ...")
      Logger.debug(Json.prettyPrint(request.body))

      val user = loggedIn
      val res1 = for {
        project <- Future{request.body.validate[Project].get}(util.MyExecutionContext.ec)
        p <- Files.transformFilePaths(user, project)
        r <- gnuplot.multiPageRender(p,
          Files.getUserScripts(user.username, p.pages.length),
          Files.getUserOutputs(user.username, p))
        zippedFiles <- Zip.zipFiles(Files.getUserZipFilePath(user.username), r)
      }  yield Ok(Json.toJson(Map("status" -> "ok")))
      res1.onComplete{
        case Success(_) => Logger.debug("Graph created correctly.")
        case Failure(e) =>
          e.printStackTrace()
          Logger.error(s"Cannot create graph: $e")
      }
      res1.fallbackTo{
        Future.successful(BadRequest("invalid json"))
      }
  }

  def getProject = AsyncStack(AuthorityKey -> NormalUser){
    implicit request =>
      val user = loggedIn
      Logger.debug(s"Downloading user project:")
      Future{
        val file = new java.io.File(Files.getUserZipFilePath(user.username))
        Logger.debug(s"Download started - file: ${file.getName}.")
        Ok.sendFile(file)
      }.fallbackTo{
        Future.successful{
          BadRequest(Json.toJson(Map("status" -> "error")))
        }
      }
  }

  def getUserProjects = AsyncStack(AuthorityKey -> NormalUser) {
    implicit request =>
      val user = loggedIn
      Logger.debug("Get user projects")
      val projectsFuture = accountsDao.getProjects(user.username).map{
        projects =>
          Ok(Json.toJson(projects))
      }

      projectsFuture.fallbackTo{
        Future.successful{
          BadRequest(Json.toJson(Map("status" -> "error")))
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
      saving.map{
        x => Ok(Json.toJson(Map("status" -> "created")))
      }.fallbackTo{
        Future.successful{
          BadRequest(Json.toJson(Map("status" -> "error")))
        }
      }
  }

  def deleteProject(projectName: String) = AsyncStack(AuthorityKey -> NormalUser) {
    implicit request =>
      val user = loggedIn
      Logger.debug(s"Delete project: $projectName. Owner: ${user.username} project")
      accountsDao.deleteProject(user.username, projectName).map {
        x => Ok(Json.toJson(Map("status" -> "deleted")))
      }.recover {
        case NoSuchProjectException(e) => BadRequest(Json.toJson(Map("status" -> "noProject")))
        case _=> BadRequest(Json.toJson(Map("status" -> "error")))
      }
  }

}
