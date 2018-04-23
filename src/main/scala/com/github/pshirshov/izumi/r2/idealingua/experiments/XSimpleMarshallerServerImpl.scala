package com.github.pshirshov.izumi.r2.idealingua.experiments

import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.circe.MuxedCodec
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.{TransportMarshallers, _}

import scala.language.{higherKinds, implicitConversions}

class XSimpleMarshallerImpl(codec: MuxedCodec) {

  import codec._
  import io.circe.parser._
  import io.circe.syntax._

  def decodeRequest(requestWire: String)(implicit m: Method): ReqBody = {
    val parsed = parse(requestWire).flatMap(_.as[ReqBody])
    println(s"Request parsed: $requestWire -> $parsed")
    parsed.right.get
  }

  def decodeResponse(responseWire: String)(implicit m: Method): ResBody = {
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


