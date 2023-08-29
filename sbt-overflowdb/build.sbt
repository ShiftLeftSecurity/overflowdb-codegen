name := "sbt-overflowdb"

scalaVersion := "2.12.14"
dependsOn(Projects.codegen_2_12)

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.1")

enablePlugins(SbtPlugin)
