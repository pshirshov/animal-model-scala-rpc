package com.github.pshirshov.izumi.r2.idealingua.experiments.runtime

import scala.language.higherKinds

trait UnsafeDispatcher[In, Out, R[_]] extends WithResultType[R] {
  def identifier: ServiceId

  def dispatchUnsafe(input: MuxRequest[_]): Option[Result[MuxResponse[_]]]
}

case class Method(service: ServiceId, methodId: MethodId)

case class ReqBody(value: AnyRef) extends AnyRef

case class ResBody(value: AnyRef) extends AnyRef

case class MuxResponse[T <: AnyRef](v: T, method: Method) {
  def body: ResBody = ResBody(v)
}
case class MuxRequest[T <: AnyRef](v: T, method: Method) {
  def body: ReqBody = ReqBody(v)
}

//case class Demuxed(v: AnyRef, service: ServiceId)

case class ServiceId(value: String) extends AnyVal
case class MethodId(value: String) extends AnyVal



class ServerMultiplexor[R[_]](dispatchers: List[UnsafeDispatcher[_, _, R]]) extends Dispatcher[MuxRequest[_], MuxResponse[_], R] {
  override def dispatch(input: MuxRequest[_]): Result[MuxResponse[_]] = {
    dispatchers.foreach {
      d =>
        d.dispatchUnsafe(input) match {
          case Some(v) =>
            return v
          case None =>
        }
    }
    throw new MultiplexingException(s"Cannot handle $input, services: $dispatchers", input)
  }
}
