package com.github.pshirshov.izumi.r2.idealingua.experiments

import com.github.pshirshov.izumi.r2.idealingua.experiments.generated._
import com.github.pshirshov.izumi.r2.idealingua.experiments.impls._
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.circe.MuxedCodec
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.{TransportMarshallers, _}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}
import scala.util.Try


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

class SimpleMarshallerImpl(codec: MuxedCodec) extends TransportMarshallers[String, MuxRequest[_], MuxResponse[_], String] {


//  import codec._
//  import io.circe.syntax._
//  import io.circe.parser._
//
//  override def decodeRequest(requestWire: String): MuxRequest[_] = {
//    val parsed = parse(requestWire).flatMap(_.as[MuxRequest[_]])
//    println(s"Request parsed: $parsed")
//    parsed.right.get
//  }
//
//  override def decodeResponse(responseWire: String): MuxResponse[_] = {
//    val parsed = parse(responseWire).flatMap(_.as[MuxResponse[_]])
//    println(s"Response parsed: $parsed")
//    parsed.right.get
//  }
//
//  override def encodeRequest(request: MuxRequest[_]): String = {
//    val out = request.asJson.noSpaces
//    println(s"Request serialized: $out")
//    out
//  }
//
//  override def encodeResponse(response: MuxResponse[_]): String = {
//    val out = response.asJson.noSpaces
//    println(s"Response serialized: $out")
//    out
//  }
  override def decodeRequest(requestWire: String): MuxRequest[_] = ???

  override def encodeRequest(request: MuxRequest[_]): String = ???

  override def decodeResponse(responseWire: String): MuxResponse[_] = ???

  override def encodeResponse(response: MuxResponse[_]): String = ???
}

class DirectMarshallerImpl() extends TransportMarshallers[String, GreeterServiceWrapped.GreeterServiceInput, GreeterServiceWrapped.GreeterServiceOutput, String] {

  import io.circe.syntax._
  import io.circe.parser._

  override def decodeRequest(requestWire: String): GreeterServiceWrapped.GreeterServiceInput = {
    parse(requestWire).flatMap(_.as[GreeterServiceWrapped.GreeterServiceInput]).right.get
  }

  override def encodeRequest(request: GreeterServiceWrapped.GreeterServiceInput): String = {
    request.asJson.noSpaces
  }

  override def decodeResponse(responseWire: String): GreeterServiceWrapped.GreeterServiceOutput = {
    parse(responseWire).flatMap(_.as[GreeterServiceWrapped.GreeterServiceOutput]).right.get
  }

  override def encodeResponse(response: GreeterServiceWrapped.GreeterServiceOutput): String = {
    response.asJson.noSpaces
  }

}


object TestMul {

  class SingleServiceDemo[R[_] : ServiceResult] {
    println(s"SingleServiceDemo: Running demo with container ${implicitly[ServiceResult[R]]}")
    println()

    val service = new AbstractGreeterServer.Impl[R]
    val serverDispatcher = GreeterServiceWrapped.server(service)

    val marshalling: TransportMarshallers[String
      , GreeterServiceWrapped.GreeterServiceInput
      , GreeterServiceWrapped.GreeterServiceOutput
      , String
      ] = new DirectMarshallerImpl()
    val server = new ServerReceiver(serverDispatcher, marshalling)

    println("Testing direct RPC call...")
    val request = marshalling.encodeRequest(GreeterServiceWrapped.GreetInput("John", "Doe"))
    println(s"RPC call performed: ${server.receive(request)}")

    val network = new NetworkSimulator(server)

    val clientDispatcher = new ClientDispatcher(network, marshalling)
    val greeterClient = GreeterServiceWrapped.client(clientDispatcher)
    println()
    println("Testing client RPC calls...")
    println(greeterClient.greet("Best", "Client"))
    println()
  }

  class MultiplexingDemo[R[_] : ServiceResult] {
    final val c = implicitly[ServiceResult[R]]
    println(s"MultiplexingDemo: Running demo with container ${implicitly[ServiceResult[R]]}")
    println()

    val greeterService = new AbstractGreeterServer.Impl[R]
    val calculatorService = new AbstractCalculatorServer.Impl[R]
    val greeterDispatcher = GreeterServiceWrapped.serverUnsafe(greeterService)
    val calculatorDispatcher = CalculatorServiceWrapped.serverUnsafe(calculatorService)

    val list: List[UnsafeDispatcher[_, _, R]] = List(greeterDispatcher, calculatorDispatcher)
    val muxer = new ServerMultiplexor[R](list)

    // all the type annotations below are optional, infering works
    val codecs = List(GreeterServiceWrapped, CalculatorServiceWrapped)
    val marshalling: TransportMarshallers[String, MuxRequest[_], MuxResponse[_], String] = ??? //new SimpleMarshallerImpl(OpinionatedMuxedCodec(codecs))
    val server = new ServerReceiver(muxer, marshalling)

