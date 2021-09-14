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
        val classWithSchema_ = (generateDomainClasses/classWithSchema).value
        val fieldName_ = (generateDomainClasses/fieldName).value
        val outputDir = sourceManaged.value / "overflowdb-codegen"
        Codegen.apply(classWithSchema_, fieldName_, outputDir)
      }.value,
      generateDomainClasses/classWithSchema := "undefined",
      generateDomainClasses/fieldName := "undefined",
    )
  }

  override def requires = sbt.plugins.JvmPlugin

  // This plugin is automatically enabled for projects which are JvmPlugin.
  override def trigger = allRequirements

  // a group of settings that are automatically added to projects.
  override val projectSettings = inConfig(Compile)(autoImport.baseSettings)
}

object Codegen {

  def apply(classWithSchema: String, fieldName: String, outputDir: File): Def.Initialize[Task[Seq[File]]] =
    Def.task {
      (Compile/runMain).toTask(s" overflowdb.codegen.Main --classWithSchema=$classWithSchema --field=$fieldName --out=$outputDir").value
      Seq.empty
  }

}
