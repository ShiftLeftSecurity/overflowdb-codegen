name := "integration-tests-schemas"

val generateDomainClasses = taskKey[Seq[File]]("generate overflowdb domain classes for all test schemas")

// cross scalaVersion is defined in project/Build.scala

/** generated results are cached, based on the codegen source, test schemas and (this) build.sbt */
generateDomainClasses := Def.taskDyn {
  val outputRoot = target.value / "odb-codegen"
  val currentSchemaMd5 = FileUtils.md5(
    sourceDirectory.value,
    file("codegen/src/main"),
    file("integration-tests/schemas/build.sbt"))

  if (outputRoot.exists && lastSchemaMd5 == Some(currentSchemaMd5)) {
    Def.task {
      streams.value.log.info("no need to run codegen, inputs did not change")
      FileUtils.listFilesRecursively(outputRoot)
    }
  } else {
    Def.task {
      val invoked = (Compile/runMain).toTask(s" overflowdb.integrationtests.CodegenForAllSchemas").value
      lastSchemaMd5(currentSchemaMd5)
      FileUtils.listFilesRecursively(outputRoot)
    }
  }
}.value

publish/skip := true

lazy val schemaMd5File                 = file("target/schema-src.md5")
def lastSchemaMd5: Option[String]      = scala.util.Try(IO.read(schemaMd5File)).toOption
def lastSchemaMd5(value: String): Unit = IO.write(schemaMd5File, value)
