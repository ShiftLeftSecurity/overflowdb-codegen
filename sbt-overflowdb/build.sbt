name := "sbt-overflowdb"
organization := "io.shiftleft"
scalaVersion := "2.12.14"

enablePlugins(SbtPlugin)

// TODO make part of regular codegen build, i.e. use dependsOn(Projects.codegen)
libraryDependencies += "io.shiftleft" %% "overflowdb-codegen" % "1.98+6-2dea5c5f+20210914-1008"

Global / onChangedBuildSource := ReloadOnSourceChanges
