package com.github.pshirshov.izumi.r2.idealingua.experiments

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.language.{higherKinds, implicitConversions}

import runtime._
import generated._

//--------------------------------------------------------------------------
// Runtime: opinionated part
class ServerReceiver[RequestWire, Request, ResponseWire, Response, R[_] : ServiceResult]
(
  dispatcher: Dispatcher[Request, Response, R]
  , bindings: TransportMarshallers[RequestWire, Request, ResponseWire, Response]
) extends Receiver[RequestWire, ResponseWire, R] with WithResult[R] {
  override protected def _ServiceResult: ServiceResult[R] = implicitly

  def receive(request: RequestWire): R[ResponseWire] = {
    import ServiceResult._
    _Result(bindings.requestUnmarshaller.decode(request))
      .flatMap(dispatcher.dispatch)
      .map(bindings.responseMarshaller.encode)
  }
}

class ClientDispatcher[RequestWire, Request, ResponseWire, Response, R[_] : ServiceResult]
(
  transport: Transport[RequestWire, R[ResponseWire]]
  , bindings: TransportMarshallers[RequestWire, Request, ResponseWire, Response]
) extends Dispatcher[Request, Response, R] with WithResult[R] {
  override protected def _ServiceResult: ServiceResult[R] = implicitly

  def dispatch(input: Request): Result[Response] = {
    import ServiceResult._
    _Result(bindings.requestMarshaller.encode(input))
      .flatMap(transport.send)
      .map(bindings.responseUnmarshaller.decode)
  }
}



//--------------------------------------------------------------------------
// setup context and use
trait AbstractGreeterServer[R[_]] extends GreeterService[R] with WithResult[R] {
  override def greet(name: String, surname: String): Result[String] = _Result {
    s"Hi, $name $surname!"
  }
}

object AbstractGreeterServer {

  class Impl[R[_] : ServiceResult] extends AbstractGreeterServer[R] {
    override protected def _ServiceResult: ServiceResult[R] = implicitly
  }

}


class TrivialAppTransport[I, O, R[_]](server: Receiver[I, O, R]) extends Transport[I, R[O]] {
  def send(v: I): R[O] = server.receive(v)
}

class PseudoNetwork[I, O, R[_], RT[_]](transport: Transport[I, R[O]])(implicit converter: ServiceResultTransformer[R, RT]) extends Transport[I, RT[O]] {
  def send(v: I): RT[O] = {
    val sent = transport.send(v)
    converter.transform(sent)
  }
}


import GreeterServiceWrapped._

object Test {

  type GSMarshaler = TransportMarshallers[GreeterServiceInput, GreeterServiceInput, GreeterServiceOutput, GreeterServiceOutput]

  class PseudoMarshallers extends GSMarshaler {
    override val requestUnmarshaller: Unmarshaller[GreeterServiceInput, GreeterServiceInput] = (v: GreeterServiceInput) => v
    override val requestMarshaller: Marshaller[GreeterServiceInput, GreeterServiceInput] = (v: GreeterServiceInput) => v
    override val responseMarshaller: Marshaller[GreeterServiceOutput, GreeterServiceOutput] = (v: GreeterServiceOutput) => v
    override val responseUnmarshaller: Unmarshaller[GreeterServiceOutput, GreeterServiceOutput] = (v: GreeterServiceOutput) => v

  }

  class FailingMarshallers extends GSMarshaler {
    override val requestUnmarshaller: Unmarshaller[GreeterServiceInput, GreeterServiceInput] = (v: GreeterServiceInput) => ???
    override val requestMarshaller: Marshaller[GreeterServiceInput, GreeterServiceInput] = (v: GreeterServiceInput) => ???
    override val responseMarshaller: Marshaller[GreeterServiceOutput, GreeterServiceOutput] = (v: GreeterServiceOutput) => ???
    override val responseUnmarshaller: Unmarshaller[GreeterServiceOutput, GreeterServiceOutput] = (v: GreeterServiceOutput) => ???

  }

  class SimpleDemo[R[_] : ServiceResult]
  (
    marshalling: GSMarshaler
  ) {


    val service = new AbstractGreeterServer.Impl[R]

    val serverDispatcher = new GreeterServiceWrapped.GreeterServiceDispatcherUnpacking.Impl(service)

    val server = new ServerReceiver(serverDispatcher, marshalling)

    val appTransport = new TrivialAppTransport(server)

    val clientDispatcher = new ClientDispatcher(appTransport, marshalling)
    val client = new GreeterServiceWrapped.GreeterServiceDispatcherPacking.Impl(clientDispatcher)
  }



  class ConvertingDemo[R[_] : ServiceResult, RT[_] : ServiceResult]
  (
    marshalling: GSMarshaler
  )
  (
    implicit converter: ServiceResultTransformer[RT, R]
    , converter1: ServiceResultTransformer[R, RT]
  ) {


    val service = new AbstractGreeterServer.Impl[R]

    val serverDispatcher = new GreeterServiceWrapped.GreeterServiceDispatcherUnpacking.Impl(service)

    val server = new ServerReceiver(serverDispatcher, marshalling)

    val serverAppTransport = new TrivialAppTransport(server)

    val serverNetwork: Transport[GreeterServiceInput, RT[GreeterServiceOutput]] = new PseudoNetwork[GreeterServiceInput, GreeterServiceOutput, R, RT](serverAppTransport)

    val clientNetwork: Transport[GreeterServiceInput, R[GreeterServiceOutput]] = new PseudoNetwork[GreeterServiceInput, GreeterServiceOutput, RT, R](serverNetwork)

    val clientDispatcher = new ClientDispatcher(clientNetwork, marshalling)

    val client = new GreeterServiceWrapped.GreeterServiceDispatcherPacking.Impl(clientDispatcher)
  }


  private def testSimple(marshalling: GSMarshaler): Unit = {
    {
      val demo = new SimpleDemo[Try](marshalling)
      val result = demo.client.greet("John", "Doe")
      println(result)
    }

    {
      import ExecutionContext.Implicits._
      val demo = new SimpleDemo[Future](marshalling)
      val result = demo.client.greet("John", "Doe")
      Thread.sleep(100)
      println(result)
    }

    {
      val demo = new SimpleDemo[Option](marshalling)
      val result = demo.client.greet("John", "Doe")
      println(result)
    }
  }


  private def testConverting(marshalling: GSMarshaler): Unit = {
    import ServiceResultTransformer._



    {
      val demo = new ConvertingDemo[Try, Try](marshalling)
      val result = demo.client.greet("John", "Doe")
      println(result)
    }

    {
      import ExecutionContext.Implicits._
      val demo = new ConvertingDemo[Future, Future](marshalling)
      val result = demo.client.greet("John", "Doe")
      Thread.sleep(100)
      println(result)
    }

    {
      val demo = new ConvertingDemo[Option, Option](marshalling)
      val result = demo.client.greet("John", "Doe")
      println(result)
    }
  }

  def main(args: Array[String]): Unit = {
    testSimple(new PseudoMarshallers())
    testConverting(new PseudoMarshallers())

    println(Try(testSimple(new FailingMarshallers())))
    println(Try(testConverting(new FailingMarshallers())))
  }
}
