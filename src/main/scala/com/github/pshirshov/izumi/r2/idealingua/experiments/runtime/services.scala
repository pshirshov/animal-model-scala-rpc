package com.github.pshirshov.izumi.r2.idealingua.experiments.runtime

import com.github.pshirshov.izumi.r2.idealingua.experiments.InContext

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

trait WrappedServiceDefinition {
  this: IdentifiableServiceDefinition =>

  type Input <: AnyRef
  type Output <: AnyRef
  type Service[_[_]]

  def client[R[_] : ServiceResult](dispatcher: Dispatcher[Input, Output, R]): Service[R]


  def server[R[_] : ServiceResult, C](service: Service[R]): Dispatcher[InContext[Input, C], Output, R]

}


trait WrappedUnsafeServiceDefinition {
  this: WrappedServiceDefinition =>
  def clientUnsafe[R[_] : ServiceResult](dispatcher: Dispatcher[MuxRequest[_], MuxResponse[_], R]): Service[R]

  def serverUnsafe[R[_] : ServiceResult, C](service: Service[R]): UnsafeDispatcher[C, R]

}
