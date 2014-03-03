package com.cddcore.carersblog.addingFirstRequirement

import org.joda.time._
import org.joda.time.format._
import scala.xml._
import org.junit.runner.RunWith
import org.cddcore.engine._
import org.cddcore.engine.tests.CddJunitRunner
import scala.language.implicitConversions

object World {
  def apply(claimDate: String): World = apply(Xmls.asDate(claimDate))
  def apply(claimDate: DateTime): World = World(Xmls.asDate("2010-7-5"), claimDate)
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

case class CarersXmlSituation(w: World, validateClaimXml: Elem) extends XmlSituation {
  import Xml._
  lazy val birthdate = xml(validateClaimXml) \ "ClaimantData" \ "ClaimantBirthDate" \ "PersonBirthDate" \ date
  lazy val claim35Hours = xml(validateClaimXml) \ "ClaimData" \ "Claim35Hours" \ yesNo(default = false)
  lazy val ClaimCurrentResidentUK = xml(validateClaimXml) \ "ClaimData" \ "ClaimCurrentResidentUK" \ yesNo(default = false)
  lazy val ClaimEducationFullTime = xml(validateClaimXml) \ "ClaimData" \ "ClaimEducationFullTime" \ yesNo(default = false)
  lazy val ClaimAlwaysUK = xml(validateClaimXml) \ "ClaimData" \ "ClaimAlwaysUK" \ yesNo(default = false)
  lazy val underSixteen = birthdate.get() match {
    case Some(bd) => bd.plusYears(16).isAfter(w.dateProcessingData)
    case _ => false
  }
}

@RunWith(classOf[CddJunitRunner])
object Carers {
  implicit def stringStringToCarers(x: Tuple2[String, String]) = CarersXmlSituation(World(x._1), Xmls.validateClaim(x._2))
  implicit def toKeyAndParams(x: String) = KeyAndParams(x)
  val engine = Engine[CarersXmlSituation, KeyAndParams]().title("ValidateClaim").
       useCase("Age Restriction", "Customers under age 16 are not entitled to Carers Allowance").
    scenario(("2010-3-1", "CL100104A"), "Cl100104A-Age Under 16").expected("carer.claimant.under16").
    because((c: CarersXmlSituation) => c.underSixteen).
    
    useCase("Caring hours", "Customers with Hours of caring must be 35 hours or more in any one week").
    scenario(("2010-1-1", "CL100105A"), "CL100105A-lessThen35Hours").
    expected("carer.claimant.under35hoursCaring").
    because((c: CarersXmlSituation) => !c.claim35Hours()).

    useCase("Qualifying Benefit", "Dependant Party's without the required level of qualyfing benefit will result in the disallowance of the claim to Carer.").
    scenario(("2010-6-23", "CL100106A"), "CL100106A-Without qualifying benefit").
    expected(("carer.qualifyingBenefit.dpWithoutRequiredLevelOfQualifyingBenefit")).

    useCase("UK Residence", "Customer who is not considered resident and present in GB is not entitled to CA.").
    scenario(("2010-6-7", "CL100107A"), "CL100107A-notInGB").
    expected("carers.claimant.notResident").
    because((c: CarersXmlSituation) => !c.ClaimAlwaysUK()).

    useCase("Immigration Status", "Customers who have restrictions on their immigration status will be disallowed CA.").
    scenario(("2010-6-7", "CL100108A"), "CL100108A-restriction on immigration status").
    expected("carers.claimant.restriction.immigrationStatus").
    because((c: CarersXmlSituation) => !c.ClaimCurrentResidentUK()).

    useCase("Full Time Eduction", "Customers in Full Time Education 21 hours or more each week are not entitled to CA.").
    scenario(("2010-2-10", "CL100109A"), "CL100109A-full time education").
    expected("carers.claimant.fullTimeEduction.moreThan21Hours").
    because((c: CarersXmlSituation) => c.ClaimEducationFullTime()).

    build

  def main(args: Array[String]) {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
    println(engine(("2010-3-1", "CL100104A")))
  }
}