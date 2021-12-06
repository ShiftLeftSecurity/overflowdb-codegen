name := "integration-tests-schemas"

val generateDomainClasses = taskKey[Seq[File]]("generate overflowdb domain classes for all test schemas")

// cross scalaVersion is defined in project/Build.scala

generateDomainClasses := Def.task {
  println("XXXXXXXXXX0")
  val outputRoot = target.value / "odb-codegen"
  FileUtils.deleteRecursively(outputRoot)
  (Compile/runMain).toTask(s" CodegenForAllSchemas integration-tests/schemas/target/odb-codegen").value
  FileUtils.listFilesRecursively(outputRoot)
}.value

publish/skip := true
