name := "integration-tests"

val generateDomainClasses = taskKey[Seq[File]]("generate overflowdb domain classes for all test schemas")

scalaVersion := Versions.scala_3
crossScalaVersions := Seq(Versions.scala_2_13, Versions.scala_3)

libraryDependencies ++= Seq(
  "io.shiftleft" %% "overflowdb-traversal" % Versions.overflowdb,
  "org.scalatest" %% "scalatest" % "3.2.9" % Test,
  "org.slf4j" % "slf4j-simple" % "1.7.28" % Test,
)

Compile/sourceGenerators += schemas / generateDomainClasses
scalacOptions -= "-Xfatal-warnings"

publish/skip := true

