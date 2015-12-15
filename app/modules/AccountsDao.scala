package modules

import models.{Account, Project}

import scala.concurrent.Future

trait AccountsDao {
  def createAccount(account: Account): Future[Unit]
  def findByUsername(username: String): Future[Option[Account]]
  def checkUsernameAvailability(username: String): Future[Boolean]
  def saveOrUpdateProject(project: Project): Future[Unit]
  def getProjects(username: String): Future[List[Project]]
  def deleteProject(username: String, projectName: String): Future[Unit]
}
