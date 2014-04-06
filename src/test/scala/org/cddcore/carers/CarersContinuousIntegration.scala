package org.cddcore.carers

import org.cddcore.engine.tests._
import org.junit.runner.RunWith
import com.cddcore.carersblog.reasoningOnDates._
import org.cddcore.engine.Engine

/**
 * This class will be swept up by JUnit. It should access all the engines that you want to check
 *  It would be nice if it could be just done by reflection but there are issues with it: objects don't get checked by JUnit, Some engines are created in places with funny constructors...
 */
@RunWith(classOf[CddContinuousIntegrationRunner])
class CarersContinuousIntegration extends CddContinuousIntegrationTest {
  def engines = Engine.test(List(
    Carers.engine,
    Carers.guardConditions,
    Carers.interestingDates,
    Carers.singleBreakInCare,
    Carers.breaksInCare,
    Income.income,
    Expenses.expenses,
    DateRanges.firstDayOfWeek,
    DateRanges.datesToRanges,
    DateRanges.splitIntoStartMiddleEnd,
    DateRanges.interestingDatesToDateRangesToBeProcessedTogether,
    DateRanges.groupByWeek))
}