package com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.circe

import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime._
import io.circe.{ACursor, Decoder, Encoder, Json}

trait MuxedCodec {
  implicit val encodePolymorphic: Encoder[Muxed]

  implicit val decodePolymorphic: Decoder[Muxed]
}

trait MuxingCodecProvider {
  def encoders: List[PartialFunction[Muxed, Json]]
  def decoders: List[PartialFunction[OpinionatedMuxedCodec.DirectedPacket, Decoder.Result[Muxed]]]

}

class OpinionatedMuxedCodec
(
  provider: List[MuxingCodecProvider]
) extends MuxedCodec {

  import OpinionatedMuxedCodec._

  private val encoders = provider.flatMap(_.encoders)
  private val decoders = provider.flatMap(_.decoders)

  implicit val encodePolymorphic: Encoder[Muxed] = Encoder.instance { c =>
    encoders.foldLeft(PartialFunction.empty[Muxed, Json])(_ orElse _)(c)
  }

  implicit val decodePolymorphic: Decoder[Muxed] = Decoder.instance(c => {
    val direction = c.keys.flatMap(_.headOption).toSeq.head
    val undirectedPacket = c.downField(direction)
    val serviceName = undirectedPacket.keys.flatMap(_.headOption).toSeq.head
    val body = undirectedPacket.downField(serviceName)
    val packet = DirectedPacket(direction, ServiceId(serviceName), body)
    decoders.foldLeft(PartialFunction.empty[DirectedPacket, Decoder.Result[Muxed]])(_ orElse _)(packet)
  })
}

object OpinionatedMuxedCodec {
  def apply(definitions: List[CirceWrappedServiceDefinition]) = new OpinionatedMuxedCodec(definitions.map(_.codecProvider))

  case class DirectedPacket(direction: String, service: ServiceId, value: ACursor)

}
