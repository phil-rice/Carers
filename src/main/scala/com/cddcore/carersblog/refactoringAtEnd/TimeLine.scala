package com.cddcore.carersblog.refactoringAtEnd

import scala.language.implicitConversions
import org.cddcore.engine.Engine
import org.joda.time.DateTime
import org.joda.time.Weeks
import org.junit.runner.RunWith
import org.cddcore.engine.tests.CddJunitRunner

case class TimeLineItem(events: List[(DateRange, TimeLineCalcs.ReasonsOrAmount)]) {
  val startDate = events.head._1.from
  val endDate = events.last._1.to
  lazy val okDays = {
    val x = TimeLineCalcs.daysInWhichIWasOk
    x(this)
  }
  lazy val wasOk = okDays > 2
  override def toString = s"TimeLineItem($startDate, $endDate,  dateRange=\n  ${events.mkString("\n  ")})"
}

@RunWith(classOf[CddJunitRunner])
object TimeLineCalcs {
  type ReasonsOrAmount = Either[Double, List[KeyAndParams]]

  type TimeLine = List[TimeLineItem]
  implicit def stringToDate(x: String) = Xmls.asDate(x)

  implicit def toTliInt(x: List[(String, String, Any)]): TimeLineItem =
    TimeLineItem(x.collect {
      case (from, to, x) => x match {
        case num: Int => (DateRange(from, to, "for test"), Left(num.toDouble))
        case reason: String => (DateRange(from, to, "for test"), Right(List(KeyAndParams(reason))))
      }
    })

  val daysInWhichIWasOk = Engine[TimeLineItem, Int]().title("Time line item: days in which I was OK").
    scenario(List(("2010-3-1", "2010-3-3", 110))).expected(3).
    code((tli: TimeLineItem) => tli.events.foldLeft[Int](0)((acc, tuple) => tuple match {
      case (dr, Left(d: Double)) => dr.days
      case (dr, Right(_)) => 0
    })).
    scenario(List(("2010-3-1", "2010-3-3", 110))).expected(3).
    scenario(List(("2010-3-1", "2010-3-3", "failed"))).expected(0).
    scenario(List(("2010-3-1", "2010-3-3", "failed"), ("2010-3-4", "2010-3-5", 0))).expected(2).
    build

  /** Returns a DatesToBeProcessedTogether and the days that the claim is valid for */
  def findTimeLine(c: CarersXmlSituation): TimeLine = {
    val dates = Carers.interestingDates(c)
    val dayToSplit = DateRanges.sunday
    val result = DateRanges.interestingDatesToDateRangesToBeProcessedTogether(dates, dayToSplit)

    result.map((dateRangeToBeProcessedTogether: DateRangesToBeProcessedTogether) => {
      TimeLineItem(dateRangeToBeProcessedTogether.dateRanges.map((dr) => {
        val result = Carers.engine(dr.from, c)
        (dr, result)
      }))
    })
  }

  case class SimplifiedTimeLineItem(startDate: DateTime, amount: Int, reasons: List[KeyAndParams])

  implicit def toSimplifiedTlItem(x: (String, Int)) =
    SimplifiedTimeLineItem(x._1, x._2, List())
  implicit def toSimplifiedTlItemWithReason(x: (String, String)) =
    SimplifiedTimeLineItem(x._1, 0, List(KeyAndParams(x._2)))

  val simplifyTimeLineEngine = Engine[TimeLine, List[SimplifiedTimeLineItem]]().
    useCase("single week").
    scenario(List(List(("2010-3-1", "2010-3-3", 110)))).expected(List(("2010-3-1", 110))).
    code((t: TimeLine) =>
      t.flatMap((tli) => {
        val weeks = Weeks.weeksBetween(tli.startDate, tli.endDate).getWeeks
        (0 to weeks - 1).map((week) => {
          val reasons = tli.events.foldLeft(List[KeyAndParams]())((acc, e) => {
            val newList: List[KeyAndParams] = e._2 match {
              case Left(num: Double) => List[KeyAndParams]()
              case Right(list: List[KeyAndParams]) => list
            }
            newList ++ acc
          })
          SimplifiedTimeLineItem(tli.startDate.plusDays(week * 7), tli.wasOk match { case false => 0; case true => 110 }, reasons)
        })
      })).

    scenario(List(List(("2010-3-1", "2010-3-3", 110), ("2010-3-4", "2010-3-6", 110)))).expected(List(("2010-3-1", 110))).
    build

  def simplifyTimeLine(t: TimeLine) = {
    t.flatMap((tli) => {
      val weeks = Weeks.weeksBetween(tli.startDate, tli.endDate).getWeeks
      (0 to weeks - 1).map((week) => {
        val reasons = tli.events.foldLeft(List[ReasonsOrAmount]())((acc, e) => e._2 :: acc)
        (tli.startDate.plusDays(week * 7), tli.wasOk match { case false => 0; case true => 110 }, reasons)
      })
    })
  }
}