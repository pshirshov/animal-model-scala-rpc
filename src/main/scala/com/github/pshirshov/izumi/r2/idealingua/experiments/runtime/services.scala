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

trait WrappedServiceDefinition {
  this: IdentifiableServiceDefinition =>

  type Input
  type Output
  type ServiceServer[_[_], _]
  type ServiceClient[_[_]]

  def client[R[_] : ServiceResult](dispatcher: Dispatcher[Input, Output, R]): ServiceClient[R]


  def server[R[_] : ServiceResult, C](service: ServiceServer[R, C]): Dispatcher[InContext[Input, C], Output, R]

}


trait WrappedUnsafeServiceDefinition {
  this: WrappedServiceDefinition =>
  def clientUnsafe[R[_] : ServiceResult](dispatcher: Dispatcher[MuxRequest[_], MuxResponse[_], R]): ServiceClient[R]

  def serverUnsafe[R[_] : ServiceResult, C](service: ServiceServer[R, C]): UnsafeDispatcher[C, R]

}
