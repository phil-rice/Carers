package org.cddcore.carers

import scala.language.implicitConversions
import org.cddcore.engine._
import org.corecdd.website._

object CarersMain {
  def main(args: Array[String]) {
    WebServer( new CddHandler(Carers.engine)).launch 
  }
}