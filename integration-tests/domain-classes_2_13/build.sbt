name := "integration-test-domain-classes-2_13"

scalaVersion := Versions.scala_2_13

val generateDomainClasses = taskKey[Seq[File]]("generate overflowdb domain classes for all test schemas")

libraryDependencies ++= Seq(
  "io.shiftleft" %% "overflowdb-traversal" % Versions.overflowdb,
  "org.scalatest" %% "scalatest" % "3.2.12" % Test,
  "org.slf4j" % "slf4j-simple" % "1.7.36" % Test,
)

Compile/sourceGenerators += Projects.integrationTestSchemas_2_13 / generateDomainClasses
scalacOptions -= "-Xfatal-warnings"

publish/skip := true

