package com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.circe

import com.github.pshirshov.izumi.r2.idealingua.experiments
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime._
import io.circe._

import scala.language.higherKinds


trait CirceWrappedServiceDefinition {
  this: IdentifiableServiceDefinition =>
  def codecProvider: MuxingCodecProvider
}

trait ClientMarshallers {
  def encodeRequest(request: ReqBody): String

  def decodeResponse(responseWire: String, m: experiments.runtime.Method): Either[Error, ResBody]
}

trait ServerMarshallers {
  def decodeRequest(requestWire: String, m: experiments.runtime.Method): Either[Error, ReqBody]

  def encodeResponse(response: ResBody): String
}

class OpinionatedMarshalers(provider: List[MuxingCodecProvider]) extends ClientMarshallers with ServerMarshallers {

  import _root_.io.circe.parser._
  import _root_.io.circe.syntax._

  def decodeRequest(requestWire: String, m: Method): Either[Error, ReqBody] = {
    implicit val x: Method = m
    val parsed = parse(requestWire).flatMap(_.as[ReqBody])
    println(s"Request parsed: $requestWire -> $parsed")
    parsed
  }

  def decodeResponse(responseWire: String, m: Method): Either[Error, ResBody] = {
    implicit val x: Method = m
    val parsed = parse(responseWire).flatMap(_.as[ResBody])
    println(s"Response parsed: $responseWire -> $parsed")
    parsed
  }

  def encodeRequest(request: ReqBody): String = {
    request.asJson.noSpaces
  }

  def encodeResponse(response: ResBody): String = {
    response.asJson.noSpaces
  }

  private val requestEncoders = provider.flatMap(_.requestEncoders)
  private val responseEncoders = provider.flatMap(_.responseEncoders)
  private val requestDecoders = provider.flatMap(_.requestDecoders)
  private val responseDecoders = provider.flatMap(_.responseDecoders)

  private implicit val encodePolymorphicRequest: Encoder[ReqBody] = Encoder.instance { c =>
    requestEncoders.foldLeft(PartialFunction.empty[ReqBody, Json])(_ orElse _)(c)
  }

  private implicit val encodePolymorphicResponse: Encoder[ResBody] = Encoder.instance { c =>
    responseEncoders.foldLeft(PartialFunction.empty[ResBody, Json])(_ orElse _)(c)
  }

  private implicit def decodePolymorphicRequest(implicit method: Method): Decoder[ReqBody] = Decoder.instance(c => {
    requestDecoders.foldLeft(PartialFunction.empty[CursorForMethod, Decoder.Result[ReqBody]])(_ orElse _)(CursorForMethod(method, c))
  })

  private  implicit def decodePolymorphicResponse(implicit method: Method): Decoder[ResBody] = Decoder.instance(c => {
    responseDecoders.foldLeft(PartialFunction.empty[CursorForMethod, Decoder.Result[ResBody]])(_ orElse _)(CursorForMethod(method, c))
  })
}


object OpinionatedMarshalers {
  def apply(definitions: List[CirceWrappedServiceDefinition]) = new OpinionatedMarshalers(definitions.map(_.codecProvider))
}


case class CursorForMethod(methodId: Method, cursor: HCursor)

trait MuxingCodecProvider {
  def requestEncoders: List[PartialFunction[ReqBody, Json]]

  def responseEncoders: List[PartialFunction[ResBody, Json]]

  def requestDecoders: List[PartialFunction[CursorForMethod, Decoder.Result[ReqBody]]]

  def responseDecoders: List[PartialFunction[CursorForMethod, Decoder.Result[ResBody]]]

}
