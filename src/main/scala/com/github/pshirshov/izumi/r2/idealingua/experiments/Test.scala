package com.github.pshirshov.izumi.r2.idealingua.experiments

import com.github.pshirshov.izumi.r2.idealingua.experiments.generated._
import com.github.pshirshov.izumi.r2.idealingua.experiments.impls._
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.circe.{MuxedCodec, OpinionatedMuxedCodec}
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.{TransportMarshallers, _}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}
import scala.util.Try


case class DummyContext(ip: String)
case class InContext[V <: AnyRef, Ctx](value: V, context: Ctx)


class NetworkSimulator[I, IC, O, R[_]](server: Receiver[IC, O, R], contextProvider: I => IC) extends Transport[I, R[O]] {
  def send(v: I): R[O] = {
    val received = server.receive(contextProvider(v))
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


  import codec._
  import io.circe.syntax._
  import io.circe.parser._

  override def decodeRequest(requestWire: String): MuxRequest[_] = {
    val parsed = parse(requestWire).flatMap {
      v => v.asObject match {
        case Some(o) =>
          Right(o)
        case None =>
          throw new UnparseableDataException(s"Not a json object: $requestWire")
      }
    }.map {
      r =>
        implicit val method: Method = Method(ServiceId(r.apply("service").flatMap(_.asString).get)
          , MethodId(r.apply("method").flatMap(_.asString).get))

        MuxRequest[AnyRef](
          r.apply("body").map(_.as[ReqBody].right.get.value).get
          , method
        )
    }
    println(s"Response parsed: $parsed")
    parsed.right.get
  }

  override def decodeResponse(responseWire: String): MuxResponse[_] = {
    val parsed = parse(responseWire).flatMap {
      v => v.asObject match {
        case Some(o) =>
          Right(o)
        case None =>
          throw new UnparseableDataException(s"Not a json object: $responseWire")
      }
    }.map {
      r =>
        implicit val method: Method = Method(ServiceId(r.apply("service").flatMap(_.asString).get)
          , MethodId(r.apply("method").flatMap(_.asString).get))

        MuxResponse[AnyRef](
          r.apply("body").map(_.as[ResBody].right.get.value).get
          , method
        )
    }
    println(s"Response parsed: $parsed")
    parsed.right.get
  }

  override def encodeRequest(request: MuxRequest[_]): String = {
    val out = request.body.asJson
    val tree = Map("method" -> request.method.methodId.value, "service" -> request.method.service.value).asJson
    val str = tree.mapObject(_.add("body", out)).noSpaces
    println(s"Request serialized: $str")
    str
  }

  override def encodeResponse(response: MuxResponse[_]): String = {
    val out = response.body.asJson
    val tree = Map("method" -> response.method.methodId.value, "service" -> response.method.service.value).asJson
    val str = tree.mapObject(_.add("body", out)).noSpaces
    println(s"Response serialized: $str")
    str
  }

}

class DirectMarshallerImpl() extends TransportMarshallers[InContext[String, Unit], InContext[GreeterServiceWrapped.GreeterServiceInput, Unit], GreeterServiceWrapped.GreeterServiceOutput, String] {

  import io.circe.syntax._
  import io.circe.parser._

  override def decodeRequest(requestWire: InContext[String, Unit]): InContext[GreeterServiceWrapped.GreeterServiceInput, Unit] = {
    InContext(parse(requestWire.value).flatMap(_.as[GreeterServiceWrapped.GreeterServiceInput]).right.get, requestWire.context)
  }

  override def encodeRequest(request: InContext[GreeterServiceWrapped.GreeterServiceInput, Unit]): InContext[String, Unit] = {
    InContext(request.value.asJson.noSpaces, request.context)
  }

  override def decodeResponse(responseWire: String): GreeterServiceWrapped.GreeterServiceOutput = {
    parse(responseWire).flatMap(_.as[GreeterServiceWrapped.GreeterServiceOutput]).right.get
  }

  override def encodeResponse(response: GreeterServiceWrapped.GreeterServiceOutput): String = {
    response.asJson.noSpaces
  }

}


object TestMul {
  // all the type annotations below are optional, infering works


    class SingleServiceDemo[R[_] : ServiceResult] {
    println(s"SingleServiceDemo: Running demo with container ${implicitly[ServiceResult[R]]}")
    println()


    final val server = {
      val service = new AbstractGreeterServer.Impl[R, Unit]
      val serverDispatcher = GreeterServiceWrapped.server(service)

      val marshalling: TransportMarshallers[
        InContext[String, Unit]
        , InContext[GreeterServiceWrapped.GreeterServiceInput, Unit]
        , GreeterServiceWrapped.GreeterServiceOutput
        , String
        ] = new DirectMarshallerImpl()
      val r = new ServerReceiver(serverDispatcher, marshalling)
      println("Testing direct RPC call...")
      val request = marshalling.encodeRequest(InContext(GreeterServiceWrapped.GreetInput("John", "Doe"), ()))
      println(s"RPC call performed: ${r.receive(request)}")
      r
    }


    val network = new NetworkSimulator(server, (p: String) => InContext(p, ()))

