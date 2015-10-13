package dao

import modules.AccountsDao
import play.Logger
import play.api.libs.json.{Json}
import play.modules.reactivemongo.{ ReactiveMongoApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import reactivemongo.api.Cursor
import reactivemongo.bson.{BSONObjectID, BSONDocument}
import scala.concurrent.{Future, Promise}
import scala.util.{Success, Failure}
import models.{Project, Account}
import models.Account._
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection._

class AccountsDaoService(reactiveMongoApi: ReactiveMongoApi) extends AccountsDao{

  private def accounts: JSONCollection = reactiveMongoApi.db.collection[JSONCollection]("accounts")
  private def projects: JSONCollection = reactiveMongoApi.db.collection[JSONCollection]("projects")

  class NoSuchUserException(message: String) extends Exception(message)

  def createAccount(account: Account): Future[Unit] =
    accounts.insert(account).map {
      lastError =>
        Logger.info(s"Account successfully inserted with LastError: $lastError")
    }

  def findByUsername(username: String): Future[Option[Account]] = {
    // let's do our query
    val cursor: Cursor[Account] = accounts.
      // find all people with username `username`
      find(Json.obj("username" -> username))
      .cursor[Account]()
    // return the first account
    cursor.collect[List](1).map{
      case Nil => None
      case list => Some(list(0))
    }
  }

  /**
   * Method check whether `username` hasn't already in collection
   * @param username
   * @return true if available false otherwise
   */
  def checkUsernameAvailability(username: String): Future[Boolean] = {
    accounts.count(Some(Json.obj("username" -> username))).map{
      case 0 => true
      case n => false
    }
  }

  /**
   * gets all user projects
   * @param username
   * @return
   */
  def getProjects(username: String): Future[List[Project]] = {
    projects.find(Json.obj("author" -> username))
            .cursor[Project]()
            .collect[List]()
  }

  /**
   * saves project in db
   * @param project - to save
   */
  def saveOrUpdateProject(project: Project) = {
    val isUserExists: Future[Boolean] = checkUsernameAvailability(project.author).map( !_ )
    val query = Json.obj("author" -> project.author, "name" -> project.name)
    lazy val isProjectExists: Future[Boolean] = projects.count(Some(query)).map{
      case 0 => false
      case _ => true
    }

    isUserExists.filter(_ == true).map{
      x => isProjectExists.map{
        case true  => projects.update(query, project); ()
        case false => projects.insert(project); ()
      }
    }
//
//    isUserExists.onComplete{
//      case Success(true) => println("exists")
//      case Success(false) => println("doesn't")
//      case Failure(_) => println("fail")
//    }
//    val p = Promise[Unit]()
//    isUserExists.onComplete{
//      case Success(true)  => projects.insert(project); p.complete(Success(()))
//      case _ => p.complete(Failure())
//    }
//    p.future
  }
}
