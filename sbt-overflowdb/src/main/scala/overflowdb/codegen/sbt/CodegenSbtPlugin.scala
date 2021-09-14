package overflowdb.codegen.sbt

import sbt._
import sbt.Keys._

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
      Def.task {
        (Compile/runMain).toTask(
          s" overflowdb.codegen.Main --classWithSchema=$classWithSchema_ --field=$fieldName_ --out=$outputDir"
        ).value
        FileUtils.listFilesRecursively(outputDir)
      }
    }

}
