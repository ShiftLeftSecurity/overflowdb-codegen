name := "sbt-overflowdb"
organization := "io.shiftleft"

enablePlugins(SbtPlugin)

// TODO make part of regular codegen build, i.e. use dependsOn(Projects.codegen)
libraryDependencies += "io.shiftleft" %% "overflowdb-codegen" % "1.98+3-cf5221f8+20210913-1607"
