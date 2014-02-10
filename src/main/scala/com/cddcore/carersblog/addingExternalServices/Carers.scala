package com.cddcore.carersblog.addingExternalServices

import scala.language.implicitConversions
import scala.xml._

import org.cddcore.engine._
import org.cddcore.engine.tests.CddJunitRunner
import org.joda.time._
import org.joda.time.format._
import org.junit.runner.RunWith

object World {
  def apply(claimDate: String, ninoToCis: NinoToCis): World = apply(Xmls.asDate(claimDate), ninoToCis)
  def apply(claimDate: DateTime, ninoToCis: NinoToCis): World = World(Xmls.asDate("2010-7-5"), claimDate, ninoToCis)
}
case class World(dateProcessingData: DateTime, dateOfClaim: DateTime, ninoToCis: NinoToCis) extends LoggerDisplay {
  def loggerDisplay(dp: LoggerDisplayProcessor): String =
    "World(" + dateOfClaim + ")"
}

trait NinoToCis {
  def apply(nino: String): Elem
}

class TestNinoToCis extends NinoToCis {
  def apply(nino: String) =
    try {
      val full = s"Cis/${nino}.txt"
      val url = getClass.getClassLoader.getResource(full)
      if (url == null)
        <NoCis/>
      else {
        val xmlString = scala.io.Source.fromURL(url).mkString
        val xml = XML.loadString(xmlString)
        xml
      }
    } catch {
      case e: Exception => throw new RuntimeException("Cannot load " + nino, e)
    }
}

case class KeyAndParams(key: String, params: Any*)

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
  lazy val DependantNino = xml(validateClaimXml) \ "DependantData" \ "DependantNINO" \ string
  lazy val dependantCisXml: Elem = DependantNino.get() match {
    case Some(s) => w.ninoToCis(s);
    case None => <NoDependantXml/>
  }
  lazy val dependantLevelOfQualifyingCare = xml(dependantCisXml) \\ "AwardComponent" \ string
  lazy val dependantHasSufficientLevelOfQualifyingCare = dependantLevelOfQualifyingCare() == "DLA Middle Rate Care"
}

@RunWith(classOf[CddJunitRunner])
object Carers {
  implicit def stringStringToCarers(x: Tuple2[String, String]) = CarersXmlSituation(World(x._1, new TestNinoToCis), Xmls.validateClaim(x._2))
  implicit def toKeyAndParams(x: String) = Some(KeyAndParams(x))
  val engine = Engine.folding[CarersXmlSituation, Option[KeyAndParams], List[KeyAndParams]]((acc, opt) => acc ::: opt.toList, List()).title("ValidateClaim").
    code((c: CarersXmlSituation) => None).
    
    childEngine("Age Restriction", "Customers under age 16 are not entitled to Carers Allowance").
    scenario(("2010-3-1", "CL100104A"), "Cl100104A-Age Under 16").expected("carer.claimant.under16").
    because((c: CarersXmlSituation) => c.underSixteen).

    childEngine("Caring hours", "Customers with Hours of caring must be 35 hours or more in any one week").
    scenario(("2010-1-1", "CL100105A"), "CL100105A-lessThen35Hours").
    expected("carer.claimant.under35hoursCaring").
    because((c: CarersXmlSituation) => !c.claim35Hours()).

    childEngine("Qualifying Benefit", "Dependant Party's without the required level of qualyfing benefit will result in the disallowance of the claim to Carer.").
    scenario(("2010-6-23", "CL100106A"), "CL100106A-Without qualifying benefit").
    expected(("carer.qualifyingBenefit.dpWithoutRequiredLevelOfQualifyingBenefit")).
    because((c: CarersXmlSituation) => !c.dependantHasSufficientLevelOfQualifyingCare).

    childEngine("UK Residence", "Customer who is not considered resident and present in GB is not entitled to CA.").
    scenario(("2010-6-7", "CL100107A"), "CL100107A-notInGB").
    expected("carers.claimant.notResident").
    because((c: CarersXmlSituation) => !c.ClaimAlwaysUK()).

    childEngine("Immigration Status", "Customers who have restrictions on their immigration status will be disallowed CA.").
    scenario(("2010-6-7", "CL100108A"), "CL100108A-restriction on immigration status").
    expected("carers.claimant.restriction.immigrationStatus").
    because((c: CarersXmlSituation) => !c.ClaimCurrentResidentUK()).

    childEngine("Full Time Eduction", "Customers in Full Time Education 21 hours or more each week are not entitled to CA.").
    scenario(("2010-2-10", "CL100109A"), "CL100109A-full time education").
    expected("carers.claimant.fullTimeEduction.moreThan21Hours").
    because((c: CarersXmlSituation) => c.ClaimEducationFullTime()).

    build

  def main(args: Array[String]) {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
    println(engine(("2010-3-1", "CL100104A")))
  }
}