package overflowdb.codegen.sbt

import sbt._
import sbt.Keys._
import scala.util.Try

object CodegenSbtPlugin extends AutoPlugin {

  object autoImport {
    val generateDomainClasses = taskKey[Seq[File]]("generate overflowdb domain classes for our schema")
    val classWithSchema = settingKey[String]("")
    val fieldName = settingKey[String]("")

    lazy val baseSettings: Seq[Def.Setting[_]] = Seq(
      generateDomainClasses := generateDomainClassesTask.value,
      generateDomainClasses/classWithSchema := "undefined",
      generateDomainClasses/fieldName := "undefined",
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

      val schemaMd5File = target.value / "overflowdb-schema.md5"
      lazy val currentSchemaMd5 = FileUtils.md5(sourceDirectory.value, baseDirectory.value/"build.sbt")
      lazy val lastSchemaMd5: Option[String] =
        Try(IO.read(schemaMd5File)).toOption
      def persistLastSchemaMd5(value: String) =
        IO.write(schemaMd5File, value)

      if (outputDir.exists && lastSchemaMd5 == Some(currentSchemaMd5)) {
        // inputs did not change, don't regenerate
        println("XXX0 before")
        Def.task {
          println("XXXX0 nothing changed, only list files")
          FileUtils.listFilesRecursively(outputDir)
        }
      } else {
        println("XXXX1 before")
        Def.task {
          println("XXXX1 need to regenerate")
          IO.delete(outputDir)
          println("XXXX1 after delete...")
          (Compile/runMain).toTask(
            s" overflowdb.codegen.Main --classWithSchema=$classWithSchema_ --field=$fieldName_ --out=$outputDir"
          ).value
          persistLastSchemaMd5(currentSchemaMd5)
          FileUtils.listFilesRecursively(outputDir)
        }
      }
    }


}
