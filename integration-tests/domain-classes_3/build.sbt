name := "integration-test-domain-classes-3"

scalaVersion := Versions.scala_3

val generateDomainClasses = taskKey[Seq[File]]("generate overflowdb domain classes for all test schemas")

libraryDependencies ++= Seq(
  "io.shiftleft" %% "overflowdb-traversal" % Versions.overflowdb,
  "org.scalatest" %% "scalatest" % "3.2.11" % Test,
  "org.slf4j" % "slf4j-simple" % "1.7.35" % Test,
)

Compile/sourceGenerators += Projects.integrationTestSchemas_3 / generateDomainClasses
scalacOptions -= "-Xfatal-warnings"

publish/skip := true

