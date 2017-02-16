name := "mrd-parser"

description := "Typesafe parser for the data encoded on machine readable identity documents"

version := "1.0"

scalaVersion := "2.12.1"

scalaOrganization := "org.typelevel"

scalacOptions ++= Seq(
  "-Ypartial-unification",
  "-Yliteral-types",
  "-Yinduction-heuristics",
  "-language:reflectiveCalls",
  "-feature"
)

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= Seq(
  "com.chuusai" %% "shapeless" % "2.3.2",
  "org.typelevel" %% "cats" % "0.9.0",
  "org.scodec" %% "scodec-bits" % "1.1.4",
  "org.scodec" %% "scodec-core" % "1.10.3",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test"
)