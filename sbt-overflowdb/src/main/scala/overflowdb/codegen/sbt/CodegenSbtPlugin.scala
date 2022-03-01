package overflowdb.codegen.sbt

import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtConfig
import sbt._
import sbt.Keys._
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

  override def requires = sbt.plugins.JvmPlugin

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

      val schemaMd5File = target.value / "overflowdb-schema.md5"
      lazy val currentSchemaMd5 = FileUtils.md5(sourceDirectory.value, baseDirectory.value/"build.sbt")
      lazy val lastSchemaMd5: Option[String] =
        Try(IO.read(schemaMd5File)).toOption
      def persistLastSchemaMd5(value: String) =
        IO.write(schemaMd5File, value)

      if (outputDir.exists && lastSchemaMd5 == Some(currentSchemaMd5)) {
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
          persistLastSchemaMd5(currentSchemaMd5)
          FileUtils.listFilesRecursively(outputDir)
        }
      }
    }


}
