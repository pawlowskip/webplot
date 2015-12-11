package controllers


import jp.t2v.lab.play2.auth.LoginLogout
import models.{NormalUser, Account}
import modules.AccountsDao
import play.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller, Result}
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class AuthenticationController (val messagesApi: MessagesApi, val accountsDao: AccountsDao)
  extends Controller
  with I18nSupport
  with LoginLogout
  with AuthConfigImpl
{

  def authenticate(username: String, password: String): Future[Account] = {
    def checkPassword(account: Account): Boolean = {
      import com.github.t3hnar.bcrypt._
      password.isBcrypted(account.password)
    }

    for {
      optUser <- accountsDao.findByUsername(username)
      if optUser.isDefined
      user = optUser.get
      if checkPassword(user)
    } yield user
  }

  
  val loginForm = Form(
    tuple(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    ))

  val registrationForm = Form(
    tuple(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText,
      "retypedPassword" -> nonEmptyText
    ).verifying("Retype password correctly!", f => f._2 == f._3)
  )

  def login(mode: String, error: String) = Action.async {
    implicit request =>
      Logger.debug("Serving login page ...")
      Future.successful(Ok(views.html.login("login", "")))
  }


  def logout = Action.async {
    implicit request =>
      Future.successful(Ok(views.html.login("created", "You are now logged out.")))
  }

  def logoutRequest = Action.async { implicit request =>
    gotoLogoutSucceeded
  }

  def authenticateRequest = Action.async { implicit request =>
    loginForm.bindFromRequest.fold[Future[Result]](
      formWithErrors => Future.successful(BadRequest(views.html.login("login", "Type username and password!"))),
      { case (username, password) =>
        (for {
          account <- authenticate(username, password)
          result <- gotoLoginSucceeded(account.username)
        } yield result).fallbackTo( Future.successful(BadRequest(views.html.login("login", "Incorrect login or password!"))) )
      }
    )
  }

  def registerRequest = Action.async { implicit request =>
    import com.github.t3hnar.bcrypt._
    registrationForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.login("register", formWithErrors.globalError.get.message))),
      { case (username, password, retyped) =>
        (for {
          available <- accountsDao.checkUsernameAvailability(username)
          if available == true
          account = Account(username, password.bcrypt, NormalUser)
          newAccount <- accountsDao.createAccount(account)
          newCatalog = util.Files.createUserCatalog(account)
          result <- Future.successful{Ok(views.html.login("created", "Account created! Log in!"))}
        } yield result).fallbackTo( Future.successful(BadRequest(views.html.login("register", "This username is not available!"))) )
      }
    )
  }

}