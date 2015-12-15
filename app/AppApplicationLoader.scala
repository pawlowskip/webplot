import akka.actor.ActorSystem
import com.softwaremill.macwire._
import controllers.{AppController, Assets, AuthenticationController}
import dao.AccountsDaoService
import gnuplot.GnuplotService
import modules.{AccountsDao, Gnuplot}
import play.api.ApplicationLoader.Context
import play.api._
import play.api.i18n.I18nComponents
import play.api.inject.DefaultApplicationLifecycle
import play.api.routing.Router
import play.modules.reactivemongo._
import router.Routes

class AppApplicationLoader extends ApplicationLoader {
  def load(context: Context) = {
    (new BuiltInComponentsFromContext(context) with AppComponents).application
  }
}

trait AppComponents extends BuiltInComponents with AppModule {
  lazy val assets: Assets = wire[Assets]
  lazy val router: Router = wire[Routes] withPrefix "/"
}

trait AppModule
  extends I18nComponents // for i18n support
  with ReactiveMongoComponents
{
  def configuration: Configuration
  def actorSystem: ActorSystem
  def applicationLifecycle: DefaultApplicationLifecycle

  lazy val reactiveMongoApi = new DefaultReactiveMongoApi(actorSystem, configuration, applicationLifecycle)

  // Define services bindings
  lazy val gnuplotService: Gnuplot = wire[GnuplotService]
  lazy val accountsDaoService: AccountsDao = wire[AccountsDaoService]

  // Define controllers bindings
  lazy val appController: AppController = wire[AppController]
  lazy val authController: AuthenticationController = wire[AuthenticationController]

}
