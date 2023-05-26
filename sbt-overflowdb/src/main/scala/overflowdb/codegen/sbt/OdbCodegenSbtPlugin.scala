package overflowdb.codegen.sbt

import org.scalafmt.sbt.ScalafmtPlugin
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtConfig
import sbt.plugins.JvmPlugin
import sbt._
import sbt.Keys._

import scala.util.Try

object OdbCodegenSbtPlugin extends AutoPlugin {

  object autoImport {
    val generateDomainClasses = taskKey[Seq[File]]("generate overflowdb domain classes for our schema")
    val outputDir = settingKey[File]("target directory for the generated domain classes, e.g. `Projects.domainClasses/scalaSource`")
    val classWithSchema = settingKey[String]("class with schema field, e.g. `org.example.MyDomain$`")
    val fieldName = settingKey[String]("(static) field name for schema within the specified `classWithSchema` with schema field, e.g. `org.example.MyDomain$`")
    val disableFormatting = settingKey[Boolean]("disable scalafmt formatting")

    lazy val baseSettings: Seq[Def.Setting[_]] = Seq(
      generateDomainClasses := generateDomainClassesTask.value,
      generateDomainClasses/disableFormatting := false,
    )
  }
  import autoImport._

  override def requires = JvmPlugin && ScalafmtPlugin

  // This plugin needs to be enabled manually via `enablePlugins`
  override def trigger = noTrigger

  // a group of settings that are automatically added to projects.
  override val projectSettings = inConfig(Compile)(autoImport.baseSettings)

  lazy val generateDomainClassesTask =
    Def.taskDyn {
      val classWithSchemaValue = (generateDomainClasses/classWithSchema).value
      val fieldNameValue = (generateDomainClasses/fieldName).value
      val outputDirValue = (generateDomainClasses/outputDir).value
      assert(outputDirValue != null, "`generateDomainClasses/outputDir` is not defined, please configure it in your build definition, e.g. `generateDomainClasses/outputDir := (Projects.domainClasses/scalaSource).value`")

      val disableFormattingParamMaybe =
        if ((generateDomainClasses/disableFormatting).value) "--noformat"
        else ""

      val scalafmtConfigFileMaybe = {
        val file = (generateDomainClasses/scalafmtConfig).value
        if (file.exists) s"--scalafmtConfig=$file"
        else ""
      }

      val schemaAndDependenciesHashFile = target.value / "overflowdb-schema-and-dependencies.md5"
      val dependenciesFile = target.value / "dependenciesCP.txt"
      IO.write(dependenciesFile, dependencyClasspath.value.mkString(System.lineSeparator))
      lazy val currentSchemaAndDependenciesHash =
        FileUtils.md5(sourceDirectory.value, baseDirectory.value/"build.sbt", dependenciesFile)
      lazy val lastSchemaAndDependenciesHash: Option[String] =
        Try(IO.read(schemaAndDependenciesHashFile)).toOption

      if (outputDirValue.exists && lastSchemaAndDependenciesHash == Some(currentSchemaAndDependenciesHash)) {
        // inputs did not change, don't regenerate
        Def.task {
          FileUtils.listFilesRecursively(outputDirValue)
        }
      } else {
        Def.task {
          FileUtils.deleteRecursively(outputDirValue)
          (Compile/runMain).toTask(
            s" overflowdb.codegen.Main --classWithSchema=$classWithSchemaValue --field=$fieldNameValue --out=$outputDirValue $disableFormattingParamMaybe $scalafmtConfigFileMaybe"
          ).value
          IO.write(schemaAndDependenciesHashFile, currentSchemaAndDependenciesHash)
          FileUtils.listFilesRecursively(outputDirValue)
        }
      }
    }


}
