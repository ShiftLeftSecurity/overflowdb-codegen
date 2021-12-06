name := "integration-tests-root"

val generateDomainClasses = taskKey[Seq[File]]("generate overflowdb domain classes for all test schemas")

val schemas = project.in(file("schemas"))
  .dependsOn(Projects.codegen_2_13)
  .settings(
    scalaVersion := Versions.scala_3,
    crossScalaVersions := Seq(Versions.scala_2_13, Versions.scala_3),
    generateDomainClasses := Def.task {
      val outputRoot = target.value / "odb-codegen"
      FileUtils.deleteRecursively(outputRoot)
      (Compile/runMain).toTask(s" CodegenForAllSchemas integration-tests/schemas/target/odb-codegen").value
      FileUtils.listFilesRecursively(outputRoot)
    }.value,
    publish/skip := true)

val integrationTests = project.in(file("tests"))
  .settings(
    scalaVersion := Versions.scala_3,
    crossScalaVersions := Seq(Versions.scala_2_13, Versions.scala_3),
    libraryDependencies ++= Seq(
      "io.shiftleft" %% "overflowdb-traversal" % Versions.overflowdb,
      "org.scalatest" %% "scalatest" % "3.2.9" % Test,
      "org.slf4j" % "slf4j-simple" % "1.7.28" % Test,
    ),
    Compile/sourceGenerators += schemas / generateDomainClasses,
    scalacOptions -= "-Xfatal-warnings",
    publish/skip := true)

publish/skip := true

