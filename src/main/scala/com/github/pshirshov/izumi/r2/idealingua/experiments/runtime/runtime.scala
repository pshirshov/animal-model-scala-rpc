package com.github.pshirshov.izumi.r2.idealingua.experiments.runtime


import scala.language.{higherKinds, implicitConversions}


trait Transport[RequestWire, ResponseWire] {
  def send(v: RequestWire): ResponseWire
}

trait TransportMarshallers[RequestWire, Request, Response, ResponseWire] {
  def decodeRequest(requestWire: RequestWire): Request

  def encodeRequest(request: Request): RequestWire

  def decodeResponse(responseWire: ResponseWire): Response

  def encodeResponse(response: Response): ResponseWire
}



class ServerReceiver[RequestWire, Request, Response, ResponseWire, R[_] : ServiceResult]
(
  dispatcher: Dispatcher[Request, Response, R]
  , bindings: TransportMarshallers[RequestWire, Request, Response, ResponseWire]
) extends Receiver[RequestWire, ResponseWire, R] with WithResult[R] {
  override protected def _ServiceResult: ServiceResult[R] = implicitly

  def receive(request: RequestWire): R[ResponseWire] = {
    import ServiceResult._
    _Result(bindings.decodeRequest(request))
      .flatMap(dispatcher.dispatch)
      .map(bindings.encodeResponse)
  }
}


class ClientDispatcher[RequestWire, Request, Response, ResponseWire, R[_] : ServiceResult]
(
  transport: Transport[RequestWire, R[ResponseWire]]
  , bindings: TransportMarshallers[RequestWire, Request, Response, ResponseWire]
) extends Dispatcher[Request, Response, R] with WithResult[R] {
  override protected def _ServiceResult: ServiceResult[R] = implicitly

  def dispatch(input: Request): Result[Response] = {
    import ServiceResult._
    _Result(bindings.encodeRequest(input))
      .flatMap(transport.send)
      .map(bindings.decodeResponse)
  }
}




