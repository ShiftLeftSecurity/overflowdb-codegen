name := "integration-test-domain-classes-3"

scalaVersion := Versions.scala_3

val generateDomainClasses = taskKey[Seq[File]]("generate overflowdb domain classes for all test schemas")

libraryDependencies ++= Seq(
  "io.shiftleft" %% "overflowdb-traversal" % Versions.overflowdb,
  "org.scalatest" %% "scalatest" % "3.2.12" % Test,
  "org.slf4j" % "slf4j-simple" % "2.0.12" % Test,
)

Compile/sourceGenerators += Projects.integrationTestSchemas_3 / generateDomainClasses
scalacOptions -= "-Xfatal-warnings"

publish/skip := true

