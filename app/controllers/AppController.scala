package controllers


import java.nio.file.Paths

import jp.t2v.lab.play2.auth.{OptionalAuthElement, AuthElement}
import modules.{AccountsDao, Gnuplot}
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.Json
import util.{Zip, Files}
import models.{Project, DataFile, NormalUser, Page}
import play.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{ResponseHeader, Result, Action, Controller}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import scala.util.{Try, Success, Failure}


class AppController (val accountsDao: AccountsDao,
                     val gnuplot: Gnuplot,
                     val messagesApi: MessagesApi)
  extends Controller
  with I18nSupport
  with AuthElement
  with AuthConfigImpl {

  val extensionToMIME =
    Map(
      "svg" -> "image/svg+xml; charset=UTF-8",
      "png" -> "image/png",
      "svg" -> "application/pdf",
      "eps" -> "application/eps"
    )


  def index =  AsyncStack(AuthorityKey -> NormalUser) { implicit request =>
    Logger.info("Serving index page ...")
    val user = loggedIn
    Future.successful(Ok(views.html.gnuplot()))
  }

  def advancedUser = Action{
    implicit request =>
      Logger.info("Serving page for advanced users ...")
      Ok( views.html.advancedGenerator() )
  }

  def filesPage = AsyncStack(AuthorityKey -> NormalUser) { implicit request =>
    Logger.info("Serving files page ...")
    val user = loggedIn
    Future.successful(Ok(views.html.files()))
  }

  def upload = AsyncStack(parse.multipartFormData, (AuthorityKey -> NormalUser)){
    implicit request =>
      request.body.file("files[]").map { file =>
        import java.io.File
        val filename = file.filename
        val toFile = Future{
          file.ref.moveTo(Files.getOrCreateUserFile(loggedIn, filename))
        }
        val res = toFile.map( f => {
          Ok(Json.toJson(Map("status" -> "ok")))
        })
        res.onComplete{
          case Success(_) => Logger.info(s"Uploaded file $filename !")
          case Failure(e) => Logger.info(s"Upload failed! Error:$e")
        }
        res
      }.getOrElse {
        Future.successful(BadRequest("Missing files"))
      }
  }

  def userFiles = AsyncStack(AuthorityKey -> NormalUser){
    implicit request =>
      import models.DataFile._
      val user = loggedIn
      Logger.info(s"Serving user files")
      Files.getAllUserDataFiles(user).map{
        x => Ok(Json.toJson(x))
      }.fallbackTo(Future.successful(BadRequest("Error!")))
  }

  def deleteFile(fileName: String) = AsyncStack(AuthorityKey -> NormalUser){
    implicit request =>
      val user = loggedIn
      Logger.info(s"Deleting user file: $fileName.")
      Files.deleteFile(user, fileName).map{
        x =>
          Logger.info(s"Deleted file: $fileName.")
          Ok(Json.toJson(Map("status" -> "deleted")))
      }.fallbackTo{
        Future.successful{
          BadRequest(Json.toJson(Map("status" -> "error")))
        }
      }
  }

  def downloadFile(fileName: String) = AsyncStack(AuthorityKey -> NormalUser){
    implicit request =>
      val user = loggedIn
      Logger.info(s"Downloading user file: $fileName.")
      Files.getUserFile(user, fileName).map{
        file =>
          Logger.info(s"Download started - file: $fileName.")
          Ok.sendFile(file)
      }.fallbackTo{
        Future.successful{
          BadRequest(Json.toJson(Map("status" -> "error")))
        }
      }
  }

  def drawSinglePlot = AsyncStack(parse.json, (AuthorityKey -> NormalUser)){
    implicit request =>
      Logger.info("Request for graph plot ...")
      Logger.info(Json.prettyPrint(request.body))
      val user = loggedIn
      val res1 = for {
        project <- Future{request.body.validate[Project].get}
        p <- Files.transformFilePaths(user, project)
        r <- gnuplot.singleRender(p.pages(0), p.graphs,
             Files.getUserScriptFile(user.username),
             Files.getUserOutputFile(user.username, p.pages(0).terminal))
      } yield Ok(r).as(extensionToMIME(p.pages(0).terminal))

      res1.onComplete{
        case Success(_) => Logger.info("Graph created correctly.")
        case Failure(e) => Logger.error(s"Cannot create graph: $e")
      }
      res1.fallbackTo{
        Future.successful(BadRequest("invalid json"))
      }
  }

  def generateProject = AsyncStack(parse.json, AuthorityKey -> NormalUser){
    implicit request =>
      Logger.info("Request for project plot ...")
      Logger.info(Json.prettyPrint(request.body))

      val user = loggedIn
      val res1 = for {
        project <- Future{request.body.validate[Project].get}
        p <- Files.transformFilePaths(user, project)
        r <- gnuplot.multiPageRender(p,
          Files.getUserScriptFiles(user.username, p.pages.length),
          Files.getUserOutputs(user.username, p))
        zippedFiles <- Zip.zipFiles(Files.getUserZipFilePath(user.username), r)
      }  yield Ok(Json.toJson(Map("status" -> "ok")))
      res1.onComplete{
        case Success(_) => Logger.info("Graph created correctly.")
        case Failure(e) =>
          e.printStackTrace()
          Logger.error(s"Cannot create graph: ${e}")
      }
      res1.fallbackTo{
        Future.successful(BadRequest("invalid json"))
      }
  }

  def getProject = AsyncStack(AuthorityKey -> NormalUser){
    implicit request =>
      val user = loggedIn
      Logger.info(s"Downloading user project:")
      Future{
        val file = new java.io.File(Files.getUserZipFilePath(user.username))
        Logger.info(s"Download started - file: ${file.getName}.")
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
      Logger.info("Get user projects")
      val a = accountsDao.getProjects(user.username).map{
        projects =>
          Ok(Json.toJson(projects))
      }
      a.onComplete{
        case a: Any => println(a)
      }
        a.fallbackTo{
          Future.successful{
            BadRequest(Json.toJson(Map("status" -> "error")))
          }
      }
  }

  def saveProject = AsyncStack(parse.json, AuthorityKey -> NormalUser) {
    implicit request =>
      val user = loggedIn
      Logger.info("Save user project")
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

}
