package com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.http4s

import cats._
import cats.data.EitherT
import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.dsl._
import fs2.{Stream, StreamApp}
import fs2.StreamApp.ExitCode
import cats.implicits._
import com.github.pshirshov.izumi.r2.idealingua.experiments.{DummyContext, SimpleMarshallerClientImpl, XSimpleMarshallerImpl}
import com.github.pshirshov.izumi.r2.idealingua.experiments
import com.github.pshirshov.izumi.r2.idealingua.experiments.TestMul.MultiplexingDemo
import com.github.pshirshov.izumi.r2.idealingua.experiments.generated.{CalculatorServiceWrapped, GreeterServiceWrapped}
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime._
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.circe.OpinionatedMuxedCodec
import org.http4s.blaze.http.HttpClient
import org.http4s.client.blaze.Http1Client
import org.http4s.server.blaze._
import org.http4s.implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions

object Definitions {
  implicit object IOResult extends ServiceResult[IO] {
    override def map[A, B](r: IO[A])(f: A => B): IO[B] = implicitly[Monad[IO]].map(r)(f)

    override def flatMap[A, B](r: IO[A])(f: A => IO[B]): IO[B] = implicitly[Monad[IO]].flatMap(r)(f)

    override def pure[A](v: => A): IO[A] = implicitly[Monad[IO]].pure(v)
  }

  val demo = new MultiplexingDemo[IO]()
  val marsh = new XSimpleMarshallerImpl(OpinionatedMuxedCodec(demo.codecs))

  def requestDecoder(context: DummyContext)(implicit m: experiments.runtime.Method): EntityDecoder[IO, demo.serverMuxer.Input] = EntityDecoder.decodeBy(MediaRange.`*/*`) {
    message =>
      val decoded: IO[Either[DecodeFailure, InContext[MuxRequest[Any], DummyContext]]] = message.as[String].map {
        str =>
          Right {
            val r = marsh.decodeRequest(str)
            InContext(MuxRequest(r.value, m), context)
          }
      }

      DecodeResult(decoded)
  }

  implicit val respEncoder: EntityEncoder[IO, demo.serverMuxer.Output] = EntityEncoder.encodeBy(headers.`Content-Type`(MediaType.`application/json`)) {
    v =>
      IO {
        val s = Stream.emits(marsh.encodeResponse(v.body).getBytes).covary[IO]
        Entity.apply(s)
      }
  }

  val httpService: HttpService[IO] = HttpService[IO] {
    case request@POST -> Root / service / method => {
      implicit val methodId = experiments.runtime.Method(ServiceId(service), MethodId(method))
      val context = DummyContext("ip")

      implicit val dec = requestDecoder(context)

      request.decode[InContext[MuxRequest[Any], DummyContext]] {
        message =>
          demo.serverMuxer.dispatch(message)
            .flatMap(Ok(_))
      }
    }
  }

  val httpClient = Http1Client[IO]().unsafeRunSync
  final val clientDispatcher = new Dispatcher[MuxRequest[Any], MuxResponse[Any], IO] {
    override def dispatch(input: MuxRequest[Any]): Result[MuxResponse[Any]] = {
      val body: EntityBody[IO] = Stream.emits(marsh.encodeRequest(input.body).getBytes).covary[IO]


      val request = Uri.fromString("http://localhost:8080").map(_ / input.method.service.value / input.method.methodId.value) match {
        case Right(uri) =>
          uri
        case Left(e) =>
          ???
      }

      val req: Request[IO] = Request(org.http4s.Method.POST, request, body = body)

      httpClient.fetch(req) {
        resp =>
          resp.as[String].map(s => MuxResponse(marsh.decodeResponse(s)(input.method).value, input.method))
      }
    }
  }

  final val greeterClient = GreeterServiceWrapped.clientUnsafe(clientDispatcher)
  final val calculatorClient = CalculatorServiceWrapped.clientUnsafe(clientDispatcher)

}

object IRTHttp4sServer extends StreamApp[IO] {
  import Definitions._

  override def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] =
    BlazeBuilder[IO]
      .bindHttp(8080, "localhost")
      .mountService(httpService, "/")
      .serve
}

object IRTHttp4sClient {
  import Definitions._

  def main(args: Array[String]): Unit = {
    println(greeterClient.greet("John", "Smith").unsafeRunSync())
  }
}


