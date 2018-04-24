package com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.http4s

import cats._
import cats.data.{Kleisli, OptionT}
import cats.effect._
import cats.implicits._
import com.github.pshirshov.izumi.r2.idealingua.experiments.generated.{CalculatorServiceWrapped, GreeterServiceWrapped}
import com.github.pshirshov.izumi.r2.idealingua.experiments.impls.{AbstractCalculatorServer, AbstractGreeterServer}
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime._
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.circe.OpinionatedMarshalers
import fs2.StreamApp.ExitCode
import fs2.{Stream, StreamApp}
import org.http4s._
import org.http4s.client.blaze.Http1Client
import org.http4s.dsl._
import org.http4s.dsl.io._
import org.http4s.server.blaze._
import cats._
import cats.effect._
import cats.implicits._
import cats.data._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.{higherKinds, implicitConversions, reflectiveCalls}


case class DummyContext(ip: String)

class Demo[R[_] : IRTServiceResult : Monad, Ctx] {

  import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime._

  final val serverMuxer = {
    val greeterService = new AbstractGreeterServer.Impl[R, Ctx]
    val calculatorService = new AbstractCalculatorServer.Impl[R, Ctx]
    val greeterDispatcher = GreeterServiceWrapped.serverUnsafe(greeterService)
    val calculatorDispatcher = CalculatorServiceWrapped.serverUnsafe(calculatorService)
    val dispatchers = List(greeterDispatcher, calculatorDispatcher)
    new ServerMultiplexor(dispatchers)
  }

  final val codecs = List(GreeterServiceWrapped, CalculatorServiceWrapped)
  final val marsh = OpinionatedMarshalers(codecs)

  val cm = marsh
  val sm = marsh

}

object Definitions {

  import RuntimeCats._

  val rt = new RuntimeHttp4s[IO]

  val demo = new Demo[IO, DummyContext]()

  val authUser: Kleisli[OptionT[IO, ?], Request[IO], DummyContext] =
    Kleisli {
      request: Request[IO] =>
        val context = DummyContext(request.remoteAddr.getOrElse("0.0.0.0"))

        OptionT.liftF(IO(context))
    }

  val ioService = rt.httpService(demo.serverMuxer, AuthMiddleware(authUser), demo.sm, io)

  val baseUri: Uri = Uri.fromString("http://localhost:8080").right.get

  val clientDispatcher = rt.httpClient(Http1Client[IO]().unsafeRunSync, demo.cm)(rt.requestBuilder(baseUri))

  final val greeterClient = GreeterServiceWrapped.clientUnsafe(clientDispatcher)
  final val calculatorClient = CalculatorServiceWrapped.clientUnsafe(clientDispatcher)
}


object IRTHttp4sServer extends StreamApp[IO] {

  import Definitions._

  override def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] = {
    new Thread(new Runnable {
      override def run(): Unit = {
        println("Client waiting")
        Thread.sleep(1500)
        println("Client started")
        IRTHttp4sClient.main(Array.empty)
        println("Client finished, terminating")
        System.exit(0)
      }
    }).start()

    BlazeBuilder[IO]
      .bindHttp(8080, "localhost")
      .mountService(ioService, "/")
      .serve
  }
}

object IRTHttp4sClient {

  import Definitions._

  def main(args: Array[String]): Unit = {
    println(greeterClient.greet("John", "Smith").unsafeRunSync())

    println(greeterClient.sayhi().unsafeRunSync())
  }
}


