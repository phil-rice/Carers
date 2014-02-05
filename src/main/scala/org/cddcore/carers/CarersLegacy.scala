package org.cddcore.carers
import scala.language.implicitConversions
import org.cddcore.engine._
import org.legacycdd.legacy.LegacyData
import org.legacycdd.legacy.MemoryReporterToHtml
import org.legacycdd.legacy.Legacy
import org.legacycdd.legacy.MemoryReporter
import scala.io.Source
import scala.xml.XML
import scala.xml.Elem
import scala.Array.canBuildFrom

object LegacyParser {
  def toParams = (
    s: String) => {
    val parts = s.split(",");
    CarersXmlSituation(World(parts(0)), Xmls.validateClaim(parts(1)))
  }
  def toExpected = (s: String) => { val parts = s.split(","); parts.size match { case 3 => ReasonAndAmount(parts(2).trim); case 4 => ReasonAndAmount(parts(2), Some(parts(3).trim.toDouble)) } }
}

object CarersLegacy {
  implicit def toROrException(x: Tuple2[Double, String]) = ROrException[ReasonAndAmount](ReasonAndAmount(x._1, x._2))
  implicit def toROrException(x: String) = ROrException(ReasonAndAmount(x))
  implicit def toReasonAndAmount(x: String) = ReasonAndAmount(x)
  implicit def worldElemToCarers(x: Tuple2[World, Elem]) = CarersXmlSituation(x._1, x._2)
  implicit def worldStringToCarers(x: Tuple2[World, String]) = CarersXmlSituation(x._1, Xmls.validateClaim(x._2))
  implicit def stringStringToCarers(x: Tuple2[String, String]) = CarersXmlSituation(World(Xmls.asDate(x._1)), Xmls.validateClaim(x._2))

  val categoriserEngine = Engine[LegacyData[Int, ReasonAndAmount], String]().title("Categorise Results").
    code((l: LegacyData[Int, ReasonAndAmount]) => "Fail").
    useCase("Pass").expected("Pass").
    scenario(LegacyData(1, List(), None, "X", "X")).because((l: LegacyData[Int, ReasonAndAmount]) =>
      l.pass).
    code((l: LegacyData[Int, ReasonAndAmount]) => "Pass").
    build

  val legacyData = Source.fromFile("CarersLegacyData.dat").mkString
  val lines = legacyData.split("\n").zipWithIndex
  val idToParams = lines.foldLeft(Map[Int, List[Any]]())((map, tuple) => tuple match {
    case (line, index) => map + (index.toInt -> List(LegacyParser.toParams(line)))
  })
  val idToExpected = lines.foldLeft(Map[Int, ROrException[ReasonAndAmount]]())((map, tuple) => tuple match {
    case (line, index) =>
      map + (index.toInt -> ROrException(LegacyParser.toExpected(line)))
  })
  val reporter = new MemoryReporter[Int, ReasonAndAmount]()
  val allIds = 0 to lines.size - 1
  val descFn = (i: Int) => Some(lines(i)._1)

