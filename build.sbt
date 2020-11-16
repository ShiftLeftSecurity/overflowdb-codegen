name := "overflowdb-codegen"
organization := "io.shiftleft"

/* used as sbt plugin, hence we need 2.12.
 * we could cross-compile to 2.13 but there are (minor) issues with how we use the collection api */
scalaVersion := "2.12.11"

enablePlugins(GitVersioning)

libraryDependencies ++= Seq(
  "com.github.pathikrit" %% "better-files" % "3.8.0",
  "com.lihaoyi" %% "ujson" % "0.9.5",
  "com.typesafe.play" %% "play-json" % "2.8.1",
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