    final val clientDispatcher = {
      val marshalling: TransportMarshallers[
        String
        , GreeterServiceWrapped.GreeterServiceInput
        , GreeterServiceWrapped.GreeterServiceOutput
        , String
        ] = ???
      new ClientDispatcher(network, marshalling)
    }
    final val greeterClient = GreeterServiceWrapped.client(clientDispatcher)
    println()
    println("Testing client RPC calls...")
    println(greeterClient.greet("Best", "Client"))
    println()
  }

  class MultiplexingDemo[R[_] : ServiceResult] {
    final val c = implicitly[ServiceResult[R]]
    println(s"MultiplexingDemo: Running demo with container ${implicitly[ServiceResult[R]]}")
    println()


    final val serverMuxer = {
      val greeterService = new AbstractGreeterServer.Impl[R, DummyContext]
      val calculatorService = new AbstractCalculatorServer.Impl[R, DummyContext]
      val greeterDispatcher = GreeterServiceWrapped.serverUnsafe(greeterService)
      val calculatorDispatcher = CalculatorServiceWrapped.serverUnsafe(calculatorService)
      val dispatchers = List(greeterDispatcher, calculatorDispatcher)
      new ServerMultiplexor(dispatchers)
    }

    final val codecs = List(GreeterServiceWrapped, CalculatorServiceWrapped)

    final val server = {
      val serverMarshalling: TransportMarshallers[
        InContext[String, DummyContext]
        , InContext[MuxRequest[_], DummyContext]
        , MuxResponse[_]
        , String] = ???
      //new SimpleMarshallerImpl(OpinionatedMuxedCodec(codecs))
      val out = new ServerReceiver(serverMuxer, serverMarshalling)
      println("Testing direct RPC call...")
      val request = serverMarshalling.encodeRequest(InContext(MuxRequest(GreeterServiceWrapped.GreetInput("John", "Doe"), Method(GreeterServiceWrapped.serviceId, MethodId("greet"))),  DummyContext("127.0.0.1")))
      println(s"RPC call performed: ${out.receive(request)}")
      out
    }



    val network = new NetworkSimulator(server, (p: String) => InContext(p, DummyContext("127.0.0.1")))



    final val clientDispatcher = {
      val clientMarshalling: TransportMarshallers[
        String
        , MuxRequest[_]
        , MuxResponse[_]
        , String] = new SimpleMarshallerImpl(OpinionatedMuxedCodec(codecs))
      new ClientDispatcher(network, clientMarshalling)
    }
    final val greeterClient = GreeterServiceWrapped.clientUnsafe(clientDispatcher)
    final val calculatorClient = CalculatorServiceWrapped.clientUnsafe(clientDispatcher)

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

    final val serverMuxer = {
      val greeterService = new AbstractGreeterServer.Impl[R, DummyContext]
      val calculatorService = new AbstractCalculatorServer.Impl[R, DummyContext]
      val greeterDispatcher = GreeterServiceWrapped.serverUnsafe(greeterService)
      val calculatorDispatcher = CalculatorServiceWrapped.serverUnsafe(calculatorService)
      val dispatchers = List(greeterDispatcher, calculatorDispatcher)
      new ServerMultiplexor(dispatchers)
    }

    final val codecs = List(GreeterServiceWrapped, CalculatorServiceWrapped)

    final val server = {
      val serverMarshalling: TransportMarshallers[
        InContext[String, DummyContext]
        , InContext[MuxRequest[_], DummyContext]
        , MuxResponse[_]
        , String] = ???
      //new SimpleMarshallerImpl(OpinionatedMuxedCodec(codecs))
      new ServerReceiver(serverMuxer, serverMarshalling)
    }

    val transport: Transport[String, R[String]] = {
      val network = new NetworkSimulator(server, (p: String) => InContext(p, DummyContext("127.0.0.1")))

      val wrapperConverter = new TransformingNetworkSimulator[String, String, R, RT](network)
      val wrapperRestorer = new TransformingNetworkSimulator[String, String, RT, R](wrapperConverter)
      wrapperRestorer
    }

    final val clientDispatcher = {
      val clientMarshalling: TransportMarshallers[
        String
        , MuxRequest[_]
        , MuxResponse[_]
        , String] = new SimpleMarshallerImpl(OpinionatedMuxedCodec(codecs))
      new ClientDispatcher(transport, clientMarshalling)
    }
    final val greeterClient = GreeterServiceWrapped.clientUnsafe(clientDispatcher)
    final val calculatorClient = CalculatorServiceWrapped.clientUnsafe(clientDispatcher)

    println()
    println("Testing client RPC calls...")
    println(greeterClient.greet("Best", "Client"))
    println(calculatorClient.sum(1, 2))
    println()
  }

//  private def testSimple(): Unit = {
//    println()
//    println("testSimple...")
//    println(Try({
//      val demo = new SingleServiceDemo[Try]()
//      val result = demo.greeterClient.greet("John", "Doe")
//      result
//    }))
//
//    println(Try({
//      import ExecutionContext.Implicits._
//      val demo = new SingleServiceDemo[Future]()
//      val result = demo.greeterClient.greet("John", "Doe")
//      Thread.sleep(100)
//      result
//    }))
//
//    println(Try({
//      val demo = new SingleServiceDemo[Option]()
//      demo.greeterClient.greet("John", "Doe")
//    }))
//  }

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
