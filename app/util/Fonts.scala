package util
import java.awt.GraphicsEnvironment

object Fonts {
  lazy val allFonts = GraphicsEnvironment.getLocalGraphicsEnvironment.getAvailableFontFamilyNames
  private val chosenFonts = List("Cambria", "Arial", "Open Sans")
  lazy val defaultsFonts = allFonts.filter(chosenFonts.contains(_))
}
