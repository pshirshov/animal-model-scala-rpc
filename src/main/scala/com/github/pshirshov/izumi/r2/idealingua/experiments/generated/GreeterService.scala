package com.github.pshirshov.izumi.r2.idealingua.experiments.generated

import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime._
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

object GreeterServiceWrapped {

  sealed trait GreeterServiceInput

  case class GreetInput(name: String, surname: String) extends GreeterServiceInput

  sealed trait GreeterServiceOutput

  case class GreetOutput(value: String) extends GreeterServiceOutput


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

  trait GreeterServiceDispatcherPacking[R[_]]
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

  class GreeterServiceSafeToUnsafeBridge[R[_] : ServiceResult](dispatcher: Dispatcher[AnyRef, AnyRef, R]) extends Dispatcher[GreeterServiceInput, GreeterServiceOutput, R] with WithResult[R] {
    override protected def _ServiceResult: ServiceResult[R] = implicitly

    import ServiceResult._

    override def dispatch(input: GreeterServiceInput): Result[GreeterServiceOutput] = {
      dispatcher.dispatch(input).map {
        case t: GreeterServiceOutput =>
          t
        case o =>
          throw new TypeMismatchException(s"Unexpected output in GreeterServiceSafeToUnsafeBridge.dispatch: $o", o)
      }
    }
  }

  object GreeterServiceDispatcherPacking {

    class Impl[R[_] : ServiceResult](val dispatcher: Dispatcher[GreeterServiceInput, GreeterServiceOutput, R]) extends GreeterServiceDispatcherPacking[R] {
      override protected def _ServiceResult: ServiceResult[R] = implicitly
    }

  }


  trait GreeterServiceDispatcherUnpacking[R[_]]
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

    def dispatchUnsafe(input: AnyRef): Option[Result[AnyRef]] = {
      input match {
        case v: GreeterServiceInput =>
          Option(_ServiceResult.map(dispatch(v))(v => v))

        case _ =>
          None
      }
    }
  }

  object GreeterServiceDispatcherUnpacking {

    class Impl[R[_] : ServiceResult](val service: GreeterService[R]) extends GreeterServiceDispatcherUnpacking[R] {
      override protected def _ServiceResult: ServiceResult[R] = implicitly
    }

  }

  type GreeterServiceStringMarshaller = TransportMarshallers[String, GreeterServiceInput, String, GreeterServiceOutput]

  class GreeterServiceStringMarshallerCirceImpl extends GreeterServiceStringMarshaller {

    import io.circe.parser._
    import io.circe.syntax._

    override val requestUnmarshaller: Unmarshaller[String, GreeterServiceInput] = new Unmarshaller[String, GreeterServiceInput] {
      override def decode(v: String): GreeterServiceInput = {
        parse(v).flatMap(_.as[GreeterServiceInput]) match {
          case Right(value) =>
            value
          case Left(problem) =>
            throw new UnparseableDataException(s"Cannot parse $problem into GreeterServiceInput")
        }
      }

      override def decodeUnsafe(v: String): Option[GreeterServiceInput] = {
        parse(v).flatMap(_.as[GreeterServiceInput]) match {
          case Right(value) =>
            Some(value)
          case Left(problem) =>
            None
        }
      }
    }

    override val responseUnmarshaller: Unmarshaller[String, GreeterServiceOutput] = new Unmarshaller[String, GreeterServiceOutput] {
      override def decode(v: String): GreeterServiceOutput = {
        parse(v).flatMap(_.as[GreeterServiceOutput]) match {
          case Right(value) =>
            value
          case Left(problem) =>
            throw new UnparseableDataException(s"Cannot parse $problem into GreeterServiceOutput")
        }
      }

      override def decodeUnsafe(v: String): Option[GreeterServiceOutput] = {
        parse(v).flatMap(_.as[GreeterServiceOutput]) match {
          case Right(value) =>
            Some(value)
          case Left(problem) =>
            None
        }
      }
    }

    override val requestMarshaller: Marshaller[GreeterServiceInput, String] = new Marshaller[GreeterServiceInput, String] {
      override def encode(v: GreeterServiceInput): String = {
        v.asJson.noSpaces
      }

      override def encodeUnsafe(v: AnyRef): Option[String] = {
        v match {
          case i: GreeterServiceInput =>
            Some(encode(i))
          case _ =>
            None
        }
      }
    }

    override val responseMarshaller: Marshaller[GreeterServiceOutput, String] = new Marshaller[GreeterServiceOutput, String] {
      override def encode(v: GreeterServiceOutput): String = {
        v.asJson.noSpaces
      }

      override def encodeUnsafe(v: AnyRef): Option[String] = {
        v match {
          case i: GreeterServiceOutput =>
            Some(encode(i))
          case _ =>
            None
        }
      }
    }

  }

}
