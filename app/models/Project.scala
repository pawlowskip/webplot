package models

import scala.language.postfixOps

case class Project(name: String,
                   graphs: List[Graph],
                   pages: List[Page],
                   author: String)

object Project {
  import models.Graph._
  import models.Page._
  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  implicit val projectReads: Reads[Project] = (
    (__ \ "name").read[String] and
      (__ \ "graphs").read(Reads.list[Graph]) and
      (__ \ "pages").read(Reads.list[Page]) and
      (__ \ "author").read[String]
    )(Project.apply _)

  implicit val projectWrites: OWrites[Project] = (
    (__ \ "name").write[String] and
      (__ \ "graphs").write(OWrites.list[Graph]) and
      (__ \ "pages").write(OWrites.list[Page]) and
      (__ \ "author").write[String]
    )(unlift(Project.unapply))

}



