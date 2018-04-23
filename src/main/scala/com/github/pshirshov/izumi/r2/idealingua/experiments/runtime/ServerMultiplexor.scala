package com.github.pshirshov.izumi.r2.idealingua.experiments.runtime


import scala.language.higherKinds

trait UnsafeDispatcher[Ctx, R[_]] extends WithResultType[R] {
  def identifier: ServiceId

  def dispatchUnsafe(input: InContext[MuxRequest[Any], Ctx]): Option[Result[MuxResponse[Any]]]
}

case class Method(service: ServiceId, methodId: MethodId)

case class ReqBody(value: Any) extends AnyRef

case class ResBody(value: Any) extends AnyRef

case class MuxResponse[T](v: T, method: Method) {
  def body: ResBody = ResBody(v)
}

case class MuxRequest[T](v: T, method: Method) {
  def body: ReqBody = ReqBody(v)
}

case class ServiceId(value: String) extends AnyVal

case class MethodId(value: String) extends AnyVal


class ServerMultiplexor[R[_] : ServiceResult, Ctx](dispatchers: List[UnsafeDispatcher[Ctx, R]])
  extends Dispatcher[InContext[MuxRequest[Any], Ctx], MuxResponse[Any], R]
    with WithResult[R] {
  override protected def _ServiceResult: ServiceResult[R] = implicitly

  type Input = InContext[MuxRequest[Any], Ctx]
  type Output = MuxResponse[Any]

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
