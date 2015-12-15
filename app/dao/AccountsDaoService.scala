package dao

import models.Account._
import models.{Account, Project}
import modules.AccountsDao
import play.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection._
import reactivemongo.api.Cursor

import scala.concurrent.Future

case class NoSuchUserException(message: String) extends Exception(message)
case class NoSuchProjectException(message: String) extends Exception(message)

class AccountsDaoService(reactiveMongoApi: ReactiveMongoApi) extends AccountsDao {
  private def accounts: JSONCollection = reactiveMongoApi.db.collection[JSONCollection]("accounts")
  private def projects: JSONCollection = reactiveMongoApi.db.collection[JSONCollection]("projects")

  override def createAccount(account: Account): Future[Unit] =
    accounts.insert(account).map {
      lastError =>
        Logger.debug(s"Account successfully inserted with LastError: $lastError")
    }

  override def findByUsername(username: String): Future[Option[Account]] = {
    val cursor: Cursor[Account] = accounts
      // find all people with username `username`
      .find(Json.obj("username" -> username))
      .cursor[Account]()
    // return the first account

    cursor.collect[List](1).map {
      case Nil => None
      case list => Some(list.head)
    }
  }

  /**
   * Method check whether `username` hasn't already in collection
   * @param username
   * @return true if available false otherwise
   */
  override def checkUsernameAvailability(username: String): Future[Boolean] =
    accounts.count(Some(Json.obj("username" -> username))).map {
      case 0 => true
      case n => false
    }

  /**
   * gets all user projects
   * @param username
   * @return
   */
  override def getProjects(username: String): Future[List[Project]] = {
    val query = Json.obj("author" -> username)
    projects.find(query)
            .cursor[Project]()
            .collect[List]()
  }

  def isUserExists(username: String) = checkUsernameAvailability(username).map(!_)

  def isProjectExists(projectName: String, userName: String) = {
    val query = Json.obj("author" -> userName, "name" -> projectName)
    projects.count(Some(query)).map {
      case 0 => false
      case _ => true
    }
  }

  /**
   * saves project in db
   * @param project - to save
   */
  override def saveOrUpdateProject(project: Project) = {
    val updateProjectQuery = Json.obj("author" -> project.author, "name" -> project.name)
//    lazy val isProjectExistsLazy: Future[Boolean] = isProjectExists(project.name, project.author)
//
//    isUserExists(project.author).filter(_ == true).map {
//      x => isProjectExistsLazy.map {
//        case true  => projects.update(query, project); ()
//        case false => projects.insert(project); ()
//      }
//    }

    for {
      isUserExists <- isUserExists(project.author)
      if isUserExists
      isProjectExistsLazy <- isProjectExists(project.name, project.author)
    } yield isProjectExistsLazy match {
      case true  => projects.update(updateProjectQuery, project); ()
      case false => projects.insert(project); ()
    }
  }

  override def deleteProject(username: String, projectName: String): Future[Unit] = {
    val query = Json.obj("author" -> username, "name" ->projectName)
    lazy val isProjectExistsLazy: Future[Boolean] = isProjectExists(projectName, username)

    isUserExists(username) map {
      case true  =>
        isProjectExistsLazy.map {
          case true  => projects.remove(query); ()
          case false => Future.failed(new NoSuchProjectException(s"Project: $projectName doesn't exists!"))
        }
      case false => Future.failed(new NoSuchUserException(s"User: $username doesn't exists!"))
    }
  }
}
