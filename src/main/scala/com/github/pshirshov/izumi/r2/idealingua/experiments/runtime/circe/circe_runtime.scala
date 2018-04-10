package com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.circe

import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime._
import io.circe._


trait CirceWrappedServiceDefinition {
  this: IdentifiableServiceDefinition =>
  def codecProvider: MuxingCodecProvider
}

case class ReqBody(value: AnyRef) extends AnyRef

case class ResBody(value: AnyRef) extends AnyRef

trait MuxedCodec {
  implicit val encodePolymorphicRequest: Encoder[ReqBody]
  implicit val decodePolymorphicRequest: Decoder[ReqBody]

  implicit val encodePolymorphicResponse: Encoder[ResBody]
  implicit val decodePolymorphicResponse: Decoder[ResBody]
}

trait MuxingCodecProvider {
  def requestEncoders: List[PartialFunction[ReqBody, Json]]

  def responseEncoders: List[PartialFunction[ResBody, Json]]

  def requestDecoders: List[PartialFunction[HCursor, Decoder.Result[ReqBody]]]

  def responseDecoders: List[PartialFunction[HCursor, Decoder.Result[ResBody]]]

}

class OpinionatedMuxedCodec
(
  provider: List[MuxingCodecProvider]
) extends MuxedCodec {

  private val requestEncoders = provider.flatMap(_.requestEncoders)
  private val responseEncoders = provider.flatMap(_.responseEncoders)
  private val requestDecoders = provider.flatMap(_.requestDecoders)
  private val responseDecoders = provider.flatMap(_.responseDecoders)

  override implicit val encodePolymorphicRequest: Encoder[ReqBody] = Encoder.instance { c =>
    requestEncoders.foldLeft(PartialFunction.empty[ReqBody, Json])(_ orElse _)(c)

  }
  override implicit val decodePolymorphicRequest: Decoder[ReqBody] = Decoder.instance(c => {
    requestDecoders.foldLeft(PartialFunction.empty[HCursor, Decoder.Result[ReqBody]])(_ orElse _)(c)
  })

  override implicit val encodePolymorphicResponse: Encoder[ResBody] = Encoder.instance { c =>
    responseEncoders.foldLeft(PartialFunction.empty[ResBody, Json])(_ orElse _)(c)

  }
  override implicit val decodePolymorphicResponse: Decoder[ResBody] = Decoder.instance(c => {
    responseDecoders.foldLeft(PartialFunction.empty[HCursor, Decoder.Result[ResBody]])(_ orElse _)(c)
  })

}

object OpinionatedMuxedCodec {
  def apply(definitions: List[CirceWrappedServiceDefinition]) = new OpinionatedMuxedCodec(definitions.map(_.codecProvider))
}
