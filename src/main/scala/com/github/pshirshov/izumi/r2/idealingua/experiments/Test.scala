package com.github.pshirshov.izumi.r2.idealingua.experiments

import com.github.pshirshov.izumi.r2.idealingua.experiments
import com.github.pshirshov.izumi.r2.idealingua.experiments.generated._
import com.github.pshirshov.izumi.r2.idealingua.experiments.impls._
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}
import scala.util.Try

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


class TrivialAppTransport[I, O, R[_]](server: Receiver[I, O, R]) extends Transport[I, R[O]] {
  def send(v: I): R[O] = server.receive(v)
}

class PseudoNetwork[I, O, R[_], RT[_]](transport: Transport[I, R[O]])(implicit converter: ServiceResultTransformer[R, RT]) extends Transport[I, RT[O]] {
  def send(v: I): RT[O] = {
    val sent = transport.send(v)
    //println(s"on wire: $v")
    converter.transform(sent)
  }
}


import com.github.pshirshov.izumi.r2.idealingua.experiments.generated.GreeterServiceWrapped._

object Test {

  class FailingMarshallers extends GreeterServiceWrapped.GreeterServiceStringMarshaller {
    override val requestUnmarshaller: Unmarshaller[String, GreeterServiceInput] = (v: String) => ???
    override val requestMarshaller: Marshaller[GreeterServiceInput, String] = (v: GreeterServiceInput) => ???
    override val responseMarshaller: Marshaller[GreeterServiceOutput, String] = (v: GreeterServiceOutput) => ???
    override val responseUnmarshaller: Unmarshaller[String, GreeterServiceOutput] = (v: String) => ???
  }

  class SimpleDemo[R[_] : ServiceResult]
  (
    marshalling: GreeterServiceWrapped.GreeterServiceStringMarshaller
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
    marshalling: GreeterServiceWrapped.GreeterServiceStringMarshaller
  )
  (
    implicit converter: ServiceResultTransformer[RT, R]
    , converter1: ServiceResultTransformer[R, RT]
  ) {


    val service = new AbstractGreeterServer.Impl[R]

    val serverDispatcher = new GreeterServiceWrapped.GreeterServiceDispatcherUnpacking.Impl(service)

    val server = new ServerReceiver(serverDispatcher, marshalling)

    val serverAppTransport: TrivialAppTransport[String, String, R] = new TrivialAppTransport(server)

    val serverNetwork: PseudoNetwork[String, String, R, RT] = new PseudoNetwork[String, String, R, RT](serverAppTransport)

    val clientNetwork: PseudoNetwork[String, String, RT, R] = new PseudoNetwork[String, String, RT, R](serverNetwork)

    val clientDispatcher = new ClientDispatcher(clientNetwork, marshalling)

    val client = new GreeterServiceWrapped.GreeterServiceDispatcherPacking.Impl(clientDispatcher)
  }


  private def testSimple(marshalling: GreeterServiceWrapped.GreeterServiceStringMarshaller): Unit = {
    println()
    println("testSimple...")
    println(Try({
      val demo = new SimpleDemo[Try](marshalling)
      val result = demo.client.greet("John", "Doe")
      result
    }))

    println(Try({
      import ExecutionContext.Implicits._
      val demo = new SimpleDemo[Future](marshalling)
      val result = demo.client.greet("John", "Doe")
      Thread.sleep(100)
      result
    }))

    println(Try({
      val demo = new SimpleDemo[Option](marshalling)
      demo.client.greet("John", "Doe")
    }))
  }


  private def testConverting(marshalling: GreeterServiceWrapped.GreeterServiceStringMarshaller): Unit = {
    import ServiceResultTransformer._

    println()
    println("testConverting...")

    println(Try({
      val demo = new ConvertingDemo[Try, Try](marshalling)
      demo.client.greet("John", "Doe")
    }))

    println(Try({
      import ExecutionContext.Implicits._
      val demo = new ConvertingDemo[Future, Future](marshalling)
      val result = demo.client.greet("John", "Doe")
      Thread.sleep(100)
      result
    }))


    println(Try({
      val demo = new ConvertingDemo[Option, Option](marshalling)
      demo.client.greet("John", "Doe")
    }))
  }

  def main(args: Array[String]): Unit = {
    testSimple(new experiments.generated.GreeterServiceWrapped.GreeterServiceStringMarshallerCirceImpl())
    testConverting(new experiments.generated.GreeterServiceWrapped.GreeterServiceStringMarshallerCirceImpl())

    testSimple(new FailingMarshallers())
    testConverting(new FailingMarshallers())
  }
}
