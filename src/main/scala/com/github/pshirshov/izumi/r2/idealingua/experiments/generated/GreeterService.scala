package com.github.pshirshov.izumi.r2.idealingua.experiments.generated

import com.github.pshirshov.izumi.r2.idealingua
import com.github.pshirshov.izumi.r2.idealingua.experiments.generated
import com.github.pshirshov.izumi.r2.idealingua.experiments.generated.CalculatorServiceWrapped.{SafeToUnsafeBridge, client}
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.circe.OpinionatedMuxedCodec.DirectedPacket
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime._
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.circe.MuxingCodecProvider
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

object GreeterServiceWrapped extends IdentifiableServiceDefinition with WrappedServiceDefinition with CirceWrappedServiceDefinition {

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

  override def clientUnsafe[R[_] : ServiceResult](dispatcher: Dispatcher[Muxed, Muxed, R]): GreeterService[R] = {
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

  class SafeToUnsafeBridge[R[_] : ServiceResult](dispatcher: Dispatcher[Muxed, Muxed, R]) extends Dispatcher[GreeterServiceInput, GreeterServiceOutput, R] with WithResult[R] {
    override protected def _ServiceResult: ServiceResult[R] = implicitly

    import ServiceResult._

    override def dispatch(input: GreeterServiceInput): Result[GreeterServiceOutput] = {
      dispatcher.dispatch(Muxed(input, serviceId)).map {
        case Muxed(t: GreeterServiceOutput, _) =>
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

    override def dispatchUnsafe(input: Muxed): Option[Result[Muxed]] = {
      input.v match {
        case v: GreeterServiceInput =>
          Option(_ServiceResult.map(dispatch(v))(v => Muxed(v, identifier)))

        case _ =>
          None
      }
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

    val encoders: List[PartialFunction[AnyRef, Json]] = List(
      {
        case Muxed(v: GreeterServiceWrapped.GreeterServiceInput, _) =>
          Map("Input" -> Map(GreeterServiceWrapped.serviceId.value -> v.asJson)).asJson
        case Muxed(v: GreeterServiceWrapped.GreeterServiceOutput, _) =>
          Map("Output" -> Map(GreeterServiceWrapped.serviceId.value -> v.asJson)).asJson
      }
    )


    val decoders: List[PartialFunction[DirectedPacket, Decoder.Result[Muxed]]] = List(
      {
        case DirectedPacket("Input", GreeterServiceWrapped.serviceId, packet) =>
          packet.as[GreeterServiceInput].map(v => Muxed(v, GreeterServiceWrapped.serviceId))

        case DirectedPacket("Output", GreeterServiceWrapped.serviceId, packet) =>
          packet.as[GreeterServiceOutput].map(v => Muxed(v, GreeterServiceWrapped.serviceId))
      }
    )
  }
}
