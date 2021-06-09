name := "integration-tests-root"

val generateDomainClasses = taskKey[Seq[File]]("generate overflowdb domain classes for all test schemas")

val schemas = project.in(file("schemas"))
  .dependsOn(Projects.codegen)
  .settings(Seq(
    generateDomainClasses := Def.task {
      val outputRoot = target.value / "odb-codegen"
      FileUtils.deleteRecursively(outputRoot)
      (Compile/runMain).toTask(s" CodegenForAllSchemas integration-tests/schemas/target/odb-codegen").value
      FileUtils.listFilesRecursively(outputRoot)
    }.value,
    publish/skip := true
  ))

val integrationTests = project.in(file("tests"))
  .settings(Seq(
    libraryDependencies ++= Seq(
      "io.shiftleft" %% "overflowdb-traversal" % Versions.overflowdb,
      "org.scalatest" %% "scalatest" % "3.2.9" % Test,
    ),
    Compile/sourceGenerators += schemas / generateDomainClasses,
    publish/skip := true
  ))


publish/skip := true

