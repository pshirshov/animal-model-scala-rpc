package com.github.pshirshov.izumi.r2.idealingua.experiments.generated

import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime._
import io.circe._
import io.circe.generic.semiauto._

import scala.language.{higherKinds, implicitConversions}

trait CalculatorService[R[_]] extends WithResultType[R] {
  def sum(a: Int, b: Int): Result[Int]
}


trait CalculatorServiceWrapped[R[_]] extends WithResultType[R] {

  import CalculatorServiceWrapped._

  def sum(input: SumInput): Result[SumOutput]
}

object CalculatorServiceWrapped {

  sealed trait CalculatorServiceInput

  case class SumInput(a: Int, b: Int) extends CalculatorServiceInput

  sealed trait CalculatorServiceOutput

  case class SumOutput(value: Int) extends CalculatorServiceOutput

  object SumInput {
    implicit val encodeTestPayload: Encoder[SumInput] = deriveEncoder
    implicit val decodeTestPayload: Decoder[SumInput] = deriveDecoder
  }

  object SumOutput {
    implicit val encodeTestPayload: Encoder[SumOutput] = deriveEncoder
    implicit val decodeTestPayload: Decoder[SumOutput] = deriveDecoder
  }

  object CalculatorServiceInput {
    implicit val encodeTestPayload: Encoder[CalculatorServiceInput] = deriveEncoder
    implicit val decodeTestPayload: Decoder[CalculatorServiceInput] = deriveDecoder
  }

  object CalculatorServiceOutput {
    implicit val encodeTestPayload: Encoder[CalculatorServiceInput] = deriveEncoder
    implicit val decodeTestPayload: Decoder[CalculatorServiceInput] = deriveDecoder
  }
  
//  trait CalculatorServiceDispatcherPacking[R[_]] extends CalculatorService[R] with WithResult[R] {
//    def dispatcher: Dispatcher[CalculatorServiceInput, CalculatorServiceOutput, R]
//
//    def sum(a: Int, b: Int): Result[Int] = {
//      val packed = SumInput(a, b)
//      val dispatched = dispatcher.dispatch(packed)
//      _ServiceResult.map(dispatched) {
//        case o: SumOutput =>
//          o.value
//      }
//    }
//  }
//
//  object CalculatorServiceDispatcherPacking {
//
//    class Impl[R[_] : ServiceResult](val dispatcher: Dispatcher[CalculatorServiceInput, CalculatorServiceOutput, R]) extends CalculatorServiceDispatcherPacking[R] {
//      override protected def _ServiceResult: ServiceResult[R] = implicitly
//    }
//
//  }
//
//
//  trait CalculatorServiceDispatcherUnpacking[R[_]]
//    extends CalculatorServiceWrapped[R]
//      with Dispatcher[CalculatorServiceInput, CalculatorServiceOutput, R]
//      with UnsafeDispatcher[CalculatorServiceInput, CalculatorServiceOutput, R]
//      with WithResult[R] {
//    def service: CalculatorService[R]
//
//
//    def sum(input: SumInput): Result[SumOutput] = {
//      val result = service.sum(input.a, input.b)
//      _ServiceResult.map(result)(SumOutput.apply)
//    }
//
//    def dispatch(input: CalculatorServiceInput): Result[CalculatorServiceOutput] = {
//      input match {
//        case v: SumInput =>
//          _ServiceResult.map(sum(v))(v => v) // upcast
//      }
//    }
//
//    def dispatchUnsafe(input: AnyRef): Option[Result[AnyRef]] = {
//      input match {
//        case v: CalculatorServiceInput =>
//          Option(_ServiceResult.map(dispatch(v))(v => v))
//
//        case _ =>
//          None
//      }
//    }
//
//  }
//
//  object CalculatorServiceDispatcherUnpacking {
//
//    class Impl[R[_] : ServiceResult](val service: CalculatorService[R]) extends CalculatorServiceDispatcherUnpacking[R] {
//      override protected def _ServiceResult: ServiceResult[R] = implicitly
//    }
//
//  }

}
