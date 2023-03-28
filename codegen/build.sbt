name := "overflowdb-codegen"

// cross scalaVersion is defined in project/Build.scala

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "4.1.0",
  "com.github.pathikrit" %% "better-files" % "3.9.2",
  ("org.scalameta" %% "scalafmt-dynamic" % "3.7.3").cross(CrossVersion.for3Use2_13),
  "org.scalatest" %% "scalatest" % "3.2.15" % Test,
)
