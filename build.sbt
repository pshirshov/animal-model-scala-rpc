name := "animal-model-scala-rpc"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= D.circe
libraryDependencies += D.cats_core
libraryDependencies ++= D.http4s_all
scalacOptions += "-Ypartial-unification"

resolvers += Resolver.sonatypeRepo("releases")

addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.6" cross CrossVersion.binary)
