package util

import java.awt.Color
import play.api.data.validation.ValidationError
import play.api.libs.json.Reads
import util.Parser._

object Validation {
  def equal[T](v: T)(implicit r: Reads[T]): Reads[T] =
    Reads.filter(ValidationError("validate.error.unexpected.value", v))( _ == v )

  def color(implicit r: Reads[String]): Reads[Color] =
    Reads.pattern(rgb, "validate.error.invalid.pattern")
      .map(parseColor)

  def parseInt(implicit r: Reads[String]): Reads[Int] =
    Reads.pattern("""^-?\d+$""".r, "validate.error.invalid.pattern")
      .map(_.toInt)

  def parseDouble(implicit r: Reads[String]): Reads[Double] =
    Reads.pattern("""^-?\d+(\.)?\d*$""".r, "validate.error.invalid.pattern")
      .map(_.toDouble)

  def isInInterval[T](min: T, max: T)(implicit ord: Ordering[T]): T => Boolean =
    x => ord.gteq(x, min) && ord.lteq(x, max)

  def intervalError[T](min: T, max: T) =
    ValidationError(s"validate.error.unexpected.value must from<$min, $max>")
}
