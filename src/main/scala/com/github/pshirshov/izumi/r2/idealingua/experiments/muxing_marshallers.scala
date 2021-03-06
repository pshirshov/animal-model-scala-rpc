package com.github.pshirshov.izumi.r2.idealingua.experiments

import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.circe.MuxedCodec
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.{TransportMarshallers, _}

import scala.language.{higherKinds, implicitConversions}

class SimpleMarshallerServerImpl(codec: MuxedCodec) extends TransportMarshallers[InContext[String, DummyContext], InContext[MuxRequest[_], DummyContext], MuxResponse[_], String] {
  val just = new SimpleMarshallerClientImpl(codec)

  override def decodeRequest(requestWire: InContext[String, DummyContext]): InContext[MuxRequest[_], DummyContext] = {
    InContext(just.decodeRequest(requestWire.value), requestWire.context)
  }

  override def encodeRequest(request: InContext[MuxRequest[_], DummyContext]): InContext[String, DummyContext] = {
    InContext(just.encodeRequest(request.value), request.context)
  }

  override def decodeResponse(responseWire: String): MuxResponse[_] = just.decodeResponse(responseWire)

  override def encodeResponse(response: MuxResponse[_]): String = just.encodeResponse(response)
}

class SimpleMarshallerClientImpl(codec: MuxedCodec) extends TransportMarshallers[String, MuxRequest[_], MuxResponse[_], String] {


  import codec._
  import io.circe.parser._
  import io.circe.syntax._

  override def decodeRequest(requestWire: String): MuxRequest[_] = {
    val parsed = parse(requestWire).flatMap {
      v =>
        v.asObject match {
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
      v =>
        v.asObject match {
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
