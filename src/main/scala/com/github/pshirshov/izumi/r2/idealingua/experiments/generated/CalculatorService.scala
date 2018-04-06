package com.github.pshirshov.izumi.r2.idealingua.experiments.generated

import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.OpinionatedMuxedCodec.DirectedPacket
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
    implicit val encodeTestPayload: Encoder[CalculatorServiceOutput] = deriveEncoder
    implicit val decodeTestPayload: Decoder[CalculatorServiceOutput] = deriveDecoder
  }

  trait CalculatorServiceDispatcherPacking[R[_]]
    extends CalculatorService[R]
      with WithResult[R] {
    def dispatcher: Dispatcher[CalculatorServiceInput, CalculatorServiceOutput, R]

    def sum(a: Int, b: Int): Result[Int] = {
      val packed = SumInput(a, b)
      val dispatched = dispatcher.dispatch(packed)
      _ServiceResult.map(dispatched) {
        case o: SumOutput =>
          o.value
        case o =>
          throw new TypeMismatchException(s"Unexpected input in CalculatorServiceDispatcherPacking.greet: $o", o)
      }
    }
  }

  class CalculatorServiceSafeToUnsafeBridge[R[_] : ServiceResult](dispatcher: Dispatcher[Muxed, Muxed, R]) extends Dispatcher[CalculatorServiceInput, CalculatorServiceOutput, R] with WithResult[R] {
    override protected def _ServiceResult: ServiceResult[R] = implicitly

    import ServiceResult._

    override def dispatch(input: CalculatorServiceInput): Result[CalculatorServiceOutput] = {
      dispatcher.dispatch(Muxed(input, serviceId) ).map {
        case Muxed(t: CalculatorServiceOutput, _) =>
          t
        case o =>
          throw new TypeMismatchException(s"Unexpected output in CalculatorServiceSafeToUnsafeBridge.dispatch: $o", o)
      }
    }
  }

  object CalculatorServiceDispatcherPacking {

    class Impl[R[_] : ServiceResult](val dispatcher: Dispatcher[CalculatorServiceInput, CalculatorServiceOutput, R]) extends CalculatorServiceDispatcherPacking[R] {
      override protected def _ServiceResult: ServiceResult[R] = implicitly
    }

  }


  val serviceId =  ServiceId("CalculatorService")

  object CodecProvider extends MuxingCodecProvider {
    import io.circe._
    import io.circe.syntax._

    val encoders: List[PartialFunction[AnyRef, Json]] = List(
      {
        case Muxed(v: CalculatorServiceWrapped.CalculatorServiceInput, _) =>
          Map("Input" -> Map(CalculatorServiceWrapped.serviceId.value -> v.asJson)).asJson
        case Muxed(v: CalculatorServiceWrapped.CalculatorServiceOutput, _) =>
          Map("Output" -> Map(CalculatorServiceWrapped.serviceId.value -> v.asJson)).asJson
      }
    )


    val decoders: List[PartialFunction[DirectedPacket, Decoder.Result[Muxed]]] = List(
      {
        case DirectedPacket("Input", CalculatorServiceWrapped.serviceId, packet) =>
          packet.as[CalculatorServiceInput].map(v => Muxed(v, CalculatorServiceWrapped.serviceId))

        case DirectedPacket("Output", CalculatorServiceWrapped.serviceId, packet) =>
          packet.as[CalculatorServiceOutput].map(v => Muxed(v, CalculatorServiceWrapped.serviceId))
      }
    )
  }

  trait CalculatorServiceDispatcherUnpacking[R[_]]
    extends CalculatorServiceWrapped[R]
      with Dispatcher[CalculatorServiceInput, CalculatorServiceOutput, R]
      with UnsafeDispatcher[CalculatorServiceInput, CalculatorServiceOutput, R]
      with WithResult[R] {
    def service: CalculatorService[R]

    def sum(input: SumInput): Result[SumOutput] = {
      val result = service.sum(input.a, input.b)
      _ServiceResult.map(result)(SumOutput.apply)
    }

    def dispatch(input: CalculatorServiceInput): Result[CalculatorServiceOutput] = {
      input match {
        case v: SumInput =>
          _ServiceResult.map(sum(v))(v => v) // upcast
      }
    }

    override def identifier: ServiceId = serviceId

    override def dispatchUnsafe(input: Muxed): Option[Result[Muxed]] = {
      input.v match {
        case v: CalculatorServiceInput =>
          Option(_ServiceResult.map(dispatch(v))(v => Muxed(v, identifier)))

        case _ =>
          None
      }
    }
  }

  object CalculatorServiceDispatcherUnpacking {

    class Impl[R[_] : ServiceResult](val service: CalculatorService[R]) extends CalculatorServiceDispatcherUnpacking[R] {
      override protected def _ServiceResult: ServiceResult[R] = implicitly
    }

  }

}
