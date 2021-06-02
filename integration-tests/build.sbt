name := "integration-tests-root"

val generateDomainClasses = taskKey[Seq[File]]("generate overflowdb domain classes for all test schemas")

val schemas = project.in(file("schemas"))
  .dependsOn(Projects.codegen)
  .settings(Seq(
    generateDomainClasses := Def.taskDyn {
      val outputRoot = target.value / "odb-codegen"
      val currentSchemaMd5 = FileUtils.md5(sourceDirectory.value, file("integration-tests/build.sbt"), file("project/Build.scala"))
      if (outputRoot.exists && lastSchemaMd5 == Some(currentSchemaMd5)) {
        Def.task {
          FileUtils.listFilesRecursively(outputRoot)
        }
      } else {
        Def.task {
          FileUtils.deleteRecursively(outputRoot)
          (Compile/runMain).toTask(s" CodegenForAllSchemas integration-tests/schemas/target/odb-codegen").value
          lastSchemaMd5(currentSchemaMd5)
          FileUtils.listFilesRecursively(outputRoot)
        }
      }
    }.value,
    publish/skip := true
  ))

val integrationTests = project.in(file("tests"))
  .settings(Seq(
    libraryDependencies ++= Seq(
      "io.shiftleft" %% "overflowdb-traversal" % Versions.overflowdb,
      "org.scalatest" %% "scalatest" % "3.2.9" % Test),
    Compile/sourceGenerators += schemas / generateDomainClasses,
    publish/skip := true
  ))


val schemaMd5File = file("target/schema-src.md5")
def lastSchemaMd5: Option[String] = scala.util.Try(IO.read(schemaMd5File)).toOption
def lastSchemaMd5(value: String): Unit = IO.write(schemaMd5File, value)

publish/skip := true

