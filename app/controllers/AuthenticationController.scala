package controllers

import jp.t2v.lab.play2.auth.LoginLogout
import models.{Account, NormalUser}
import modules.AccountsDao
import play.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{Action, Controller, Result}

import scala.concurrent.Future

class AuthenticationController(val accountsDao: AccountsDao)
  extends Controller
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
    )
  )

  private def checkForm(inputs: (String, String, String)): Boolean = inputs match {
    case (_, password, retyped) => password == retyped
  }

  val registrationForm = Form(
    tuple(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText,
      "retypedPassword" -> nonEmptyText
    ).verifying("Retype password correctly!", checkForm _)
  )

  def login(mode: String, error: String) = Action.async { implicit request =>
    Logger.debug("Serving login page ...")
    Future.successful(Ok(views.html.login("login", "")))
  }


  def logout = Action.async { implicit request =>
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

  def registerNewUser(inputs: (String, String, String)): Future[Result] = inputs match {
    case (username, password, retyped) =>
      import com.github.t3hnar.bcrypt._

      val result = for {
        available <- accountsDao.checkUsernameAvailability(username)
        if available
        account = Account(username, password.bcrypt, NormalUser)
        newAccount <- accountsDao.createAccount(account)
        newCatalog = util.Files.createUserCatalog(account)
      } yield Ok(views.html.login("created", "Account created! Log in!"))

      result.fallbackTo( Future.successful(BadRequest(views.html.login("register", "This username is not available!"))) )
  }


  def registerRequest = Action.async { implicit request =>
    registrationForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.login("register", formWithErrors.globalError.get.message))),
      inputs => registerNewUser(inputs)
    )
  }

}