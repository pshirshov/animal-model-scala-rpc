package com.github.pshirshov.izumi.r2.idealingua.experiments.runtime

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.language.{higherKinds, implicitConversions}

//--------------------------------------------------------------------------
// Runtime: unopinionated part
trait ServiceResult[R[_]] {
  @inline def map[A, B](r: R[A])(f: A => B): R[B]

  @inline def flatMap[A, B](r: R[A])(f: A => R[B]): R[B]

  @inline def pure[A](v: => A): R[A]
}

object ServiceResult {
  type Id[T] = T

  @inline implicit def toOps[R[_], A](value: R[A]): ServiceResultOps[R, A] = new ServiceResultOps[R, A](value)

  class ServiceResultOps[R[_], A](val value: R[A]) extends AnyVal {
    @inline def map[B](f: A => B)(implicit serviceResult: ServiceResult[R]): R[B] = serviceResult.map(value)(f)

    @inline def flatMap[B](f: A => R[B])(implicit serviceResult: ServiceResult[R]): R[B] = serviceResult.flatMap(value)(f)
  }

  implicit object ServiceResultId extends ServiceResult[Id] {
    @inline override def map[A, B](r: Id[A])(f: A => B) = f(r)

    @inline override def flatMap[A, B](fa: Id[A])(f: A => Id[B]): Id[B] = f(fa)

    @inline override def pure[A](v: => A): Id[A] = v
  }

  implicit object ServiceResultOption extends ServiceResult[Option] {
    @inline override def map[A, B](r: Option[A])(f: A => B): Option[B] = r.map(f)

    @inline override def flatMap[A, B](r: Option[A])(f: A => Option[B]): Option[B] = r.flatMap(f)

    @inline override def pure[A](v: => A): Option[A] = Option(v)
  }

  implicit object ServiceResultTry extends ServiceResult[Try] {
    @inline override def map[A, B](r: Try[A])(f: A => B): Try[B] = r.map(f)

    @inline override def flatMap[A, B](r: Try[A])(f: A => Try[B]): Try[B] = r.flatMap(f)

    @inline override def pure[A](v: => A): Try[A] = Try(v)
  }

  implicit def toServiceResultFutureOps(implicit ec: ExecutionContext): ServiceResultFuture = new ServiceResultFuture

  class ServiceResultFuture(implicit ec: ExecutionContext) extends ServiceResult[Future] {
    @inline override def map[A, B](r: Future[A])(f: A => B): Future[B] = r.map(f)

    @inline override def flatMap[A, B](r: Future[A])(f: A => Future[B]): Future[B] = r.flatMap(f)

    @inline override def pure[A](v: => A): Future[A] = Future(v)
  }

}

trait ServiceResultTransformer[R1[_], R2[_]] {
  def transform[A](r: R1[A]): R2[A]
}

object ServiceResultTransformer {
  implicit val transformId: ServiceResultTransformer[ServiceResult.Id, ServiceResult.Id] = new ServiceResultTransformerId[ServiceResult.Id]
  implicit val transformTry: ServiceResultTransformer[Try, Try] = new ServiceResultTransformerId[Try]
  implicit val transformOption: ServiceResultTransformer[Option, Option] = new ServiceResultTransformerId[Option]
  implicit val transformFuture: ServiceResultTransformer[Future, Future] = new ServiceResultTransformerId[Future]

  class ServiceResultTransformerId[R[_]] extends ServiceResultTransformer[R, R] {
    override def transform[A](r: R[A]): R[A] = r
  }
}

trait WithResultType[R[_]] {
  type Result[T] = R[T]
}

trait WithResult[R[_]] extends WithResultType[R] {
  protected def _ServiceResult: ServiceResult[R]

  protected def _Result[T](value: => T): R[T] = _ServiceResult.pure(value)
}

trait Marshaller[Value, Marshalled] {
  def encode(v: Value): Marshalled
}

trait Unmarshaller[Marshalled, Value] {
  def decode(v: Marshalled): Value
}

trait Transport[RequestWire, ResponseWire] {
  def send(v: RequestWire): ResponseWire
}

trait TransportMarshallers[RequestWire, Request, ResponseWire, Response] {
  val requestUnmarshaller: Unmarshaller[RequestWire, Request]
  val requestMarshaller: Marshaller[Request, RequestWire]

  val responseMarshaller: Marshaller[Response, ResponseWire]
  val responseUnmarshaller: Unmarshaller[ResponseWire, Response]
}

trait Dispatcher[In, Out, R[_]] extends WithResultType[R] {
  def dispatch(input: In): Result[Out]
}

trait Receiver[In, Out, R[_]] extends WithResultType[R] {
  def receive(input: In): Result[Out]
}

trait TransportException

class UnparseableDataException(message: String) extends RuntimeException(message) with TransportException
class TypeMismatchException(message: String, val v: Any) extends RuntimeException(message) with TransportException
