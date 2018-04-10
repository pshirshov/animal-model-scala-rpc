package com.github.pshirshov.izumi.r2.idealingua.experiments.runtime

import scala.language.higherKinds


trait UnsafeDispatcher[In, Out, R[_]] extends WithResultType[R] {
  def identifier: ServiceId

  def dispatchUnsafe(input: Muxed): Option[Result[Muxed]]
}

case class Muxed(v: AnyRef, service: ServiceId)

case class Demuxed(v: AnyRef, service: ServiceId)

case class ServiceId(value: String) extends AnyVal

