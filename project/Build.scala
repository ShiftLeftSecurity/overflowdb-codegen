import sbt._
import com.lucidchart.sbtcross.BaseProject

object Versions {
  val overflowdb = "1.107+11-43298b39+20220201-2337"
  val scala_2_12 = "2.12.15"
  val scala_2_13 = "2.13.8"
  val scala_3 = "3.1.1"
}

object Projects {
  /** scala cross version settings for codegen:
    * we need scala 2.12 for the sbt plugin and 2.13 for everything else */
  lazy val codegen = BaseProject(project.in(file("codegen"))).cross
  lazy val codegen_2_12 = codegen(Versions.scala_2_12)
  lazy val codegen_2_13 = codegen(Versions.scala_2_13)
  lazy val codegen_3 = codegen(Versions.scala_3)

  lazy val integrationTestSchemas = BaseProject(project.in(file("integration-tests/schemas"))).cross.dependsOn(codegen)
  lazy val integrationTestSchemas_2_13 = integrationTestSchemas(Versions.scala_2_13)
  lazy val integrationTestSchemas_3 = integrationTestSchemas(Versions.scala_3)
  lazy val integrationTestDomainClasses_2_13 = project.in(file("integration-tests/domain-classes_2_13"))
  lazy val integrationTestDomainClasses_3 = project.in(file("integration-tests/domain-classes_3"))

  lazy val integrationTests = BaseProject(project.in(file("integration-tests/tests"))).cross
  lazy val integrationTests_2_13 = integrationTests(Versions.scala_2_13).dependsOn(integrationTestDomainClasses_2_13)
  lazy val integrationTests_3 = integrationTests(Versions.scala_3).dependsOn(integrationTestDomainClasses_3)

  val generateDomainClasses = taskKey[Seq[File]]("generate overflowdb domain classes for all test schemas")
  val sourceGenerators = sbt.Keys.sourceGenerators

  lazy val sbtPlugin = project.in(file("sbt-overflowdb"))
}
