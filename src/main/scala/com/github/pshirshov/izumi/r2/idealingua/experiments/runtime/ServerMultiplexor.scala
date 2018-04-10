package com.github.pshirshov.izumi.r2.idealingua.experiments.runtime

import scala.language.higherKinds

trait UnsafeDispatcher[In, Out, R[_]] extends WithResultType[R] {
  def identifier: ServiceId

  def dispatchUnsafe(input: Muxed): Option[Result[Muxed]]
}

case class Muxed(v: AnyRef, service: ServiceId)

case class Demuxed(v: AnyRef, service: ServiceId)

case class ServiceId(value: String) extends AnyVal



class ServerMultiplexor[R[_]](dispatchers: List[UnsafeDispatcher[_, _, R]]) extends Dispatcher[Muxed, Muxed, R] {
  override def dispatch(input: Muxed): Result[Muxed] = {
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
