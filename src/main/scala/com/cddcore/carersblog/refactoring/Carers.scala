package com.cddcore.carersblog.refactoring

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
  lazy val DependantNino = xml(validateClaimXml) \ "DependantData" \ "DependantNINO" \ string
  lazy val dependantCisXml: Elem = DependantNino.get() match {
    case Some(s) => w.ninoToCis(s);
    case None => <NoDependantXml/>
  }
  lazy val dependantLevelOfQualifyingCare = xml(dependantCisXml) \\ "AwardComponent" \ string
  lazy val dependantHasSufficientLevelOfQualifyingCare = dependantLevelOfQualifyingCare() == "DLA Middle Rate Care"

  lazy val hasChildExpenses = xml(validateClaimXml) \ "ExpensesData" \ "ExpensesChild" \ yesNo(default = false)
  lazy val childExpensesAcount = xml(validateClaimXml) \ "ExpensesData" \ "ExpensesChildAmount" \ double
  lazy val hasPsnPension = xml(validateClaimXml) \ "ExpensesData" \ "ExpensesPsnPension" \ yesNo(default = false)
  lazy val psnPensionAcount = xml(validateClaimXml) \ "ExpensesData" \ "ExpensesPsnPensionAmount" \ double
  lazy val hasOccPension = xml(validateClaimXml) \ "ExpensesData" \ "ExpensesOccPension" \ yesNo(default = false)
  lazy val occPensionAcount = xml(validateClaimXml) \ "ExpensesData" \ "ExpensesOccPensionAmount" \ double
  lazy val hasEmploymentData = xml(validateClaimXml) \ "newEmploymentData" \ boolean
  lazy val employmentGrossSalary = xml(validateClaimXml) \ "EmploymentData" \ "EmploymentGrossSalary" \ double
  lazy val employmentPayPeriodicity = xml(validateClaimXml) \ "EmploymentData" \ "EmploymentPayPeriodicity" \ string

  lazy val nettIncome = Income.income(this) - Expenses.expenses(this)
  lazy val incomeTooHigh = nettIncome >= 110
  lazy val incomeOK = !incomeTooHigh

  lazy val guardConditions = Carers.guardConditions(this)
}

@RunWith(classOf[CddJunitRunner])
object Expenses {
  implicit def stringStringToCarers(x: Tuple2[String, String]) = CarersXmlSituation(World(x._1, new TestNinoToCis), Xmls.validateClaim(x._2))

  val expenses = Engine.folding[CarersXmlSituation, Double, Double]((acc, v) => acc + v, 0).
    title("Expenses").
    code((c: CarersXmlSituation) => 0.0).
    childEngine("Child care expenses", """Customer's claiming CA may claim an allowable expense of up to 50% of their childcare expenses
        where the child care is not being undertaken by a direct relative. This amount may then be deducted from their gross pay.""").
    scenario(("2010-3-1", "CL100110A")).expected(15).
    because((c: CarersXmlSituation) => c.hasChildExpenses()).
    code((c: CarersXmlSituation) => c.childExpensesAcount() / 2).
    scenario(("2010-3-1", "CL100104A")).expected(0).

    childEngine("PSN  Pensions", """Customers claiming CA may claim an allowable expense of up to 50% of their Private Pension contributions. 
        This amount may then be deducted from their gross pay figure.""").
    scenario(("2010-3-1", "CL100111A")).expected(15).
    because((c: CarersXmlSituation) => c.hasPsnPension()).
    code((c: CarersXmlSituation) => c.psnPensionAcount() / 2).
    scenario(("2010-3-1", "CL100104A")).expected(0).

    childEngine("Occupational Pension",
      """Customers claiming CA may claim an allowable expense of up to 50% of their Occupational Pension contributions. 
        This amount may then be deducted from their gross pay figure.""").
      scenario(("2010-3-1", "CL100112A")).expected(15).
      because((c: CarersXmlSituation) => c.hasOccPension()).
      code((c: CarersXmlSituation) => c.occPensionAcount() / 2).
      scenario(("2010-3-1", "CL100104A")).expected(0).

      build
}

@RunWith(classOf[CddJunitRunner])
object Income {
  implicit def stringStringToCarers(x: Tuple2[String, String]) = CarersXmlSituation(World(x._1, new TestNinoToCis), Xmls.validateClaim(x._2))

