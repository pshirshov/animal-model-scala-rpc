package com.github.pshirshov.izumi.r2.idealingua.experiments.runtime

import scala.language.higherKinds

trait IdentifiableServiceDefinition {
  def serviceId: ServiceId
}

trait Dispatcher[In, Out, R[_]] extends WithResultType[R] {
  def dispatch(input: In): Result[Out]
}

trait Receiver[In, Out, R[_]] extends WithResultType[R] {
  def receive(input: In): Result[Out]
}
