package models

import util.Parser
import scala.language.postfixOps

case class Page(
  title: String,
  titleSize: Int,
  width: Int,
  height: Int,
  font: (String, Int),
  terminal: String,
  url: String,
  graphRefs: List[GraphRef])

object Page {
  import util.Validation._
  import util.Parser._
  import models.GraphRef._
  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  implicit val pageReads: Reads[Page] = (
      (__ \ "title").read[String] and
      (__ \ "titleSize").read(parseInt) and
      (__ \ "width").read(parseInt) and
      (__ \ "height").read(parseInt) and
      (__ \ "font").read(
        (__ \ "name").read[String] and
        (__ \ "size").read(parseInt)
        tupled
      ) and
      (__ \ "terminal").read(Reads.pattern(terminalRegex)) and
      (__ \ "url").read[String] and
      (__ \ "graphRefs").read( Reads.list[GraphRef] )
    )(Page.apply _)

  implicit val pageWrites: OWrites[Page] = (
      (__ \ "title").write[String] and
      (__ \ "titleSize").write[String].contramap(Parser.intToStr) and
      (__ \ "width").write[String].contramap(Parser.intToStr) and
      (__ \ "height").write[String].contramap(Parser.intToStr) and
      (__ \ "font").write(
          (__ \ "name").write[String] and
          (__ \ "size").write[String].contramap(Parser.intToStr)
          tupled
      ) and
      (__ \ "terminal").write[String] and
      (__ \ "url").write[String] and
      (__ \ "graphRefs").write( OWrites.list[GraphRef] )
    )(unlift(Page.unapply))


}