  val income = Engine[CarersXmlSituation, Double]().title("Income").
    useCase("No income", "A person without any income should return 0 as their income").
    scenario(("2010-3-1", "CL100104A")).expected(0).
    because((c: CarersXmlSituation) => !c.hasEmploymentData()).

    useCase("Annually paid", "A person who is annually paid has their annual salary divided by 52 to calculate their income").
    scenario(("2010-3-1", "CL100113A")).expected(7000.0 / 52).
    because((c: CarersXmlSituation) => c.employmentPayPeriodicity() == "Annually").
    code((c: CarersXmlSituation) => c.employmentGrossSalary() / 52).

    useCase("Weekly paid").
    scenario(("2010-3-1", "CL100110A")).expected(110).
    because((c: CarersXmlSituation) => c.employmentPayPeriodicity() == "Weekly").
    code((c: CarersXmlSituation) => c.employmentGrossSalary()).
    build
}

@RunWith(classOf[CddJunitRunner])
object Carers {
  val carersPayment: Double = 110

  implicit def stringStringToCarers(x: Tuple2[String, String]) = CarersXmlSituation(World(x._1, new TestNinoToCis), Xmls.validateClaim(x._2))
  implicit def toKeyAndParams(x: String) = Some(KeyAndParams(x))
  val guardConditions = Engine.folding[CarersXmlSituation, Option[KeyAndParams], List[KeyAndParams]]((acc, opt) => acc ::: opt.toList, List()).title("Check Guard Condition").
    code((c: CarersXmlSituation) => None).

    childEngine("Age Restriction", "Customers under age 16 are not entitled to Carers Allowance").
    scenario(("2010-3-1", "CL100104A"), "Cl100104A-Age Under 16").expected("carer.claimant.under16.510").
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

    childEngine("High Salary", "Customers who earn more than the threshold value per week are not entitled to CA").
    scenario(("2010-2-10", "CL100111A")).expected(None).
    assertion((c: CarersXmlSituation, optReason: ROrException[Option[KeyAndParams]]) => c.nettIncome == 95).

    scenario(("2010-2-10", "CL100112A")).expected(None).
    assertion((c: CarersXmlSituation, optReason: ROrException[Option[KeyAndParams]]) => c.nettIncome == 95).

    scenario(("2010-2-10", "CL100113A")).expected("carers.income.tooHigh").
    because((c: CarersXmlSituation) => c.incomeTooHigh).
    build

  type ReasonsOrAmount = Either[Double, List[KeyAndParams]]
  implicit def toAmoumt(x: Double) = Left(x)
  implicit def toReasons(x: List[KeyAndParams]) = Right(x)
  implicit def stringsToReasons(x: List[String]) = Right(x.map(KeyAndParams(_)))

  val engine = Engine[CarersXmlSituation, ReasonsOrAmount]().title("Validate Claim").
    code((c: CarersXmlSituation) => Left(carersPayment)).
    useCase("Guard Conditions", "All guard conditions should be passed").
    scenario(("2010-6-7", "CL100108A"), "CL100108A-restriction on immigration status").
    expected(List("carers.claimant.notResident", "carers.claimant.restriction.immigrationStatus")).
    because((c: CarersXmlSituation) => c.guardConditions.size > 0).
    code((c: CarersXmlSituation) => Right(c.guardConditions)).

    useCase("Employment 4", """Customer's claiming CA may claim an allowable expense of up to 50% of their childcare expenses where the child care is not being undertaken by a direct relative. 
        This amount may then be deducted from their gross pay.""").
    scenario(("2010-3-22", "CL100110A"), "CL100110A-child care allowance").
    expected(carersPayment).

    useCase("Employment 5", """Customers claiming CA may claim an allowable expense of up to 50% of their Private Pension contributions. 
        This amount may then be deducted from their gross pay figure.""").
    scenario(("2010-3-8", "CL100111A"), "CL100111A-private pension").
    expected(carersPayment).

    useCase("Employment 6", """Customers claiming CA may claim an allowable expense of up to 50% of their Occupational Pension contributions. 
        This amount may then be deducted from their gross pay figure.""").
    scenario(("2010-3-8", "CL100112A"), "CL100112A-occupational pension").
    expected(carersPayment).

    build

  def main(args: Array[String]) {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
    val c: CarersXmlSituation = ("2010-3-1", "CL100104A")
    println(engine(c))
  }
}