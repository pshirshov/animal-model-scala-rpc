package com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.http4s

import cats._
import cats.implicits._
import com.github.pshirshov.izumi.r2.idealingua.experiments
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.{ServerMultiplexor, _}
import fs2.Stream
import org.http4s._
import org.http4s.client.Client
import org.http4s.dsl._

import scala.language.{higherKinds, implicitConversions}

class RuntimeHttp4s[R[_] : ServiceResult : Monad] {
  type MaterializedStream = String
  type StreamDecoder = EntityDecoder[R, MaterializedStream]

  def httpService[Ctx]
  (
    muxer: ServerMultiplexor[R, Ctx]
    , contextProvider: Request[R] => Ctx
    , marshallers: ServerMarshallers[R]
    , dsl: Http4sDsl[R]
  )(implicit ed: StreamDecoder): HttpService[R] = {
    val TM = implicitly[ServiceResult[R]]

    def requestDecoder(context: Ctx, m: experiments.runtime.Method): EntityDecoder[R, muxer.Input] =
      EntityDecoder.decodeBy(MediaRange.`*/*`) {
        message =>
          val decoded: R[Either[DecodeFailure, InContext[MuxRequest[Any], Ctx]]] = message.as[String].map {
            str =>
              Right {
                val r = marshallers.decodeRequest(str, m)
                InContext(MuxRequest(r.value, m), context)
              }
          }

          DecodeResult(decoded)
      }


    def respEncoder(): EntityEncoder[R, muxer.Output] =
      EntityEncoder.encodeBy(headers.`Content-Type`(MediaType.`application/json`)) {
        v =>
          TM.pure {
            val s = Stream.emits(marshallers.encodeResponse(v.body).getBytes).covary[R]
            Entity.apply(s)
          }
      }


    import dsl._

    implicit val enc: EntityEncoder[R, muxer.Output] = respEncoder()

    HttpService[R] {
      case request@GET -> Root / service / method =>
        val methodId = experiments.runtime.Method(ServiceId(service), MethodId(method))
        val req = InContext(MuxRequest[Any](methodId, methodId), contextProvider(request))
        TM.flatMap(muxer.dispatch(req))(dsl.Ok(_))


      case request@POST -> Root / service / method =>
        val methodId = experiments.runtime.Method(ServiceId(service), MethodId(method))
        implicit val dec: EntityDecoder[R, muxer.Input] = requestDecoder(contextProvider(request), methodId)


        request.decode[InContext[MuxRequest[Any], Ctx]] {
          message =>
            TM.flatMap(muxer.dispatch(message))(dsl.Ok(_))
        }
    }
  }

  def httpClient
  (
    baseUri: Uri
    , marshallers: ClientMarshallers[R]
    , client: Client[R]
  )(implicit ed: StreamDecoder): Dispatcher[MuxRequest[Any], MuxResponse[Any], R] = {

    new Dispatcher[MuxRequest[Any], MuxResponse[Any], R] {
      override def dispatch(input: MuxRequest[Any]): Result[MuxResponse[Any]] = {
        val uri = baseUri / input.method.service.value / input.method.methodId.value

        val outBytes: Array[Byte] = marshallers.encodeRequest(input.body).getBytes
        val body: EntityBody[R] = Stream.emits(outBytes).covary[R]

        val req: Request[R] = if (true) {
          Request(org.http4s.Method.POST, uri, body = body)
        } else {
          Request(org.http4s.Method.GET, uri)
        }

        client.fetch(req) {
          resp =>
            resp.as[MaterializedStream].map {
              s =>
                val inValue: Any = marshallers.decodeResponse(s, input.method).value
                MuxResponse(inValue, input.method)
            }
        }
      }
    }

  }
}
