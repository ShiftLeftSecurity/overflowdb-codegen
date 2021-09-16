name := "sbt-overflowdb"
organization := "io.shiftleft"
scalaVersion := "2.12.14"

enablePlugins(SbtPlugin)

// TODO make part of regular codegen build, i.e. use dependsOn(Projects.codegen)
libraryDependencies += "io.shiftleft" %% "overflowdb-codegen" % "1.98+11-9d08dab6+20210916-1632"

Global / onChangedBuildSource := ReloadOnSourceChanges
