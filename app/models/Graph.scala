package models

import java.awt.Color
import util.{Parser}
import GraphType.GraphType

import scala.util.Try
import scala.language.postfixOps

case class Graph(
                  title: String,
                  graphType: GraphType, // 2d, 3d, hist, cont, pm3d
                  xAxis: Axis,
                  yAxis: Axis,
                  zAxis: Axis,
                  view: (Int, Int),
                  samples:(Int, Int, Int),
                  grid: (Double, String), // empty string = no grid, x, y, x y
                  legend: String, //
                  width: Double,
                  height: Double,
                  position: (Double, Double),
                  contourFontSize: Double,
                  contourLevels: Int,
                  histogramType: String, // rows, cluster
                  histogramGap: Double,
                  histogramBoxWidth: Double,// relative
                  histogramBorder: (String, Color), // np. fill solid border
                  palette: List[Color],
                  plots: List[Plot])


object GraphType extends Enumeration{
  type GraphType = Value
  val PL2D = Value("2D")
  val PL3D = Value("3D")
  val HIST = Value("HIST")
  val CONT = Value("CONT")
  val PM3D = Value("PM3D")
}

object Graph{
  import util.Validation._
  import util.Parser._
  import play.api.libs.json._
  import play.api.libs.functional.syntax._
  import Axis.axisReads
  import Plot.plotReads

  implicit val graphReads: Reads[Graph] = (
      (__ \ "title").read[String] and
      (__ \ "graphType").read[String].map( Try{GraphType.withName(_)}.getOrElse( (x:String) => GraphType.PL2D) ) and
      (__ \ "xAxis").read[Axis] and
      (__ \ "yAxis").read[Axis] and
      (__ \ "zAxis").read[Axis] and
      (__ \ "view").read(
        (__ \ "viewX").read(parseInt) and
        (__ \ "viewY").read(parseInt)
        tupled
      ) and
      (__ \ "samples").read(
        (__ \ "samplesX").read(parseInt) and
        (__ \ "samplesY").read(parseInt) and
        (__ \ "samplesZ").read(parseInt)
        tupled
      ) and
      (__ \ "grid").read(
        (__ \ "gridWidth").read(parseDouble) and
        (__ \ "gridType").read(Reads.pattern(gridRegex))
        tupled
      ) and
      (__ \ "legend").read(
        (__ \ "isUsed").read[String] and
        (__ \ "place").read[String] and
        (__ \ "horiPos").read[String] and
        (__ \ "vertPos").read[String] and
        (__ \ "adjust").read[String] and
        (__ \ "box").read[String]
        tupled
      ).map(parseLegend) and
      (__ \ "width").read(parseDouble) and
      (__ \ "height").read(parseDouble) and
      (__ \ "position").read(
        (__ \ "posX").read(parseDouble) and
        (__ \ "posY").read(parseDouble)
        tupled
      ) and
      (__ \ "contourFontSize").read(parseDouble) and
      (__ \ "contourLevels").read(parseInt) and
      (__ \ "histogramType").read(Reads.pattern(histogramTypeRegex)) and
      (__ \ "histogramGap").read(parseDouble) and
      (__ \ "histogramBoxWidth").read(parseDouble) and
      (__ \ "histogramBorder").read(
        (__ \ "type").read( Reads.list[String]).map(parseHistoramBorderType) and
        (__ \ "color").read(color)
        tupled
      ) and
      (__ \ "palette").read( Reads.list[String] ).map( x => x.map( s => parseColor(s) ) ) and
      (__ \ "plots").read( Reads.list[Plot] )
    )(Graph.apply _)

  implicit val graphWrites: OWrites[Graph] = (
      (__ \ "title").write[String] and
      (__ \ "graphType").write[String].contramap( (x: GraphType) => x.toString ) and
      (__ \ "xAxis").write[Axis] and
      (__ \ "yAxis").write[Axis] and
      (__ \ "zAxis").write[Axis] and
      (__ \ "view").write(
        (__ \ "viewX").write[String].contramap(Parser.intToStr) and
        (__ \ "viewY").write[String].contramap(Parser.intToStr)
        tupled
      ) and
      (__ \ "samples").write(
        (__ \ "samplesX").write[String].contramap(Parser.intToStr) and
        (__ \ "samplesY").write[String].contramap(Parser.intToStr) and
        (__ \ "samplesZ").write[String].contramap(Parser.intToStr)
        tupled
      ) and
      (__ \ "grid").write(
        (__ \ "gridWidth").write[String].contramap(Parser.doubleToStr) and
        (__ \ "gridType").write[String]
        tupled
      ) and
      (__ \ "legend").write(
        (__ \ "isUsed").write[String] and
        (__ \ "place").write[String] and
        (__ \ "horiPos").write[String] and
        (__ \ "vertPos").write[String] and
        (__ \ "adjust").write[String] and
        (__ \ "box").write[String]
        tupled
      ).contramap((x: String) => Parser.unapplyLegend(x)) and
      (__ \ "width").write[String].contramap(Parser.doubleToStr) and
      (__ \ "height").write[String].contramap(Parser.doubleToStr) and
      (__ \ "position").write(
        (__ \ "posX").write[String].contramap(Parser.doubleToStr) and
        (__ \ "posY").write[String].contramap(Parser.doubleToStr)
        tupled
      ) and
      (__ \ "contourFontSize").write[String].contramap(Parser.doubleToStr) and
      (__ \ "contourLevels").write[String].contramap(Parser.intToStr) and
      (__ \ "histogramType").write[String] and
      (__ \ "histogramGap").write[String].contramap(Parser.doubleToStr) and
      (__ \ "histogramBoxWidth").write[String].contramap(Parser.doubleToStr) and
      (__ \ "histogramBorder").write(
        (__ \ "type").write(Writes.list[String]).contramap((x: String) => Parser.unapplyHistogramBorderType(x)) and
        (__ \ "color").write[String].contramap(Parser.colorRgbToStr)
        tupled
      ) and
      (__ \ "palette").write( OWrites.list[String] ).contramap( (list: List[Color]) => list.map( Parser.colorRgbToStr(_) ) ) and
      (__ \ "plots").write( OWrites.list[Plot] )
    )(unlift(Graph.unapply))

}