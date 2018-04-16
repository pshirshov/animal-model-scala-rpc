package com.github.pshirshov.izumi.r2.idealingua.experiments.generated

import com.github.pshirshov.izumi.r2.idealingua
import com.github.pshirshov.izumi.r2.idealingua.experiments.generated
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime._
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.circe.{CirceWrappedServiceDefinition, CursorForMethod, MuxingCodecProvider}
import io.circe.Decoder.Result
import io.circe._
import io.circe.generic.semiauto._

import scala.language.{higherKinds, implicitConversions}

trait CalculatorServiceClient[R[_]] extends WithResultType[R] {
  def sum(a: Int, b: Int): Result[Int]
}


trait CalculatorService[R[_], C] extends WithResultType[R] {
  def sum(ctx: C, a: Int, b: Int): Result[Int]
}

//trait CalculatorServiceCtx[R[_], C] extends WithResultType[R] with WithContext[C] {
//  def sum(ctx: C, a: Int, b: Int): Result[Int]
//}


trait CalculatorServiceWrapped[R[_], C] extends WithResultType[R] {

  import CalculatorServiceWrapped._

  def sum(ctx: C, input: SumInput): Result[SumOutput]
}

object CalculatorServiceWrapped
  extends IdentifiableServiceDefinition
    with WrappedServiceDefinition
    with WrappedUnsafeServiceDefinition
    with CirceWrappedServiceDefinition {

  sealed trait CalculatorServiceInput extends AnyRef

  case class SumInput(a: Int, b: Int) extends CalculatorServiceInput

  sealed trait CalculatorServiceOutput extends Any

  case class SumOutput(value: Int) extends AnyVal with CalculatorServiceOutput

  override type Input = CalculatorServiceInput
  override type Output = CalculatorServiceOutput


  override type ServiceServer[R[_], C] = CalculatorService[R, C]
  override type ServiceClient[R[_]] = CalculatorServiceClient[R]

  override def client[R[_] : ServiceResult](dispatcher: Dispatcher[CalculatorServiceInput, CalculatorServiceOutput, R]): CalculatorServiceClient[R] = {
    new generated.CalculatorServiceWrapped.PackingDispatcher.Impl[R](dispatcher)
  }


  override def clientUnsafe[R[_] : ServiceResult](dispatcher: Dispatcher[MuxRequest[_], MuxResponse[_], R]): CalculatorServiceClient[R] = {
    client(new SafeToUnsafeBridge[R](dispatcher))
  }

  override def server[R[_] : ServiceResult, C](service: CalculatorService[R, C]): Dispatcher[InContext[CalculatorServiceInput, C], CalculatorServiceOutput, R] = {
    new idealingua.experiments.generated.CalculatorServiceWrapped.UnpackingDispatcher.Impl[R, C](service)
  }


  override def serverUnsafe[R[_] : ServiceResult, C](service: CalculatorService[R, C]): UnsafeDispatcher[C, R] = {
    new idealingua.experiments.generated.CalculatorServiceWrapped.UnpackingDispatcher.Impl[R, C](service)
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
    extends CalculatorServiceClient[R]
      with WithResult[R] {
    def dispatcher: Dispatcher[CalculatorServiceInput, CalculatorServiceOutput, R]

    def sum(a: Int, b: Int): Result[Int] = {
      val packed = SumInput(a, b)
      val dispatched = dispatcher.dispatch(packed)
      _ServiceResult.map(dispatched) {
        case o: SumOutput =>
          o.value
        case o =>
          throw new TypeMismatchException(s"Unexpected input in CalculatorServiceDispatcherPacking.sum: $o", o)
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

  trait UnpackingDispatcher[R[_], C]
    extends CalculatorServiceWrapped[R, C]
      with Dispatcher[InContext[CalculatorServiceInput, C], CalculatorServiceOutput, R]
      with UnsafeDispatcher[C, R]
      with WithResult[R] {
    def service: CalculatorService[R, C]

    def sum(ctx: C, input: SumInput): Result[SumOutput] = {
      val result = service.sum(ctx, input.a, input.b)
      _ServiceResult.map(result)(SumOutput.apply)
    }

    def dispatch(input: InContext[CalculatorServiceInput, C]): Result[CalculatorServiceOutput] = {
      input match {
        case InContext(v: SumInput, c) =>
          _ServiceResult.map(sum(c, v))(v => v) // upcast
      }
    }

    override def identifier: ServiceId = serviceId

    override def dispatchUnsafe(input: InContext[MuxRequest[_], C]): Option[Result[MuxResponse[_]]] = {
      input.value.v match {
        case v: CalculatorServiceInput =>
          Option(_ServiceResult.map(dispatch(InContext(v, input.context)))(v => MuxResponse(v, toMethodId(v))))

        case _ =>
          None
      }
    }
  }

  object UnpackingDispatcher {

    class Impl[R[_] : ServiceResult, C](val service: CalculatorService[R, C]) extends UnpackingDispatcher[R, C] {
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

    override def requestEncoders: List[PartialFunction[ReqBody, Json]] = List(
      {
        case ReqBody(v: CalculatorServiceInput) =>
          v.asJson
      }
    )

    override def responseEncoders: List[PartialFunction[ResBody, Json]] = List(
      {
        case ResBody(v: CalculatorServiceOutput) =>
          v.asJson
      }
    )

    override def requestDecoders: List[PartialFunction[CursorForMethod, Result[ReqBody]]] = List(
      {
        case CursorForMethod(m, packet) if m.service == serviceId =>
          packet.as[CalculatorServiceInput].map(v => ReqBody(v))
      }
    )

    override def responseDecoders: List[PartialFunction[CursorForMethod, Result[ResBody]]] = List(
      {
        case CursorForMethod(m, packet) if m.service == serviceId =>
          packet.as[CalculatorServiceOutput].map(v => ResBody(v))
      }
    )
  }

}
