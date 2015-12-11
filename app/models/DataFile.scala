package models
import scala.language.postfixOps

case class DataFile(
                   name: String,
                   size: Long, // size in bytes
                   dataHead: String) // contains first x characters

object DataFile{
  import play.api.libs.json._

  implicit val dataFileWrites: Writes[DataFile] = Json.writes[DataFile]
}

