package com.github.pshirshov.izumi.r2.idealingua.experiments

import com.github.pshirshov.izumi.r2.idealingua.experiments.generated._
import com.github.pshirshov.izumi.r2.idealingua.experiments.impls._
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.{TransportMarshallers, _}
import io.circe._
import io.circe.parser._
import io.circe.syntax._

import scala.language.{higherKinds, implicitConversions}
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


object Test {

  import com.github.pshirshov.izumi.r2.idealingua.experiments.generated.GreeterServiceWrapped._

  //  class FailingMarshallers extends GreeterServiceWrapped.GreeterServiceStringMarshaller {
  //    override val requestUnmarshaller: Unmarshaller[String, GreeterServiceInput] = (v: String) => ???
  //    override val requestMarshaller: Marshaller[GreeterServiceInput, String] = (v: GreeterServiceInput) => ???
  //    override val responseMarshaller: Marshaller[GreeterServiceOutput, String] = (v: GreeterServiceOutput) => ???
  //    override val responseUnmarshaller: Unmarshaller[String, GreeterServiceOutput] = (v: String) => ???
  //  }

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

    val greeterService = new AbstractGreeterServer.Impl[R]
    val calculatorService = new AbstractCalculatorServer.Impl[R]
    val greeterDispatcher = new GreeterServiceWrapped.GreeterServiceDispatcherUnpacking.Impl(greeterService)
    //val calculatorDispatcher = new CalculatorServiceWrapped.CalculatorServiceDispatcherUnpacking.Impl(calculatorService)

    val list: List[UnsafeDispatcher[_, _, R]] = List(greeterDispatcher) //, calculatorDispatcher)
    val multiplexor = new ServerMultiplexor[R](list)


    val marshalling = new MImpl()
    val server = new ServerReceiver(multiplexor, marshalling)

    val request = marshalling.encodeRequest(Muxed(GreeterServiceWrapped.GreetInput("John", "Doe"), GreeterServiceWrapped.serviceId))
    println(s"RPC call performed: ${server.receive(request)}")

    //    val m: M = new TransportMarshallers[Json, AnyRef, Json, AnyRef] {
    //      override val requestUnmarshaller: FullUnmarshaller[Json, AnyRef] = new FullUnmarshaller[Json, AnyRef] {
    //        override def decodeUnsafe(v: Json): Option[AnyRef] = lis
    //
    //        override def decode(v: Json): AnyRef = ???
    //      }
    //      override val requestMarshaller: FullMarshaller[AnyRef, Json] = ???
    //      override val responseMarshaller: FullMarshaller[AnyRef, Json] = ???
    //      override val responseUnmarshaller: FullUnmarshaller[Json, AnyRef] = ???
    //    }
    //
    //
    //    val server = new ServerReceiver(multiplexor, marshalling)
    //
    //    val appTransport = new TrivialAppTransport(server)
    //
    //    val clientDispatcher = new ClientDispatcher(appTransport, marshalling)
    //    val client = new GreeterServiceWrapped.GreeterServiceDispatcherPacking.Impl(new GreeterServiceWrapped.GreeterServiceSafeToUnsafeBridge(clientDispatcher))
  }


  //  private def testSimple(marshalling: M): Unit = {
  //    println()
  //    println("testSimple...")
  //    println(Try({
  //      val demo = new SimpleDemo[Try](marshalling)
  //      val result = demo.client.greet("John", "Doe")
  //      result
  //    }))
  //
  //    println(Try({
  //      import ExecutionContext.Implicits._
  //      val demo = new SimpleDemo[Future](marshalling)
  //      val result = demo.client.greet("John", "Doe")
  //      Thread.sleep(100)
  //      result
  //    }))
  //
  //    println(Try({
  //      val demo = new SimpleDemo[Option](marshalling)
  //      demo.client.greet("John", "Doe")
  //    }))
  //  }
  //
  //
  //
  def main(args: Array[String]): Unit = {

    new SimpleDemo[Option]
  }
}
