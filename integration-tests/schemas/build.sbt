name := "integration-tests-schemas"

val generateDomainClasses = taskKey[Seq[File]]("generate overflowdb domain classes for all test schemas")

scalaVersion := Versions.scala_3
crossScalaVersions := Seq(Versions.scala_2_13, Versions.scala_3)

dependsOn(Projects.codegen_2_13)

generateDomainClasses := Def.task {
  val outputRoot = target.value / "odb-codegen"
  FileUtils.deleteRecursively(outputRoot)
  (Compile/runMain).toTask(s" CodegenForAllSchemas integration-tests/schemas/target/odb-codegen").value
  FileUtils.listFilesRecursively(outputRoot)
}.value

publish/skip := true
