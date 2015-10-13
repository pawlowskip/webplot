import java.awt.Color

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.specs2.runner.JUnitRunner

import util.Parser._

@RunWith(classOf[JUnitRunner])
class ParserSpec extends FunSuite{

  test("parse should works"){
    assert( parseColor("rgb(200,100,2)") == new Color(200,100,2) )
  }

}
