package models

import java.awt.Color

import models.PlotType.PlotType
import util.Parser

import scala.language.postfixOps
import scala.util.Try

case class Plot(label: String,
                plotDataType: String,
                dataFun: String,
                dataFile: String,
                plotType: PlotType,
                using: String,
                patternType: Int,
                fillType: String,
                lineType: Int,
                pointType: Int,
                lineWidth: Int,
                pointSize: Double,
                color: Color)

object PlotType extends Enumeration {
  type PlotType = Value
  val LINES = Value("lines")
  val POINTS = Value("points")
  val LINESPOINTS = Value("linespoints")
  val FILLEDCURVE = Value("filledcurve")
  val XERRORBARS = Value("xerrorbars")
  val YERRORBARS = Value("yerrorbars")
  val XYERRORBARS = Value("xyerrorbars")
}

object Plot{
  import play.api.libs.functional.syntax._
  import util.Parser._
  import util.Validation._
  import play.api.libs.json._

  implicit val plotReads: Reads[Plot] = (
        (__ \ "label").read[String] and
        (__ \ "plotDataType").read(equal("f") or equal("d")) and
        (__ \ "dataFun").read[String] and
        (__ \ "dataFile").read[String] and
        (__ \ "plotType").read[String].map(Try{PlotType.withName(_)}.getOrElse((x:String) => PlotType.LINES)) and
        (__ \ "using").read[String] and
        (__ \ "patternType").read(parseInt) and
        (__ \ "fillType").read(Reads.pattern(filledCurvePatternRegex)) and
        (__ \ "lineType").read(parseInt).filter(intervalError(1, 5))(isInInterval(1, 5)) and
        (__ \ "pointType").read(parseInt).filter(intervalError(1, 15))(isInInterval(1, 15)) and
        (__ \ "lineWidth").read(parseInt).filter(intervalError(1, 99))(isInInterval(1, 99)) and
        (__ \ "pointSize").read(parseDouble).filter(intervalError(0.0, 10.0))(isInInterval(0.0, 10.0)) and
        (__ \ "color").read( color )
    )(Plot.apply _)

  implicit val plotOWrites: OWrites[Plot] = (
      (__ \ "label").write[String] and
      (__ \ "plotDataType").write[String] and
      (__ \ "dataFun").write[String] and
      (__ \ "dataFile").write[String] and
      (__ \ "plotType").write[String].contramap( (x: PlotType) => x.toString ) and
      (__ \ "using").write[String] and
      (__ \ "patternType").write[String].contramap(Parser.intToStr) and
      (__ \ "fillType").write[String] and
      (__ \ "lineType").write[String].contramap(Parser.intToStr) and
      (__ \ "pointType").write[String].contramap(Parser.intToStr) and
      (__ \ "lineWidth").write[String].contramap(Parser.intToStr) and
      (__ \ "pointSize").write[String].contramap(Parser.doubleToStr) and
      (__ \ "color").write[String].contramap(Parser.colorRgbToStr)
    )(unlift(Plot.unapply))

}
