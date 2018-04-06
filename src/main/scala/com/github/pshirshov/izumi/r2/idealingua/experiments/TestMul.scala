package com.github.pshirshov.izumi.r2.idealingua.experiments

import com.github.pshirshov.izumi.r2.idealingua.experiments.generated._
import com.github.pshirshov.izumi.r2.idealingua.experiments.impls._
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.{TransportMarshallers, _}
import io.circe.parser._
import io.circe.syntax._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}
import scala.util.Try

//--------------------------------------------------------------------------
// setup context and use

class NetworkSimulator[I, O, R[_]](server: Receiver[I, O, R]) extends Transport[I, R[O]] {
  def send(v: I): R[O] = {
    val received = server.receive(v)
    println(s"NetworkSimulator: value on wire: $v")
    received
  }
}

class TransformingNetworkSimulator[I, O, R[_], RT[_]](transport: Transport[I, R[O]])(implicit converter: ServiceResultTransformer[R, RT]) extends Transport[I, RT[O]] {
  def send(v: I): RT[O] = {
    val sent = transport.send(v)
    println(s"TransformingNetworkSimulator: value on wire: $v")
    converter.transform(sent)
  }
}

class SimpleMarshallerImpl(codec: MuxedCodec) extends TransportMarshallers[String, Muxed, Muxed, String] {

  import codec._

  override def decodeRequest(requestWire: String): Muxed = {
    val parsed = parse(requestWire).flatMap(_.as[Muxed])
    println(s"Request parsed: $parsed")
    parsed.right.get
  }

  override def decodeResponse(responseWire: String): Muxed = {
    val parsed = parse(responseWire).flatMap(_.as[Muxed])
    println(s"Response parsed: $parsed")
    parsed.right.get
  }

  override def encodeRequest(request: Muxed): String = {
    val out = request.asJson.noSpaces
    println(s"Request serialized: $out")
    out
  }

  override def encodeResponse(response: Muxed): String = {
    val out = response.asJson.noSpaces
    println(s"Response serialized: $out")
    out
  }
}

object Test {

  class SimpleDemo[R[_] : ServiceResult] {
    final val c = implicitly[ServiceResult[R]]
    println(s"Running demo with container $c")
    println()

    val greeterService = new AbstractGreeterServer.Impl[R]
    val calculatorService = new AbstractCalculatorServer.Impl[R]
    val greeterDispatcher = new GreeterServiceWrapped.GreeterServiceDispatcherUnpacking.Impl(greeterService)
    val calculatorDispatcher = new CalculatorServiceWrapped.CalculatorServiceDispatcherUnpacking.Impl(calculatorService)

    val list: List[UnsafeDispatcher[_, _, R]] = List(greeterDispatcher, calculatorDispatcher)
    val multiplexor = new ServerMultiplexor[R](list)

    // all the type annotations below are optional, infering works
    val codecs = List(GreeterServiceWrapped.CodecProvider, CalculatorServiceWrapped.CodecProvider)
    val marshalling: TransportMarshallers[String, Muxed, Muxed, String] = new SimpleMarshallerImpl(new OpinionatedMuxedCodec(codecs))
    val server = new ServerReceiver(multiplexor, marshalling)

    println("Testing direct RPC call...")
    val request = marshalling.encodeRequest(Muxed(GreeterServiceWrapped.GreetInput("John", "Doe"), GreeterServiceWrapped.serviceId))
    println(s"RPC call performed: ${server.receive(request)}")

    val network: NetworkSimulator[String, String, R] = new NetworkSimulator(server)

    val clientDispatcher: ClientDispatcher[String, Muxed, Muxed, String, R] = new ClientDispatcher(network, marshalling)
    val greeterClient = new GreeterServiceWrapped.GreeterServiceDispatcherPacking.Impl(new GreeterServiceWrapped.GreeterServiceSafeToUnsafeBridge(clientDispatcher))
    val calculatorClient = new CalculatorServiceWrapped.CalculatorServiceDispatcherPacking.Impl(new CalculatorServiceWrapped.CalculatorServiceSafeToUnsafeBridge(clientDispatcher))

    println()
    println("Testing client RPC calls...")
    println(greeterClient.greet("Best", "Client"))
    println(calculatorClient.sum(1, 2))
    println()
  }

  def main(args: Array[String]): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
    new SimpleDemo[Option]
    new SimpleDemo[Try]
    new SimpleDemo[Future]
  }
}
