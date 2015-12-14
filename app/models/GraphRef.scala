package models
import scala.language.postfixOps

case class GraphRef(graphId: Int,
                    x: Double,
                    y: Double,
                    width: Double,
                    height: Double)

object GraphRef {
  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  implicit val graphRefReads: Reads[GraphRef] = (
    (__ \ "graphId").read[Int] and
      (__ \ "x").read[Double] and
      (__ \ "y").read[Double] and
      (__ \ "width").read[Double] and
      (__ \ "height").read[Double]
    )(GraphRef.apply _)

  implicit val graphRefWrites: OWrites[GraphRef] = (
    (__ \ "graphId").write[Int] and
      (__ \ "x").write[Double] and
      (__ \ "y").write[Double] and
      (__ \ "width").write[Double] and
      (__ \ "height").write[Double]
    )(unlift(GraphRef.unapply))
}