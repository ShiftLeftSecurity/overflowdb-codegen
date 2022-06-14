name := "overflowdb-schema-generator"

// cross scalaVersion is defined in project/Build.scala

libraryDependencies ++= Seq(
  "io.shiftleft" % "overflowdb-core" % Versions.overflowdb,
  "io.shiftleft" %% "overflowdb-formats" % Versions.overflowdb,
  "org.slf4j" % "slf4j-simple" % "1.7.36",
  "org.scalatest" %% "scalatest" % "3.2.12" % Test,
)
