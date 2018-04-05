package com.github.pshirshov.izumi.r2.idealingua.experiments.impls

import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime._
import com.github.pshirshov.izumi.r2.idealingua.experiments.generated._


import scala.language.{higherKinds, implicitConversions}

trait AbstractGreeterServer[R[_]] extends GreeterService[R] with WithResult[R] {
  override def greet(name: String, surname: String): Result[String] = _Result {
    s"Hi, $name $surname!"
  }
}

object AbstractGreeterServer {

  class Impl[R[_] : ServiceResult] extends AbstractGreeterServer[R] {
    override protected def _ServiceResult: ServiceResult[R] = implicitly
  }

}

trait AbstractCalculatorServer[R[_]] extends CalculatorService[R] with WithResult[R] {

  override def sum(a: Int, b: Int): Result[Int] = _Result {
    a + b
  }
}

object AbstractCalculatorServer {

  class Impl[R[_] : ServiceResult] extends AbstractCalculatorServer[R] {
    override protected def _ServiceResult: ServiceResult[R] = implicitly
  }

}