  def main(args: Array[String]) {

    val engine = Engine[CarersXmlSituation, ReasonAndAmount]().param((s: String) => CarersXmlSituation(World("2010-6-9"), XML.loadString(s)), "Validate Claim XML").
      code((c: CarersXmlSituation) => ReasonAndAmount("carer.default.notPaid")).
      useCase("Customers under age 16 are not entitled to CA").
      //      scenario((World("2010-6-9"), "CL100104A")).expected("carer.claimant.under16").
      scenario((World("2010-6-9"), "CL100104A")).expected(ReasonAndAmount("carer.claimant.under16")).
      because((c: CarersXmlSituation) => c.underSixteen).
      //      scenario((World("2010-6-9"), "CL100104A"), "Cl100104A-Age Under 16").
      //      expected(ReasonAndAmount("carer.claimant.under16")).
      //
      useCase("Hours1 - Customers with Hours of caring must be 35 hours or more in any one week").
      scenario((World("2010-1-1"), "CL100105A"), "CL100105A-lessThen35Hours").
      expected(ReasonAndAmount("carer.claimant.under35hoursCaring")).
      because((c: CarersXmlSituation) => !c.Claim35Hours()).
      //
      useCase("Qualifying Benefit 3 - DP's without the required level of qualyfing benefit will result in the disallowance of the claim to CA.").
      scenario((World("2010-6-23"), "CL100106A"), "CL100106A-?????? ").
      expected(ReasonAndAmount("carer.qualifyingBenefit.dpWithoutRequiredLevelOfQualifyingBenefit")).
      because((c: CarersXmlSituation) => c.DependantAwardComponent() != "DLA Middle Rate Care").
      //
      //      useCase("Residence 3- Customer who is not considered resident and present in GB is not entitled to CA.").
      //      scenario((World("2010-6-7"), "CL100107A"), "CL100107A-notInGB").
      //      expected(ReasonAndAmount("carers.claimant.notResident")).
      //      because((c: CarersXmlSituation) => !c.ClaimAlwaysUK()).
      ////
      //      useCase("Presence 2- Customers who have restrictions on their immigration status will be disallowed CA.").
      //      scenario((World("2010-6-7"), "CL100108A"), "CL100108A-restriction on immigration status").
      //      expected(ReasonAndAmount("carers.claimant.restriction.immigrationStatus")).
      //      because((c: CarersXmlSituation) => !c.ClaimCurrentResidentUK()).
      //      //
      //      useCase("Full Time Eduction 2  -Customer in FTE 21 hours or more each week are not entitled to CA.").
      //      scenario((World("2010-2-10"), "CL100109A"), "CL100109A-full time education").
      //      expected(ReasonAndAmount("carers.claimant.fullTimeEduction.moreThan21Hours")).
      //      because((c: CarersXmlSituation) => c.ClaimEducationFullTime()).
      //
      //      useCase("Employment 4  - Customer's claiming CA may claim an allowable expense of up to 50% of their childcare expenses where the child care is not being undertaken by a direct relative. This amount may then be deducted from their gross pay.").
      //      scenario((World("2010-3-22"), "CL100110A"), "CL100110A-child care allowance").
      //      expected(ReasonAndAmount("carers.validClaim", Some(95.0))).
      //      code((c: CarersXmlSituation) => ReasonAndAmount("carers.validClaim", c.nettIncome)).
      //      because((c: CarersXmlSituation) => c.incomeOk).
      //
      //      useCase("Employment 5 - Customers claiming CA may claim an allowable expense of up to 50% of their Private Pension contributions. This amount may then be deducted from their gross pay figure.").
      //      scenario((World("2010-3-8"), "CL100111A"), "CL100111A-private pension").
      //      expected(ReasonAndAmount("carers.validClaim", Some(95.0))).
      //
      //      useCase("Employment 6 - Customers claiming CA may claim an allowable expense of up to 50% of their Occupational Pension contributions. This amount may then be deducted from their gross pay figure.").
      //      scenario((World("2010-3-8"), "CL100112A"), "CL100112A-occupational pension").
      //      expected(ReasonAndAmount("carers.validClaim", Some(95.0))).
      //
      //      useCase("Employment 7 - Customer in paid employment exceeding GBP100 (after allowable expenses) per week is not entitled to CA.").
      //      scenario((World("2010-6-1"), "CL100113A"), "CL100113A-paid employment earning too much").
      //      expected(ReasonAndAmount("carers.nettIncome.moreThan100PerWeek")).
      //      because((c: CarersXmlSituation) => !c.incomeOk).
      //
      //      useCase("Self employment 2 - Customer in Self employed work earning more than the prescribed limit of GBP100 per week (after allowable expenses) are not entitled to CA.").
      //      scenario((World("2010-3-1"), "CL100114A"), "CL114A-self employed earning too much").
      //      expected(ReasonAndAmount("carers.nettIncome.moreThan100PerWeek")).
      //
      //      useCase("Sublet 2- Customers receiving payment for subletting their property for board and lodgings receiving more than the prescribed limit of GBP100 (after allowable expenses) will be disallowed for CA.").
      //      scenario((World("2010-3-1"), "CL100115A"), "CL115A-sub let").
      //      expected(ReasonAndAmount("carers.income.rental")).
      //      because((c: CarersXmlSituation) => c.ClaimRentalIncome()).
      //
      //      useCase("Prop 2- Customer receiving an Income from the renting of another property or land in the UK or abroad either their own name or a share in a partners profit is above GBP100 per week(after allowable expenses) is not entitled to CA.").
      //      scenario((World("2010-3-1"), "CL100116A"), "CL116A-income from renting").
      //      expected(ReasonAndAmount("carers.income.rental")).
      build

    new Legacy[Int, ReasonAndAmount](
      allIds,
      idToParams,
      idToExpected,
      engine,
      categoriserEngine,
      reporter,
      descFn)

    new MemoryReporterToHtml[Int, ReasonAndAmount, List[ReasonAndAmount]](categoriserEngine, engine, reporter).createReport()
    println("Done")
  }
}