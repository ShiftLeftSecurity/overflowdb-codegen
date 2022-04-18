name := "overflowdb-codegen"

// cross scalaVersion is defined in project/Build.scala

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "4.0.1",
  ("com.github.pathikrit" %% "better-files" % "3.9.1").cross(CrossVersion.for3Use2_13),
  ("org.scalameta" %% "scalafmt-dynamic" % "3.5.2").cross(CrossVersion.for3Use2_13),
  "org.scalatest" %% "scalatest" % "3.2.11" % Test,
)
