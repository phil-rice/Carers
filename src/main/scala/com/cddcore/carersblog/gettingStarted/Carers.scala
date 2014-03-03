package com.cddcore.carersblog.gettingStarted

import org.joda.time._
import org.joda.time.format._
import scala.xml._
import org.junit.runner.RunWith
import org.cddcore.engine._
import org.cddcore.engine.tests.CddJunitRunner
import scala.language.implicitConversions

object World {
  def apply(claimDate: String): World = apply(Xmls.asDate(claimDate))
  def apply(claimDate: DateTime): World = World(Xmls.asDate("2013-7-5"), claimDate)
}
case class World(dateProcessingData: DateTime, dateOfClaim: DateTime) extends LoggerDisplay {
  def loggerDisplay(dp: LoggerDisplayProcessor): String =
    "World(" + dateOfClaim + ")"
}
case class KeyAndParams(key: String, params: Any*) {
  override def toString = "<" + key + params.mkString("(", ",", ")") + ">"
}
object Xmls {

  def validateClaim(id: String) = {
    try {
      val full = s"ValidateClaim/${id}.xml"
      val url = getClass.getClassLoader.getResource(full)
      val xmlString = scala.io.Source.fromURL(url).mkString
      val xml = XML.loadString(xmlString)
      xml
    } catch {
      case e: Exception => throw new RuntimeException("Cannot load " + id, e)
    }
  }
  private val formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
  def asDate(s: String): DateTime = formatter.parseDateTime(s);
}

case class CarersXmlSituation(w: World, validateClaimXml: Elem) extends XmlSituation

@RunWith(classOf[CddJunitRunner])
object Carers {
  implicit def stringStringToCarers(x: Tuple2[String, String]) = CarersXmlSituation(World(x._1), Xmls.validateClaim(x._2))
  val engine = Engine[CarersXmlSituation, KeyAndParams]().
    build

  def main(args: Array[String]) {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
    println(engine(("2010-3-1", "CL100104A")))
  }
}