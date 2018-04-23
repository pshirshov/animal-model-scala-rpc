package com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.http4s

import cats._
import cats.effect._
import cats.implicits._
import com.github.pshirshov.izumi.r2.idealingua.experiments
import com.github.pshirshov.izumi.r2.idealingua.experiments.generated.{CalculatorServiceWrapped, GreeterServiceWrapped}
import com.github.pshirshov.izumi.r2.idealingua.experiments.impls.{AbstractCalculatorServer, AbstractGreeterServer}
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.circe.OpinionatedMuxedCodec
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.{ServerMultiplexor, _}
import fs2.StreamApp.ExitCode
import fs2.{Stream, StreamApp}
import org.http4s._
import org.http4s.client.blaze.Http1Client
import org.http4s.dsl._
import org.http4s.dsl.io._
import org.http4s.server.blaze._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.{higherKinds, implicitConversions}

trait ClientMarshallers[T[_]] {
  def encodeRequest(request: ReqBody): String

  def decodeResponse(responseWire: String, m: experiments.runtime.Method): ResBody
}

trait ServerMarshallers[T[_]] {
  def decodeRequest(requestWire: String, m: experiments.runtime.Method): ReqBody

  def encodeResponse(response: ResBody): String
}

case class DummyContext(ip: String)

class Demo[R[_] : ServiceResult : Monad, Ctx] {

  import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime._
  import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.circe.MuxedCodec


  final val serverMuxer = {
    val greeterService = new AbstractGreeterServer.Impl[R, Ctx]
    val calculatorService = new AbstractCalculatorServer.Impl[R, Ctx]
    val greeterDispatcher = GreeterServiceWrapped.serverUnsafe(greeterService)
    val calculatorDispatcher = CalculatorServiceWrapped.serverUnsafe(calculatorService)
    val dispatchers = List(greeterDispatcher, calculatorDispatcher)
    new ServerMultiplexor(dispatchers)
  }

  class XSimpleMarshallerImpl(codec: MuxedCodec) {

    import _root_.io.circe.parser._
    import _root_.io.circe.syntax._
    import codec._

    def decodeRequest(requestWire: String)(implicit m: Method): ReqBody = {
      val parsed = parse(requestWire).flatMap(_.as[ReqBody])
      println(s"Request parsed: $requestWire -> $parsed")
      parsed.right.get
    }

    def decodeResponse(responseWire: String, m: Method): ResBody = {
      implicit val x: Method = m
      val parsed = parse(responseWire).flatMap(_.as[ResBody])
      println(s"Response parsed: $responseWire -> $parsed")
      parsed.right.get
    }

    def encodeRequest(request: ReqBody): String = {
      request.asJson.noSpaces
    }

    def encodeResponse(response: ResBody): String = {
      response.asJson.noSpaces
    }
  }


  final val codecs = List(GreeterServiceWrapped, CalculatorServiceWrapped)
  final val marsh = new XSimpleMarshallerImpl(OpinionatedMuxedCodec(codecs))

  final val c = implicitly[ServiceResult[R]]

  val TM = implicitly[Monad[R]]

  val cm = new ClientMarshallers[R] {
    override def encodeRequest(request: ReqBody): String = marsh.encodeRequest(request)

    override def decodeResponse(responseWire: String, m: Method): ResBody = marsh.decodeResponse(responseWire, m)
  }


  val sm = new ServerMarshallers[R] {
    override def decodeRequest(requestWire: String, m: Method): ReqBody = marsh.decodeRequest(requestWire)(m)

    override def encodeResponse(response: ResBody): String = marsh.encodeResponse(response)
  }

}

object Definitions {

  import RuntimeCats._
  val rt = new RuntimeHttp4s[IO]

  val demo = new Demo[IO, DummyContext]()

  def ctx[T[_]](request: Request[T]): DummyContext = {
    DummyContext(request.remoteAddr.getOrElse("0.0.0.0"))
  }

  val ioService = rt.httpService(demo.serverMuxer, ctx[IO], demo.sm, io)

  val baseUri: Uri = Uri.fromString("http://localhost:8080").right.get

  val clientDispatcher = rt.httpClient(baseUri, demo.cm, Http1Client[IO]().unsafeRunSync)
  final val greeterClient = GreeterServiceWrapped.clientUnsafe(clientDispatcher)
  final val calculatorClient = CalculatorServiceWrapped.clientUnsafe(clientDispatcher)

}

object IRTHttp4sServer extends StreamApp[IO] {

  import Definitions._

  override def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] =
    BlazeBuilder[IO]
      .bindHttp(8080, "localhost")
      .mountService(ioService, "/")
      .serve
}

object IRTHttp4sClient {

  import Definitions._

  def main(args: Array[String]): Unit = {
    println(greeterClient.greet("John", "Smith").unsafeRunSync())
    println(greeterClient.sayhi().unsafeRunSync())
  }
}


