package com.github.pshirshov.izumi.r2.idealingua.experiments.runtime


import scala.language.higherKinds

trait UnsafeDispatcher[Ctx, R[_]] extends WithResultType[R] {
  def identifier: ServiceId

  def dispatchUnsafe(input: InContext[MuxRequest[Product], Ctx]): Option[Result[MuxResponse[Product]]]
}

case class Method(service: ServiceId, methodId: MethodId)

case class ReqBody(value: Product) extends AnyRef

case class ResBody(value: Product) extends AnyRef

case class MuxResponse[T <: Product](v: T, method: Method) {
  def body: ResBody = ResBody(v)
}

case class MuxRequest[T <: Product](v: T, method: Method) {
  def body: ReqBody = ReqBody(v)
}

case class ServiceId(value: String) extends AnyVal

case class MethodId(value: String) extends AnyVal


class ServerMultiplexor[R[_] : ServiceResult, Ctx](dispatchers: List[UnsafeDispatcher[Ctx, R]])
  extends Dispatcher[InContext[MuxRequest[Product], Ctx], MuxResponse[Product], R]
    with WithResult[R] {
  override protected def _ServiceResult: ServiceResult[R] = implicitly

  type Input = InContext[MuxRequest[Product], Ctx]
  type Output = MuxResponse[Product]

  override def dispatch(input: Input): Result[Output] = {
    dispatchers.foreach {
      d =>
        d.dispatchUnsafe(input) match {
          case Some(v) =>
            return _ServiceResult.map(v)(v => MuxResponse(v.v, v.method))
          case None =>
        }
    }
    throw new MultiplexingException(s"Cannot handle $input, services: $dispatchers", input)
  }
}
