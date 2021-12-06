name := "overflowdb-codegen-root"

ThisBuild/organization := "io.shiftleft"

/** scala cross version settings for codegen:
  * we need scala 2.12 for the sbt plugin, 2.13 and 3 for everything else */
lazy val codegen_2_12 = Projects.codegen_2_12
lazy val codegen_2_13 = Projects.codegen_2_13
lazy val codegen_3 = Projects.codegen_3
lazy val integrationTestSchemas_3 = Projects.integrationTestSchemas_3
lazy val integrationTestDomainClasses_3 = Projects.integrationTestDomainClasses_3
lazy val integrationTests_2_13 = Projects.integrationTests_2_13
lazy val integrationTests_3 = Projects.integrationTests_3
lazy val sbtPlugin = Projects.sbtPlugin

ThisBuild/resolvers += Resolver.mavenLocal

ThisBuild/Compile/scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-feature",
  "-deprecation",
  "-language:implicitConversions",
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

