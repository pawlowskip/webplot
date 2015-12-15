package util

import java.io._
import java.util.zip.{ZipEntry, ZipOutputStream}

import util.MyExecutionContext._

import scala.concurrent.Future

object Zip {
  def zipFiles(outputPath: String, files: List[File]): Future[File] = {
    val fos = new FileOutputStream(outputPath)
    val zipOut = new ZipOutputStream(new BufferedOutputStream(fos))
    Future{
      for(input <- files){
        val fis = new FileInputStream(input)
        val ze = new ZipEntry(input.getName)
        zipOut.putNextEntry(ze)
        val tmp = new Array[Byte](4*1024)
        var size = 0
        while(size != -1 ){
          zipOut.write(tmp, 0, size)
          size = fis.read(tmp)
        }
        zipOut.flush()
        fis.close()
      }
      zipOut.close()
      new File(outputPath)
    }
  }

  def zip(out: String, files: Iterable[String]): Future[File] = {
    val zip = new ZipOutputStream(new FileOutputStream(out))
    Future {
      files.foreach { name =>
        zip.putNextEntry(new ZipEntry(name))
        val in = new BufferedInputStream(new FileInputStream(name))
        var b = in.read()
        while (b > -1) {
          zip.write(b)
          b = in.read()
        }
        in.close()
        zip.closeEntry()
      }
      zip.close()
      new File(out)
    }
  }

}
