package models
import scala.language.postfixOps

/**
 * Class contains info about data file
 * @param name - name of file
 * @param size - size in bytes
 * @param dataHead - contains first n characters
 */
case class DataFile(name: String,
                    size: Long,
                    dataHead: String)

object DataFile{
  import play.api.libs.json._

  implicit val dataFileWrites: Writes[DataFile] = Json.writes[DataFile]
}