    //    println("Testing direct RPC call...")
    //    val request = marshalling.encodeRequest(Muxed(GreeterServiceWrapped.GreetInput("John", "Doe"), GreeterServiceWrapped.serviceId))
    //    println(s"RPC call performed: ${server.receive(request)}")

    val network: NetworkSimulator[String, String, R] = new NetworkSimulator(server)

    val clientDispatcher: ClientDispatcher[String, MuxRequest[_], MuxResponse[_], String, R] = new ClientDispatcher(network, marshalling)
    val greeterClient = GreeterServiceWrapped.clientUnsafe(clientDispatcher)
    val calculatorClient = CalculatorServiceWrapped.clientUnsafe(clientDispatcher)

    println()
    println("Testing client RPC calls...")
    println(greeterClient.greet("Best", "Client"))
    println(calculatorClient.sum(1, 2))
    println()
  }

  class ConvertingDemo[R[_] : ServiceResult, RT[_] : ServiceResult](
                                                                     implicit converter: ServiceResultTransformer[RT, R]
                                                                     , converter1: ServiceResultTransformer[R, RT]
                                                                   ) {
    println(s"ConvertingDemo: Running demo with container ${implicitly[ServiceResult[R]]}")
    println()

    val greeterService = new AbstractGreeterServer.Impl[R]
    val calculatorService = new AbstractCalculatorServer.Impl[R]
    val greeterDispatcher = GreeterServiceWrapped.serverUnsafe(greeterService)
    val calculatorDispatcher = CalculatorServiceWrapped.serverUnsafe(calculatorService)

    val list: List[UnsafeDispatcher[_, _, R]] = List(greeterDispatcher, calculatorDispatcher)
    val muxer = new ServerMultiplexor[R](list)

    // all the type annotations below are optional, infering works
    val codecs = List(GreeterServiceWrapped, CalculatorServiceWrapped)
    val marshalling: TransportMarshallers[String, MuxRequest[_], MuxResponse[_], String] = ??? //new SimpleMarshallerImpl(OpinionatedMuxedCodec(codecs))
    val server = new ServerReceiver(muxer, marshalling)

    //    println("Testing direct RPC call...")
    //    val request = marshalling.encodeRequest(Muxed(GreeterServiceWrapped.GreetInput("John", "Doe"), GreeterServiceWrapped.serviceId))
    //    println(s"RPC call performed: ${server.receive(request)}")

    val transport: Transport[String, R[String]] = {
      val network: NetworkSimulator[String, String, R] = new NetworkSimulator(server)

      val wrapperConverter = new TransformingNetworkSimulator[String, String, R, RT](network)
      val wrapperRestorer = new TransformingNetworkSimulator[String, String, RT, R](wrapperConverter)
      wrapperRestorer
    }

    val clientDispatcher: ClientDispatcher[String, MuxRequest[_], MuxResponse[_], String, R] = new ClientDispatcher(transport, marshalling)
    val greeterClient = GreeterServiceWrapped.clientUnsafe(clientDispatcher)
    val calculatorClient = CalculatorServiceWrapped.clientUnsafe(clientDispatcher)

    println()
    println("Testing client RPC calls...")
    println(greeterClient.greet("Best", "Client"))
    println(calculatorClient.sum(1, 2))
    println()
  }

  private def testSimple(): Unit = {
    println()
    println("testSimple...")
    println(Try({
      val demo = new SingleServiceDemo[Try]()
      val result = demo.greeterClient.greet("John", "Doe")
      result
    }))

    println(Try({
      import ExecutionContext.Implicits._
      val demo = new SingleServiceDemo[Future]()
      val result = demo.greeterClient.greet("John", "Doe")
      Thread.sleep(100)
      result
    }))

    println(Try({
      val demo = new SingleServiceDemo[Option]()
      demo.greeterClient.greet("John", "Doe")
    }))
  }

  def main(args: Array[String]): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

    new SingleServiceDemo[Option]
    new SingleServiceDemo[Try]
    new SingleServiceDemo[Future]

    new MultiplexingDemo[Option]
    new MultiplexingDemo[Try]
    new MultiplexingDemo[Future]

    implicit val transformOption: ServiceResultTransformer[Option, Try] = new ServiceResultTransformer[Option, Try] {
      override def transform[A](r: Option[A]): Try[A] = {
        Try(r.get)
      }
    }
    implicit val transformFuture: ServiceResultTransformer[Try, Option] = new ServiceResultTransformer[Try, Option] {
      override def transform[A](r: Try[A]): Option[A] = {
        r.toOption
      }
    }

    new ConvertingDemo[Option, Option]
    new ConvertingDemo[Option, Try]
  }
}
