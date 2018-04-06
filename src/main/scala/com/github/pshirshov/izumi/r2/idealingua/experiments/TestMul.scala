package com.github.pshirshov.izumi.r2.idealingua.experiments

import com.github.pshirshov.izumi.r2.idealingua.experiments.generated._
import com.github.pshirshov.izumi.r2.idealingua.experiments.impls._
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.{TransportMarshallers, _}
import io.circe._
import io.circe.parser._
import io.circe.syntax._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}
import scala.util.Try

//--------------------------------------------------------------------------
// setup context and use

class NetworkSimulator[I, O, R[_]](server: Receiver[I, O, R]) extends Transport[I, R[O]] {
  def send(v: I): R[O] = server.receive(v)
}

class PseudoNetwork[I, O, R[_], RT[_]](transport: Transport[I, R[O]])(implicit converter: ServiceResultTransformer[R, RT]) extends Transport[I, RT[O]] {
  def send(v: I): RT[O] = {
    val sent = transport.send(v)
    //println(s"on wire: $v")
    converter.transform(sent)
  }
}


object Test {

  import com.github.pshirshov.izumi.r2.idealingua.experiments.generated.GreeterServiceWrapped._

  type M = TransportMarshallers[String, Muxed, String, Muxed]

  class MImpl extends M {
    implicit val encodePolymorphic: Encoder[Muxed] = Encoder.instance { c =>
      c.v match {
        case v: GreeterServiceWrapped.GreeterServiceInput =>
          Map("Input" -> Map(GreeterServiceWrapped.serviceId.value -> v.asJson)).asJson
        case v: GreeterServiceWrapped.GreeterServiceOutput =>
          Map("Output" -> Map(GreeterServiceWrapped.serviceId.value -> v.asJson)).asJson
      }
    }

    implicit val decodePolymorphic: Decoder[Muxed] = Decoder.instance(c => {
      val fname = c.keys.flatMap(_.headOption).toSeq.head
      val value = c.downField(fname)
      val sname = value.keys.flatMap(_.headOption).toSeq.head
      val svalue = value.downField(sname)

      fname match {
        case "Input" =>
          sname match {
            case GreeterServiceWrapped.serviceId.value =>
              svalue.as[GreeterServiceInput].map(v => Muxed(v, GreeterServiceWrapped.serviceId))
          }
        case "Output" =>
          sname match {
            case GreeterServiceWrapped.serviceId.value =>
              svalue.as[GreeterServiceOutput].map(v => Muxed(v, GreeterServiceWrapped.serviceId))
          }
      }
    })

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

  class SimpleDemo[R[_] : ServiceResult] {
    final val c = implicitly[ServiceResult[R]]
    println(s"Running demo with container $c")
    println()

    val greeterService = new AbstractGreeterServer.Impl[R]
    val calculatorService = new AbstractCalculatorServer.Impl[R]
    val greeterDispatcher = new GreeterServiceWrapped.GreeterServiceDispatcherUnpacking.Impl(greeterService)
    //val calculatorDispatcher = new CalculatorServiceWrapped.CalculatorServiceDispatcherUnpacking.Impl(calculatorService)

    val list: List[UnsafeDispatcher[_, _, R]] = List(greeterDispatcher) //, calculatorDispatcher)
    val multiplexor = new ServerMultiplexor[R](list)


    val marshalling: TransportMarshallers[String, Muxed, String, Muxed] = new MImpl()
    val server = new ServerReceiver(multiplexor, marshalling)

    println("Testing direct RPC call...")
    val request = marshalling.encodeRequest(Muxed(GreeterServiceWrapped.GreetInput("John", "Doe"), GreeterServiceWrapped.serviceId))
    println(s"RPC call performed: ${server.receive(request)}")

    val network: NetworkSimulator[String, String, R] = new NetworkSimulator(server)

    val clientDispatcher: ClientDispatcher[String, Muxed, String, Muxed, R] = new ClientDispatcher(network, marshalling)
    val client = new GreeterServiceWrapped.GreeterServiceDispatcherPacking.Impl(new GreeterServiceWrapped.GreeterServiceSafeToUnsafeBridge(clientDispatcher))

    println()
    println("Testing client RPC call...")
    println(client.greet("Best", "Client"))
    println()
  }

  def main(args: Array[String]): Unit = {
    implicit val ec = ExecutionContext.Implicits.global
    new SimpleDemo[Option]
    new SimpleDemo[Try]
    new SimpleDemo[Future]
  }
}
