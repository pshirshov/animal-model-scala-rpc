package com.github.pshirshov.izumi.r2.idealingua.experiments

import com.github.pshirshov.izumi.r2.idealingua.experiments.generated._
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.{InContext, TransportMarshallers}

import scala.language.{higherKinds, implicitConversions}

class DirectMarshallerServerImpl extends TransportMarshallers[InContext[String, Unit], InContext[GreeterServiceWrapped.GreeterServiceInput, Unit], GreeterServiceWrapped.GreeterServiceOutput, String] {
  val just = new DirectMarshallerClientImpl()

  override def decodeRequest(requestWire: InContext[String, Unit]): InContext[GreeterServiceWrapped.GreeterServiceInput, Unit] = {
    InContext(just.decodeRequest(requestWire.value), requestWire.context)
  }

  override def encodeRequest(request: InContext[GreeterServiceWrapped.GreeterServiceInput, Unit]): InContext[String, Unit] = {
    InContext(just.encodeRequest(request.value), request.context)
  }

  override def encodeResponse(response: GreeterServiceWrapped.GreeterServiceOutput): String = just.encodeResponse(response)

  override def decodeResponse(responseWire: String): GreeterServiceWrapped.GreeterServiceOutput = just.decodeResponse(responseWire)

}

class DirectMarshallerClientImpl() extends TransportMarshallers[String, GreeterServiceWrapped.GreeterServiceInput, GreeterServiceWrapped.GreeterServiceOutput, String] {

  import io.circe.parser._
  import io.circe.syntax._

  override def decodeRequest(requestWire: String): GreeterServiceWrapped.GreeterServiceInput = {
    parse(requestWire).flatMap(_.as[GreeterServiceWrapped.GreeterServiceInput]).right.get
  }

  override def encodeRequest(request: GreeterServiceWrapped.GreeterServiceInput): String = {
    request.asJson.noSpaces
  }

  override def decodeResponse(responseWire: String): GreeterServiceWrapped.GreeterServiceOutput = {
    parse(responseWire).flatMap(_.as[GreeterServiceWrapped.GreeterServiceOutput]).right.get
  }

  override def encodeResponse(response: GreeterServiceWrapped.GreeterServiceOutput): String = {
    response.asJson.noSpaces
  }

}
