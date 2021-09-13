package overflowdb.codegen.sbt

import sbt._
import sbt.Keys._

object CodegenSbtPlugin extends AutoPlugin {

  object autoImport {
    val generateDomainClasses = taskKey[Seq[File]]("generate overflowdb domain classes for our schema")
    val classWithSchema = settingKey[String]("")
    val fieldName = settingKey[String]("")

    lazy val baseSettings: Seq[Def.Setting[_]] = Seq(
      generateDomainClasses := Def.taskDyn {
        Def.task {
          val classWithSchema_ = (Compile/generateDomainClasses/classWithSchema).value
          val fieldName_ = (Compile/generateDomainClasses/fieldName).value
          val outputDir = sourceManaged.value / "overflowdb-codegen"
          Codegen(classWithSchema_, fieldName_, outputDir)
        }
      }.value,
      generateDomainClasses/classWithSchema := "undefined",
      generateDomainClasses/fieldName := "undefined",
    )
  }
  import autoImport._

  override def requires = sbt.plugins.JvmPlugin

  // This plugin is automatically enabled for projects which are JvmPlugin.
  override def trigger = allRequirements

  // a group of settings that are automatically added to projects.
  override val projectSettings = inConfig(Compile)(baseSettings)
}

object Codegen {
  def apply(classWithSchema: String, fieldName: String, outputDir: File): Seq[File] = {
    println(s"XXX0 $classWithSchema $fieldName $outputDir")
    Seq.empty
  }
}
