name := "overflowdb-codegen"
organization := "io.shiftleft"

scalaVersion := "2.13.6"
crossScalaVersions := Seq("2.12.13", "2.13.6")

enablePlugins(GitVersioning)

libraryDependencies ++= Seq(
  "io.shiftleft" % "overflowdb-core" % "1.38",
  "com.github.pathikrit" %% "better-files" % "3.8.0",
  "org.scalatest" %% "scalatest" % "3.2.7" % Test,
)

resolvers += Resolver.mavenLocal

Compile/scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-feature",
  "-deprecation",
  "-language:implicitConversions",
  "-Ywarn-unused",
  // "-language:existentials",
)

licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
scmInfo := Some(ScmInfo(url("https://github.com/ShiftLeftSecurity/overflowdb-codegen"),
                        "scm:git@github.com:ShiftLeftSecurity/overflowdb-codegen.git"))
homepage := Some(url("https://github.com/ShiftLeftSecurity/overflowdb-codegen/"))
developers := List(
  /* sonatype requires this to be non-empty */
  Developer(
    "mpollmeier",
    "Michael Pollmeier",
    "michael@michaelpollmeier.com",
    url("http://www.michaelpollmeier.com/")
  )
)
publishTo := sonatypePublishToBundle.value

Global/cancelable := true
Global/onChangedBuildSource := ReloadOnSourceChanges

