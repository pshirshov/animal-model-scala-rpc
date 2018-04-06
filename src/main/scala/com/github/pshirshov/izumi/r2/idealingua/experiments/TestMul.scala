package com.github.pshirshov.izumi.r2.idealingua.experiments

import com.github.pshirshov.izumi.r2.idealingua.experiments
import com.github.pshirshov.izumi.r2.idealingua.experiments.generated._
import com.github.pshirshov.izumi.r2.idealingua.experiments.impls._
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.{TransportMarshallers, _}

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}
import scala.util.Try
import io.circe._



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

  type M = TransportMarshallers[Json, AnyRef, Json, AnyRef]
  class SimpleDemo[R[_] : ServiceResult] {

    val greeterService = new AbstractGreeterServer.Impl[R]
    val calculatorService = new AbstractCalculatorServer.Impl[R]
    val greeterDispatcher = new GreeterServiceWrapped.GreeterServiceDispatcherUnpacking.Impl(greeterService)
    //val calculatorDispatcher = new CalculatorServiceWrapped.CalculatorServiceDispatcherUnpacking.Impl(calculatorService)

    val list: List[UnsafeDispatcher[_, _, R]] = List(greeterDispatcher) //, calculatorDispatcher)
    val multiplexor = new ServerMultiplexor[R](list)

    val m: M = new TransportMarshallers[Json, AnyRef, Json, AnyRef] {
      override val requestUnmarshaller: FullUnmarshaller[Json, AnyRef] = new FullUnmarshaller[Json, AnyRef] {
        override def decodeUnsafe(v: Json): Option[AnyRef] = lis

        override def decode(v: Json): AnyRef = ???
      }
      override val requestMarshaller: FullMarshaller[AnyRef, Json] = ???
      override val responseMarshaller: FullMarshaller[AnyRef, Json] = ???
      override val responseUnmarshaller: FullUnmarshaller[Json, AnyRef] = ???
    }


    val server = new ServerReceiver(multiplexor, marshalling)

    val appTransport = new TrivialAppTransport(server)

    val clientDispatcher = new ClientDispatcher(appTransport, marshalling)
    val client = new GreeterServiceWrapped.GreeterServiceDispatcherPacking.Impl(new GreeterServiceWrapped.GreeterServiceSafeToUnsafeBridge(clientDispatcher))
  }




  private def testSimple(marshalling: M): Unit = {
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



  def main(args: Array[String]): Unit = {

    testSimple()
  }
}
