name := "overflowdb-codegen"
organization := "io.shiftleft"
scalaVersion := "2.13.1"
crossScalaVersions := Seq("2.12.11", "2.13.1")
enablePlugins(GitVersioning)

libraryDependencies ++= Seq(
  "com.github.pathikrit" %% "better-files" % "3.8.0",
  "com.lihaoyi" %% "ujson" % "0.9.5",
  "org.scalatest" %% "scalatest" % "3.0.8" % Test
)

resolvers ++= Seq(
  Resolver.mavenLocal,
  Resolver.bintrayRepo("shiftleft", "maven"))

Compile/scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-feature",
  "-deprecation",
  "-language:implicitConversions",
  // "-language:existentials",
)

bintrayVcsUrl := Some("https://github.com/ShiftLeftSecurity/overflowdb-codgen")
licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

Global/cancelable := true
Global/onChangedBuildSource := ReloadOnSourceChanges

