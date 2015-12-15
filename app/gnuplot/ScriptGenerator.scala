package gnuplot
import java.awt.Color

import models._

private [gnuplot] object ScriptGenerator {

  def generateScript(page: Page, graphs: List[Graph], outputFilePath: String): String = {
    def multiPlotTitle = page.title match {
      case "" => ""
      case _  => s"""title "${page.title}" font ",${page.titleSize}""""
    }

    s"""|${terminal(page)}
        |${output(outputFilePath)}
        |set multiplot $multiPlotTitle
        |${(for(g <- page.graphRefs) yield graph(graphs, g)).mkString("", "reset\n", "reset\n")}
        |unset multiplot
        |quit
        |""".stripMargin
  }

  private def graph(graphs: List[Graph], graph: GraphRef): String = {

    val g = graphs(graph.graphId)
              .copy(width = graph.width,
                    height = graph.height,
                    position = (graph.x, graph.y))

    val head = s"""|set size ${g.width},${g.height}
        |set origin ${g.position._1},${g.position._2}
        |${legend(g.legend)}
        |set title "${g.title}"
        |${grid(g.grid)}
        |""".stripMargin

    val body = g.graphType match {
      case GraphType.PL2D =>
        s"""|set samples ${g.samples._1}, ${g.samples._2}
            |${axis(g.xAxis)}
            |${axis(g.yAxis)}
            |plot ${(for (p <- g.plots)
              yield s"${plotData(p)}  ${plotWith(p)}").mkString("", ", ", " \n")}
            |""".stripMargin

      case GraphType.PL3D =>
        s"""|set samples ${g.samples._1}, ${g.samples._2}
            |set isosamples ${g.samples._3}
            |${axis(g.xAxis)}
            |${axis(g.yAxis)}
            |${axis(g.zAxis)}
            |set view ${g.view._1}, ${g.view._2}
            |splot ${(for (p <- g.plots)
              yield s"${plotData(p)}  ${plotWith(p)}").mkString("", ", ", " \n")}
            |""".stripMargin

      case GraphType.CONT =>
        s"""|set contour
            |set cntrlabel font ',${g.contourFontSize}'
            |set cntrparam levels ${g.contourLevels}
            |set view map scale 1
            |unset surface
            |${axis(g.xAxis)}
            |${axis(g.yAxis)}
            |${axis(g.zAxis)}
            |set isosamples ${g.samples._1}, ${g.samples._2}
            |splot ${(for (p <- g.plots)
              yield s"${plotData(p)}").mkString("", ", ", " \n")}
            |""".stripMargin

      case GraphType.PM3D =>
        s"""|set pm3d
           |set hidden3d
           |set isosamples ${g.samples._3}
            |unset surface
            |${axis(g.xAxis)}
            |${axis(g.yAxis)}
            |${axis(g.zAxis)}
            |set view map scale 1
            |${palette(g.palette)}
            |splot ${(for(p <- g.plots)
              yield s"${plotData(p)} title '${p.label}' with pm3d").mkString("", ", ", " \n")}
            |""".stripMargin

      case GraphType.HIST =>
        s"""|set style data histogram
            |${histogramType(g)}
            |${histogramBorder(g)}
            |set boxwidth ${g.histogramBoxWidth} relative
            |set xtic rotate by -45 scale 0
            |plot ${histogramPlots(g.plots)}
            |""".stripMargin

      case _ => ""
    }

    head + body
  }

  private def plotWith(p: Plot): String = {
    val main = p.plotType match {
      case PlotType.LINES => s"with lines dt ${p.lineType} lc rgb ${color(p.color)} lw ${p.lineWidth}"
      case PlotType.LINESPOINTS => s"with linespoints dt ${p.lineType} lc rgb ${color(p.color)} lw ${p.lineWidth}  ps ${p.pointSize} pt ${p.pointType}"
      case PlotType.POINTS => s"with points lc rgb ${color(p.color)} pt ${p.pointType} ps ${p.pointSize}"
      case PlotType.FILLEDCURVE => s"with filledcurve ${p.fillType} fs pattern ${p.patternType} lc rgb ${color(p.color)}"
      case PlotType.XERRORBARS => s"with xerrorbars ls 7 lc rgb ${color(p.color)}"
      case PlotType.YERRORBARS => s"with yerrorbars ls 7 lc rgb ${color(p.color)}"
      case PlotType.XYERRORBARS => s"with xyerrorbars ls 7 lc rgb ${color(p.color)}"
    }
    val title = p.label match {
      case "" => "notitle"
      case _ => s"""title "${p.label}""""
    }
    main + " " + title
  }

  private def plotData(p: Plot): String = p.plotDataType match {
    case "f" => p.dataFun
    case "d" => s"'${p.dataFile}' using ${p.using}"
    case _ => ""
  }

  private def histogramPlots(plots: List[Plot]) = plots match {
    case Nil => ""
    case _ => s"'${plots.head.dataFile}' using ${plots.head.using}:xtic(1) t col lc rgb ${color(plots.head.color)}" +
      (for(p <- plots.drop(1)) yield s"'' using ${p.using} title  col lc rgb ${color(p.color)}").mkString(", ", " , ", "\n")
  }

  private def histogramType(g: Graph) =
    g.histogramType match {
      case "row" => "set style histogram rows"
      case "cluster" => s"set style histogram cluster gap ${g.histogramGap}"
      case _ => ""
    }

  private def histogramBorder(g: Graph) = {
    g.histogramBorder match {
      case (a, b) if a.endsWith("noborder")  => s"set style fill $a"
      case (a, b) => s"set style fill $a rgb ${color(b)}"
    }
  }

  private def setParametrization(params: List[(String, String, String)]): String =
    params match {
      case Nil => ""
      case _ =>
        val dummy = params.map{ case (name, from, to) => name }.mkString("set dummy ", " ,", "\n")
        val parametric = params.map{
          case (name, from, to) => s"${range(name, from, to)}"
        }.mkString("set parametric\n", "\n", "\n")
        dummy + parametric
    }

  private def axis(axis: Axis) = {
    val setRange = axis.fromTo match {
      case Some((from, to)) => range(axis.name, from, to) + "\n"
      case None => ""
    }
    val label = s"""set ${axis.name}label "${axis.label}" \n"""
    val logScale = axis.isLogScale match {
      case true => s"""set logscale ${axis.name} \n"""
      case false => ""
    }
    label + setRange + logScale
  }

  private def grid(params: (Double, String)) = params match {
    case (size: Double, "None") => ""
    case (size, gridType) =>  s"""set grid layerdefault linewidth $size $gridType"""
  }

  private def range(name: String, from: String, to: String) =
    s"set ${name}range [$from:$to]"

  private def terminal(page: Page): String = page.terminal match {
    case "svg" => s"set term svg size ${page.width},${page.height} fname '${page.font._1}' fsize ${page.font._2}"
    case "png" => s"set terminal png size ${page.width},${page.height} enhanced font '${page.font._1},${page.font._2}'"
    case "eps" => s"set terminal postscript eps size ${page.width/100},${page.height/100} font '${page.font._1},${page.font._2}'"
    case "pdf" => s"set terminal pdfcairo size ${page.width/100},${page.height/100} font '${page.font._1},${page.font._2}'"
    case _ => ""
  }

  private def output(path: String) = s"set output '$path'"

  private def legend(param: String) = param match {
    case "" => "unset key"
    case _ => s"set key $param"
  }

  private def color(c: Color) = c.getAlpha match {
    case 255 => s""""#${Integer.toHexString(c.getRGB).substring(2)}""""
    case _ => s""""#${Integer.toHexString(c.getRGB)}""""
  }

//  {
//    val (a, r, g, b) = (c.getAlpha, c.getRed, c.getGreen, c.getBlue)
//
//    a match {
//      case 255 => s""""#${Integer.toHexString(c.getRGB).substring(2)}""""
//      case _ => s""""#${Integer.toHexString(c.getRGB)}""""
//    }
//  }

  private def palette(gradient: List[Color]) = {
    val colorsDefinition = for {
      (c, n) <- gradient.zipWithIndex
    } yield s"""$n ${color(c)}"""
    val definitionString = colorsDefinition.mkString("( ", ", ", " )")
    s"set palette defined $definitionString"
  }

}
