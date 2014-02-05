package org.cddcore.carers

import org.junit.runner.notification.RunNotifier
import org.junit.runner._
import junit.framework._

class MyRunner(clazz: Class[_]) extends Runner {
  def getDescription() = Description.createSuiteDescription("MySuiteName")
  def run(notifier: RunNotifier): Unit = println("\n\n==============================Executing=================================\n\n")
}

@RunWith(classOf[MyRunner])
object DemoTest extends TestCase {
  final val d = DemoTest.getClass()

  def testX() {}
}  