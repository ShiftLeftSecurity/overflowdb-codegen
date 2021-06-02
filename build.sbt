name := "overflowdb-codegen-root"

ThisBuild/organization := "io.shiftleft"
ThisBuild/scalaVersion := "2.13.6"
ThisBuild/crossScalaVersions := Seq("2.12.13", "2.13.6")

val codegen = Projects.codegen
val integrationTests = Projects.integrationTests

ThisBuild/resolvers += Resolver.mavenLocal

enablePlugins(GitVersioning)

ThisBuild/Compile/scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-feature",
  "-deprecation",
  "-language:implicitConversions",
  "-Ywarn-unused",
  // "-language:existentials",
)

ThisBuild/licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild/scmInfo := Some(ScmInfo(url("https://github.com/ShiftLeftSecurity/overflowdb-codegen"),
                                      "scm:git@github.com:ShiftLeftSecurity/overflowdb-codegen.git"))
ThisBuild/homepage := Some(url("https://github.com/ShiftLeftSecurity/overflowdb-codegen/"))
ThisBuild/developers := List(
  Developer("mpollmeier", "Michael Pollmeier", "michael@michaelpollmeier.com", url("http://www.michaelpollmeier.com/"))
)
ThisBuild/publishTo := sonatypePublishToBundle.value
publish/skip := true

Global/cancelable := true
Global/onChangedBuildSource := ReloadOnSourceChanges

