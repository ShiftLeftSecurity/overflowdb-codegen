package overflowdb.codegen.sbt

import sbt._
import sbt.Keys._

object CodegenSbtPlugin extends AutoPlugin {

  object autoImport {
    val generateDomainClasses = taskKey[Seq[File]]("generate overflowdb domain classes for our schema")
    val classWithSchema = settingKey[String]("")
    val fieldName = settingKey[String]("")

    lazy val baseSettings: Seq[Def.Setting[_]] = Seq(
      // generateDomainClasses := Def.taskDyn {
      //   val classWithSchema_ = (generateDomainClasses/classWithSchema).value
      //   val fieldName_ = (generateDomainClasses/fieldName).value
      //   val outputDir = sourceManaged.value / "overflowdb-codegen"
      //   Def.task {
      //     // Codegen(classWithSchema_, fieldName_, outputDir)
      //     (Compile/runMain).toTask(s" overflowdb.codegen.Main ")
      //     // (Compile/runMain).toTask(s" overflowdb.codegen.Main --classWithSchema=$classWithSchema_ --field=$fieldName_ --out=$outputDir").value
      //     // Seq.empty
      //   }
      // }.value,
      generateDomainClasses := Def.taskDyn {
        val classWithSchema_ = (generateDomainClasses/classWithSchema).value
        val fieldName_ = (generateDomainClasses/fieldName).value
        val outputDir = sourceManaged.value / "overflowdb-codegen"
        // val runFoo = (Compile/runMain).toTask(s" overflowdb.codegen.Main ")
        // Def.task {
          // runMain.toTask(s" overflowdb.codegen.Main ").value
          // (Compile/runMain).toTask(s" overflowdb.codegen.Main --classWithSchema=$classWithSchema_ --field=$fieldName_ --out=$outputDir").value
        //   Seq.empty
        // }
        Codegen.apply2(classWithSchema_, fieldName_, outputDir)
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
  def apply2(classWithSchema: String, fieldName: String, outputDir: File): sbt.Def.Initialize[sbt.Task[Seq[File]]] =
    Def.task {
      // runMain.toTask(s" overflowdb.codegen.Main ").value

      // val classWithSchema_ = (generateDomainClasses/classWithSchema).value
      // val fieldName_ = (generateDomainClasses/fieldName).value
      // val outputDir = sourceManaged.value / "overflowdb-codegen"
      println(s"XXX3 $classWithSchema $fieldName $outputDir")
      (Compile/runMain).toTask(s" overflowdb.codegen.Main --classWithSchema=$classWithSchema --field=$fieldName --out=$outputDir").value
      // (Compile/runMain).toTask(s" overflowdb.codegen.Main --classWithSchema=$c").value
      Seq.empty
  }

}
