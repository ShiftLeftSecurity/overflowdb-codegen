name := "overflowdb-codegen"

// cross scalaVersion is defined in project/Build.scala

libraryDependencies ++= Seq(
  "io.shiftleft" % "overflowdb-core" % Versions.overflowdb,
  ("com.github.pathikrit" %% "better-files" % "3.8.0").cross(CrossVersion.for3Use2_13),
  "com.github.scopt" %% "scopt" % "4.0.1",
  "org.scalatest" %% "scalatest" % "3.2.9" % Test,
)
