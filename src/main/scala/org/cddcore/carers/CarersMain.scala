package org.cddcore.carers
import org.cddcore.engine._
import org.corecdd.website._
import org.junit.runner.RunWith 
object CarersMain {
  def main(args: Array[String]) {
    val project = Project("Carers",  Carers.engine,  Carers.checkGuardConditions, Income.income, Expenses.expenses)
    WebServer(8090, project).launch 
  }
}