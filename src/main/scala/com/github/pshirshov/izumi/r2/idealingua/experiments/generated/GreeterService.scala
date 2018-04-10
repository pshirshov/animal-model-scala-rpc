package com.github.pshirshov.izumi.r2.idealingua.experiments.generated

import com.github.pshirshov.izumi.r2.idealingua
import com.github.pshirshov.izumi.r2.idealingua.experiments.generated
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime._
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.circe._
import io.circe.Decoder.Result
import io.circe._
import io.circe.generic.semiauto._

import scala.language.{higherKinds, implicitConversions}

trait GreeterService[R[_]] extends WithResultType[R] {
  def greet(name: String, surname: String): Result[String]
}


trait GreeterServiceWrapped[R[_]] extends WithResultType[R] {

  import GreeterServiceWrapped._

  def greet(input: GreetInput): Result[GreetOutput]
}

object GreeterServiceWrapped
  extends IdentifiableServiceDefinition
    with WrappedServiceDefinition
    with WrappedUnsafeServiceDefinition
    with CirceWrappedServiceDefinition {

  sealed trait GreeterServiceInput

  case class GreetInput(name: String, surname: String) extends GreeterServiceInput

  sealed trait GreeterServiceOutput

  case class GreetOutput(value: String) extends GreeterServiceOutput


  override type Input = GreeterServiceInput
  override type Output = GreeterServiceOutput

  override type Service[R[_]] = GreeterService[R]


  override def client[R[_] : ServiceResult](dispatcher: Dispatcher[GreeterServiceInput, GreeterServiceOutput, R]): GreeterService[R] = {
    new generated.GreeterServiceWrapped.PackingDispatcher.Impl[R](dispatcher)
  }


  override def server[R[_] : ServiceResult](service: GreeterService[R]): Dispatcher[GreeterServiceInput, GreeterServiceOutput, R] = {
    new idealingua.experiments.generated.GreeterServiceWrapped.UnpackingDispatcher.Impl[R](service)
  }


  override def serverUnsafe[R[_] : ServiceResult](service: GreeterService[R]): UnsafeDispatcher[GreeterServiceInput, GreeterServiceOutput, R] = {
    new idealingua.experiments.generated.GreeterServiceWrapped.UnpackingDispatcher.Impl[R](service)
  }

  override def clientUnsafe[R[_] : ServiceResult](dispatcher: Dispatcher[MuxRequest[_], MuxResponse[_], R]): GreeterService[R] = {
    client(new SafeToUnsafeBridge[R](dispatcher))
  }

  object GreetInput {
    implicit val encode: Encoder[GreetInput] = deriveEncoder
    implicit val decode: Decoder[GreetInput] = deriveDecoder
  }

  object GreetOutput {
    implicit val encode: Encoder[GreetOutput] = deriveEncoder
    implicit val decode: Decoder[GreetOutput] = deriveDecoder
  }

  object GreeterServiceInput {
    implicit val encode: Encoder[GreeterServiceInput] = deriveEncoder
    implicit val decode: Decoder[GreeterServiceInput] = deriveDecoder
  }

  object GreeterServiceOutput {
    implicit val encode: Encoder[GreeterServiceOutput] = deriveEncoder
    implicit val decode: Decoder[GreeterServiceOutput] = deriveDecoder
  }


  val serviceId =  ServiceId("GreeterService")

  trait PackingDispatcher[R[_]]
    extends GreeterService[R]
      with WithResult[R] {
    def dispatcher: Dispatcher[GreeterServiceInput, GreeterServiceOutput, R]

    def greet(name: String, surname: String): Result[String] = {
      val packed = GreetInput(name, surname)
      val dispatched = dispatcher.dispatch(packed)
      _ServiceResult.map(dispatched) {
        case o: GreetOutput =>
          o.value
        case o =>
          throw new TypeMismatchException(s"Unexpected input in GreeterServiceDispatcherPacking.greet: $o", o)
      }
    }
  }

  class SafeToUnsafeBridge[R[_] : ServiceResult](dispatcher: Dispatcher[MuxRequest[_], MuxResponse[_], R]) extends Dispatcher[GreeterServiceInput, GreeterServiceOutput, R] with WithResult[R] {
    override protected def _ServiceResult: ServiceResult[R] = implicitly

    import ServiceResult._

    override def dispatch(input: GreeterServiceInput): Result[GreeterServiceOutput] = {
      val muxedRequest = MuxRequest(input, toMethodId(input))
      dispatcher.dispatch(muxedRequest).map {
        case MuxResponse(t: GreeterServiceOutput, _) =>
          t
        case o =>
          throw new TypeMismatchException(s"Unexpected output in GreeterServiceSafeToUnsafeBridge.dispatch: $o", o)
      }
    }
  }

  object PackingDispatcher {

    class Impl[R[_] : ServiceResult](val dispatcher: Dispatcher[GreeterServiceInput, GreeterServiceOutput, R]) extends PackingDispatcher[R] {
      override protected def _ServiceResult: ServiceResult[R] = implicitly
    }

  }

  trait UnpackingDispatcher[R[_]]
    extends GreeterServiceWrapped[R]
      with Dispatcher[GreeterServiceInput, GreeterServiceOutput, R]
      with UnsafeDispatcher[GreeterServiceInput, GreeterServiceOutput, R]
      with WithResult[R] {
    def service: GreeterService[R]

    def greet(input: GreetInput): Result[GreetOutput] = {
      val result = service.greet(input.name, input.surname)
      _ServiceResult.map(result)(GreetOutput.apply)
    }

    def dispatch(input: GreeterServiceInput): Result[GreeterServiceOutput] = {
      input match {
        case v: GreetInput =>
          _ServiceResult.map(greet(v))(v => v) // upcast
      }
    }

    override def identifier: ServiceId = serviceId

    override def dispatchUnsafe(input: MuxRequest[_]): Option[Result[MuxResponse[_]]] = {
      input.v match {
        case v: GreeterServiceInput =>
          Option(_ServiceResult.map(dispatch(v))(v => MuxResponse(v, toMethodId(v))))

        case _ =>
          None
      }
    }
  }

  def toMethodId(v: GreeterServiceInput): Method  = {
    v match {
      case _: GreetInput => Method(serviceId, MethodId("greet"))
    }
  }
  def toMethodId(v: GreeterServiceOutput): Method  = {
    v match {
      case _: GreetOutput => Method(serviceId, MethodId("greet"))
    }
  }


  object UnpackingDispatcher {

    class Impl[R[_] : ServiceResult](val service: GreeterService[R]) extends UnpackingDispatcher[R] {
      override protected def _ServiceResult: ServiceResult[R] = implicitly
    }

  }


  override def codecProvider: MuxingCodecProvider = CodecProvider

  object CodecProvider extends MuxingCodecProvider {

    import io.circe._
    import io.circe.syntax._

    override def requestEncoders: List[PartialFunction[ReqBody, Json]] = List(
      {
        case ReqBody(v: GreeterServiceInput) =>
          v.asJson
      }
    )

    override def responseEncoders: List[PartialFunction[ResBody, Json]] = List(
      {
        case ResBody(v: GreeterServiceOutput) =>
          v.asJson
      }
    )


    override def requestDecoders: List[PartialFunction[CursorForMethod, Result[ReqBody]]] = List(
      {
        case CursorForMethod(m, packet) if m.service == serviceId =>
          packet.as[GreeterServiceInput].map(v => ReqBody(v))
      }
    )

    override def responseDecoders: List[PartialFunction[CursorForMethod, Result[ResBody]]] =  List(
      {
        case CursorForMethod(m, packet) if m.service == serviceId =>
          packet.as[GreeterServiceOutput].map(v => ResBody(v))
      }
    )
  }
}
