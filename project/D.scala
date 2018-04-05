import sbt._

object D {
  val circeVersion = "0.9.1"

  val circe: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-core"
    , "io.circe" %% "circe-generic"
    , "io.circe" %% "circe-generic-extras"
    , "io.circe" %% "circe-parser"
    , "io.circe" %% "circe-java8"
  ).map(_ % circeVersion)

  val cats_version = "1.1.0"
  val cats_core = "org.typelevel" %% "cats-core" % cats_version
}
