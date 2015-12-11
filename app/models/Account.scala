package models
import scala.language.postfixOps


sealed trait Role
case object Administrator extends Role{
  override val toString = "Administrator"
}
case object NormalUser extends Role{
  override val toString = "NormalUser"
}

case class Account(
               username: String,
               password: String,
                   role: Role)

object Account{

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  implicit val accountReads: Reads[Account] = (
      (__ \ "username").read[String] and
      (__ \ "password").read[String] and
      (__ \ "role").read[String].map{
        case "Administrator" => Administrator
        case _ => NormalUser
      }
    )(Account.apply _)

  def extractor(account: Account): Option[(String, String, String)] = account match {
    case Account(username, password, role) => Some(username, password, role.toString)
  }

  implicit lazy val accountWrites: OWrites[Account] = (
      (__ \ "username").write[String] and
      (__ \ "password").write[String] and
      (__ \ "role").write[String]
    )(unlift(extractor))

}