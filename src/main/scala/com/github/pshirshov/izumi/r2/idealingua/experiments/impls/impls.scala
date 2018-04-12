package com.github.pshirshov.izumi.r2.idealingua.experiments.impls

import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime._
import com.github.pshirshov.izumi.r2.idealingua.experiments.generated._


import scala.language.{higherKinds, implicitConversions}

trait AbstractGreeterServer[R[_], C] extends GreeterService[R, C] with WithResult[R] {
  override def greet(ctx: C, name: String, surname: String): Result[String] = _Result {
    s"Hi, $name $surname!"
  }
}

object AbstractGreeterServer {

  class Impl[R[_] : ServiceResult, C] extends AbstractGreeterServer[R, C] {
    override protected def _ServiceResult: ServiceResult[R] = implicitly
  }

}

trait AbstractCalculatorServer[R[_], C] extends CalculatorService[R, C] with WithResult[R] {

  override def sum(ctx: C, a: Int, b: Int): Result[Int] = _Result {
    a + b
  }
}

object AbstractCalculatorServer {

  class Impl[R[_] : ServiceResult, C] extends AbstractCalculatorServer[R, C] {
    override protected def _ServiceResult: ServiceResult[R] = implicitly
  }

}
