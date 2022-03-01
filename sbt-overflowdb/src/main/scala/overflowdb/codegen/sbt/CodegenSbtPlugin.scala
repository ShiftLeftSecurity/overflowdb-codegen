package overflowdb.codegen.sbt

import org.scalafmt.sbt.ScalafmtPlugin
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtConfig
import sbt.plugins.JvmPlugin
import sbt._
import sbt.Keys._
import sbt.plugins.DependencyTreeKeys.dependencyDot

import scala.util.Try

object CodegenSbtPlugin extends AutoPlugin {

  object autoImport {
    val generateDomainClasses = taskKey[Seq[File]]("generate overflowdb domain classes for our schema")
    val classWithSchema = settingKey[String]("class with schema field, e.g. `org.example.MyDomain$`")
    val fieldName = settingKey[String]("(static) field name for schema within the specified `classWithSchema` with schema field, e.g. `org.example.MyDomain$`")
    val disableFormatting = settingKey[Boolean]("disable scalafmt formatting")

    lazy val baseSettings: Seq[Def.Setting[_]] = Seq(
      generateDomainClasses := generateDomainClassesTask.value,
      generateDomainClasses/classWithSchema := "undefined",
      generateDomainClasses/fieldName := "undefined",
      generateDomainClasses/disableFormatting := false,
    )
  }
  import autoImport._

  override def requires = JvmPlugin && ScalafmtPlugin

  // This plugin is automatically enabled for projects which are JvmPlugin.
  override def trigger = allRequirements

  // a group of settings that are automatically added to projects.
  override val projectSettings = inConfig(Compile)(autoImport.baseSettings)

  lazy val generateDomainClassesTask =
    Def.taskDyn {
      val classWithSchema_ = (generateDomainClasses/classWithSchema).value
      val fieldName_ = (generateDomainClasses/fieldName).value
      val outputDir = sourceManaged.value / "overflowdb-codegen"

      val disableFormattingParamMaybe =
        if ((generateDomainClasses/disableFormatting).value) "--noformat"
        else ""

      val scalafmtConfigFileMaybe = {
        val file = (generateDomainClasses/scalafmtConfig).value
        if (file.exists) s"--scalafmtConfig=$file"
        else ""
      }

      val schemaAndDependenciesHashFile = target.value / "overflowdb-schema-and-dependencies.md5"
      lazy val currentSchemaAndDependenciesHash =
        FileUtils.md5(sourceDirectory.value, baseDirectory.value/"build.sbt", dependencyDot.value)
      lazy val lastSchemaAndDependenciesHash: Option[String] =
        Try(IO.read(schemaAndDependenciesHashFile)).toOption

      if (outputDir.exists && lastSchemaAndDependenciesHash == Some(currentSchemaAndDependenciesHash)) {
        // inputs did not change, don't regenerate
        Def.task {
          FileUtils.listFilesRecursively(outputDir)
        }
      } else {
        Def.task {
          FileUtils.deleteRecursively(outputDir)
          (Compile/runMain).toTask(
            s" overflowdb.codegen.Main --classWithSchema=$classWithSchema_ --field=$fieldName_ --out=$outputDir $disableFormattingParamMaybe $scalafmtConfigFileMaybe"
          ).value
          IO.write(schemaAndDependenciesHashFile, currentSchemaAndDependenciesHash)
          FileUtils.listFilesRecursively(outputDir)
        }
      }
    }


}
