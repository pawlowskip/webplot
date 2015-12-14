package models

import scala.language.postfixOps

case class Axis(name: String,
                label: String,
                fromTo: Option[(String, String)],
                isLogScale: Boolean)

object Axis{
  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  val axises = List("x", "y", "z")

  implicit val axisReads: Reads[Axis] = (
    (__ \ "name").read[String].filter(axises.contains(_)) and
    (__ \ "label").read[String] and
    (__ \ "fromTo").readNullable(
      (__ \ "from").read[String] and
      (__ \ "to").read[String]
      tupled) and
    (__ \ "isLogScale").read[Boolean]
  )(Axis.apply _)

  implicit val axisWrites: OWrites[Axis] = (
    (__ \ "name").write[String] and
    (__ \ "label").write[String] and
    (__ \ "fromTo").writeNullable(
      (__ \ "from").write[String] and
      (__ \ "to").write[String]
      tupled) and
    (__ \ "isLogScale").write[Boolean]
  )(unlift(Axis.unapply))
}
