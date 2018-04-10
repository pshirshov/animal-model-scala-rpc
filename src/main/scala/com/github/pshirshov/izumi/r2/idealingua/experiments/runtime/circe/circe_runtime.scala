package com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.circe

import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime._
import io.circe.{ACursor, Decoder, Encoder, Json}



trait CirceWrappedServiceDefinition {
  this: IdentifiableServiceDefinition =>
  def codecProvider: MuxingCodecProvider
}

case class Body(value: AnyRef) extends AnyRef

trait MuxedCodec {
  implicit val encodePolymorphic: Encoder[Body]

  implicit val decodePolymorphic: Decoder[Body]
}
trait MuxingCodecProvider {
  def encoders: List[PartialFunction[Body, Json]]
  def decoders: List[PartialFunction[OpinionatedMuxedCodec.DirectedPacket, Decoder.Result[Body]]]

}

class OpinionatedMuxedCodec
(
  provider: List[MuxingCodecProvider]
) extends MuxedCodec {

  import OpinionatedMuxedCodec._

  private val encoders = provider.flatMap(_.encoders)
  private val decoders = provider.flatMap(_.decoders)

  implicit val encodePolymorphic: Encoder[Body] = Encoder.instance { c =>
    encoders.foldLeft(PartialFunction.empty[Body, Json])(_ orElse _)(c)
  }

  implicit val decodePolymorphic: Decoder[Body] = Decoder.instance(c => {
    val direction = c.keys.flatMap(_.headOption).toSeq.head
    val undirectedPacket = c.downField(direction)
    val serviceName = undirectedPacket.keys.flatMap(_.headOption).toSeq.head
    val body = undirectedPacket.downField(serviceName)
    val packet = DirectedPacket(direction, ServiceId(serviceName), body)
    decoders.foldLeft(PartialFunction.empty[DirectedPacket, Decoder.Result[Body]])(_ orElse _)(packet)
  })
}

object OpinionatedMuxedCodec {
  def apply(definitions: List[CirceWrappedServiceDefinition]) = new OpinionatedMuxedCodec(definitions.map(_.codecProvider))

  case class DirectedPacket(direction: String, service: ServiceId, value: ACursor)

}
