package com.github.pshirshov.izumi.r2.idealingua.experiments.generated

import com.github.pshirshov.izumi.r2.idealingua
import com.github.pshirshov.izumi.r2.idealingua.experiments.generated
import com.github.pshirshov.izumi.r2.idealingua.experiments.generated.GreeterServiceWrapped.{GreetInput, GreetOutput, GreeterServiceInput, GreeterServiceOutput}
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime._
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.circe.OpinionatedMuxedCodec.DirectedPacket
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.circe.{Body, CirceWrappedServiceDefinition, MuxingCodecProvider}
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

object CalculatorServiceWrapped
  extends IdentifiableServiceDefinition
    with WrappedServiceDefinition
    with WrappedUnsafeServiceDefinition
    with CirceWrappedServiceDefinition {

  sealed trait CalculatorServiceInput

  case class SumInput(a: Int, b: Int) extends CalculatorServiceInput

  sealed trait CalculatorServiceOutput

  case class SumOutput(value: Int) extends CalculatorServiceOutput

  override type Input = CalculatorServiceInput
  override type Output = CalculatorServiceOutput


  override type Service[R[_]] = CalculatorService[R]

  override def client[R[_] : ServiceResult](dispatcher: Dispatcher[CalculatorServiceInput, CalculatorServiceOutput, R]): CalculatorService[R] = {
    new generated.CalculatorServiceWrapped.PackingDispatcher.Impl[R](dispatcher)
  }


  override def clientUnsafe[R[_] : ServiceResult](dispatcher: Dispatcher[MuxRequest[_], MuxResponse[_], R]): CalculatorService[R] = {
    client(new SafeToUnsafeBridge[R](dispatcher))
  }

  override def server[R[_] : ServiceResult](service: CalculatorService[R]): Dispatcher[CalculatorServiceInput, CalculatorServiceOutput, R] = {
    new idealingua.experiments.generated.CalculatorServiceWrapped.UnpackingDispatcher.Impl[R](service)
  }


  override def serverUnsafe[R[_] : ServiceResult](service: CalculatorService[R]): UnsafeDispatcher[CalculatorServiceInput, CalculatorServiceOutput, R] = {
    new idealingua.experiments.generated.CalculatorServiceWrapped.UnpackingDispatcher.Impl[R](service)
  }


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

  val serviceId = ServiceId("CalculatorService")

  trait PackingDispatcher[R[_]]
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

  class SafeToUnsafeBridge[R[_] : ServiceResult](dispatcher: Dispatcher[MuxRequest[_], MuxResponse[_], R]) extends Dispatcher[CalculatorServiceInput, CalculatorServiceOutput, R] with WithResult[R] {
    override protected def _ServiceResult: ServiceResult[R] = implicitly

    import ServiceResult._

    override def dispatch(input: CalculatorServiceInput): Result[CalculatorServiceOutput] = {
      dispatcher.dispatch(MuxRequest(input, toMethodId(input))).map {
        case MuxResponse(t: CalculatorServiceOutput, _) =>
          t
        case o =>
          throw new TypeMismatchException(s"Unexpected output in CalculatorServiceSafeToUnsafeBridge.dispatch: $o", o)
      }
    }
  }

  object PackingDispatcher {

    class Impl[R[_] : ServiceResult](val dispatcher: Dispatcher[CalculatorServiceInput, CalculatorServiceOutput, R]) extends PackingDispatcher[R] {
      override protected def _ServiceResult: ServiceResult[R] = implicitly
    }

  }

  trait UnpackingDispatcher[R[_]]
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

    override def dispatchUnsafe(input: MuxRequest[_]): Option[Result[MuxResponse[_]]] = {
      input.v match {
        case v: CalculatorServiceInput =>
          Option(_ServiceResult.map(dispatch(v))(v => MuxResponse(v, toMethodId(v))))

        case _ =>
          None
      }
    }
  }

  object UnpackingDispatcher {

    class Impl[R[_] : ServiceResult](val service: CalculatorService[R]) extends UnpackingDispatcher[R] {
      override protected def _ServiceResult: ServiceResult[R] = implicitly
    }

  }

  def toMethodId(v: CalculatorServiceInput): Method = {
    v match {
      case _: SumInput => Method(serviceId, MethodId("sum"))
    }
  }

  def toMethodId(v: CalculatorServiceOutput): Method = {
    v match {
      case _: SumOutput => Method(serviceId, MethodId("sum"))
    }
  }


  override def codecProvider: MuxingCodecProvider = CodecProvider

  object CodecProvider extends MuxingCodecProvider {

    import io.circe._
    import io.circe.syntax._

    val encoders: List[PartialFunction[Body, Json]] = List(
      {
        case Body(v: CalculatorServiceWrapped.CalculatorServiceInput) =>
          v.asJson
        case Body(v: CalculatorServiceWrapped.CalculatorServiceOutput) =>
          v.asJson
      }
    )


    val decoders: List[PartialFunction[DirectedPacket, Decoder.Result[Body]]] = List(
      {
        case DirectedPacket("Input", CalculatorServiceWrapped.serviceId, packet) =>
          packet.as[CalculatorServiceInput].map(v => Body(v))

        case DirectedPacket("Output", CalculatorServiceWrapped.serviceId, packet) =>
          packet.as[CalculatorServiceOutput].map(v => Body(v))
      }
    )
  }

}
