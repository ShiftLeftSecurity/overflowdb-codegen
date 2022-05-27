name := "overflowdb-schema-generator"

// cross scalaVersion is defined in project/Build.scala

libraryDependencies ++= Seq(
  "io.shiftleft" % "overflowdb-core" % Versions.overflowdb,
  "org.scalatest" %% "scalatest" % "3.2.12" % Test,
)
