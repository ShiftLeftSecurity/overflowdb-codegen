package overflowdb.codegen.sbt

import sbt._
import sbt.Keys._

object CodegenSbtPlugin extends AutoPlugin {

  object autoImport {
    val generateDomainClasses = taskKey[Seq[File]]("generate overflowdb domain classes for our schema")
    val classWithSchema = settingKey[String]("")
    val fieldName = settingKey[String]("")

    lazy val baseSettings000: Seq[Def.Setting[_]] = Seq(
      generateDomainClasses := {
        Codegen(sources.value, (generateDomainClasses/classWithSchema).value)
      },
      generateDomainClasses/classWithSchema := "initXX0"
    )
  }
  import autoImport._

  override def requires = sbt.plugins.JvmPlugin

  // This plugin is automatically enabled for projects which are JvmPlugin.
  override def trigger = allRequirements

  // a group of settings that are automatically added to projects.
  override val projectSettings = inConfig(Compile)(baseSettings000)
}

object Codegen {
  def apply(sources: Seq[File], classWithSchema: String): Seq[File] = {
    println("XXX0 $classWithSchema")
    // TODO codegen stuff!
    sources
  }
}
