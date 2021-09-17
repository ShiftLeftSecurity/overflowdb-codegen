name := "overflowdb-codegen-root"

ThisBuild/organization := "io.shiftleft"

/** scala cross version settings
  * we need scala 2.12 for the sbt plugin and 2.13 for everything else */
val codegen = project.in(file("codegen"))
  .settings(
    name := "overflowdb-codegen",
    libraryDependencies ++= Seq(
      "io.shiftleft" % "overflowdb-core" % "1.62",
      "com.github.pathikrit" %% "better-files" % "3.8.0",
      "com.github.scopt" %% "scopt" % "4.0.1",
      "org.scalatest" %% "scalatest" % "3.2.9" % Test,
    )
  ).cross
val codegen_2_12 = codegen("2.12.4") // for sbtPlugin
val codegen_2_13 = codegen("2.13.6") // for everything else

val sbtPlugin = project.in(file("sbt-overflowdb"))
  .settings(
    name := "sbt-overflowdb",
    scalaVersion := "2.12.14")
  .dependsOn(codegen_2_12)
  .enablePlugins(SbtPlugin)

val integrationTests = project.in(file("integration-tests"))
  .dependsOn(codegen_2_13)
  // .settings(
  //   name := "sbt-overflowdb",
  //   scalaVersion := "2.13.6")



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

