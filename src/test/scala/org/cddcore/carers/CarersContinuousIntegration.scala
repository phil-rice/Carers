package org.cddcore.carers

import org.cddcore.engine.tests._
import org.junit.runner.RunWith
import com.cddcore.carersblog.startingDates.Carers
import com.cddcore.carersblog.startingDates.Expenses
import com.cddcore.carersblog.startingDates.Income
import com.cddcore.carersblog.startingDates.DateRanges

/**
 * This class will be swept up by JUnit. It should access all the engines that you want to check
 *  It would be nice if it could be just done by reflection but there are issues with it: objects don't get checked by JUnit, Some engines are created in places with funny constructors...
 */
@RunWith(classOf[CddContinuousIntegrationRunner])
class CarersContinuousIntegration extends CddContinuousIntegrationTest {
  def engines = List(
    Carers.engine,
    Carers.guardConditions,
    Carers.interestingDates,
    Income.income,
    Expenses.expenses,
    DateRanges.firstDayOfWeek,
    DateRanges.datesToRanges,
    DateRanges.splitIntoStartMiddleEnd,
    DateRanges.interestingDatesToDateRangesToBeProcessedTogether,
    DateRanges.groupByWeek)
}