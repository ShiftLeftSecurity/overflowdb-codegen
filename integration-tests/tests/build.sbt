name := "integration-tests"

// val generateDomainClasses = taskKey[Seq[File]]("generate overflowdb domain classes for all test schemas")

// cross scalaVersion is defined in project/Build.scala

libraryDependencies ++= Seq(
  "io.shiftleft" %% "overflowdb-traversal" % Versions.overflowdb,
  "org.scalatest" %% "scalatest" % "3.2.11" % Test,
  "org.slf4j" % "slf4j-simple" % "1.7.35" % Test,
)

// Compile/sourceGenerators += Projects.integrationTestSchemas / generateDomainClasses
// Compile/sourceGenerators += schemas / generateDomainClasses
scalacOptions -= "-Xfatal-warnings"

publish/skip := true

