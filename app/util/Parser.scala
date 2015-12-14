package util

import java.awt.Color


object Parser {
  val rgba = """^rgba\((\d{1,3}),(\d{1,3}),(\d{1,3}),(\d(\.\d+)?)\)$""".r
  val rgb = """^rgb\((\d{1,3}),(\d{1,3}),(\d{1,3})\)$""".r
  val gridRegex = """^(x|y|x y|None)$""".r // matches `x` `y` `x y` `None`
  val legendRegex = """^(on|off) (outside|inside) (left|right|center) (top|bottom|center) (vertical|horizontal) (box|nobox)$""".r
  val histogramTypeRegex = """^(row|cluster)$""".r // matches `row` `cluster`
  val histogramBorderRegex = """^(empty|solid) (border|noborder)$""".r
  val terminalRegex = """^(svg|png|eps|pdf)$""".r
  val filledCurvePatternRegex = """^(x1|x2|y1|y2)$""".r
  val fileExtension = """^.+\.([a-z]+)$""".r

  def parseColor(color: String): Color = color match {
    case rgba(r, g, b, a) => new Color(r.toInt, g.toInt, b.toInt, a.toInt * 255)
    case rgb(r, g, b) => new Color(r.toInt, g.toInt, b.toInt)
    case _ => new Color(0, 0, 0)
  }

  def parseLegend(params: (String, String, String, String, String, String)): String = params match {
    case (a, b, c, d, e, f) => 
      s"$a $b $c $d $e $f" match {
        case legendRegex(g, h, i, j, k, l) if g != "off" => s"$h $i $j $k $l"
        case _ => ""
      }
    case _ => ""
  }
  
  def unapplyLegend(legend: String): (String, String, String, String, String, String) = legend match {
    case legendRegex(a, b, c, d, e, f) => (a, b, c, d, e, f)
    case _ => ("off", "outside", "left", "top", "vertical", "box")
  }

  def parseHistogramBorderType(params: List[String]): String = params.mkString(" ") match {
    case histogramBorderRegex(a, b) => s"$a $b"
    case _ => ""
  }

  def unapplyHistogramBorderType(hist: String) = hist match {
    case histogramBorderRegex(a, b) => List(a, b)
    case _ => List("solid", "border")
  }

  val doubleToStr = (d: Double) => d.toString
  val intToStr = (i: Int) => i.toString
  val colorRgbToStr = (c: Color) => s"rgb(${c.getRed},${c.getGreen},${c.getBlue})"

}
