import sbt._
import com.lucidchart.sbtcross.BaseProject

object Versions {
  val overflowdb = "1.69"
  val scala_2_12 = "2.12.15"
  val scala_2_13 = "2.13.7"
}

object Projects {
  /** scala cross version settings for codegen:
    * we need scala 2.12 for the sbt plugin and 2.13 for everything else */
  lazy val codegen = BaseProject(project.in(file("codegen"))).cross
  lazy val codegen_2_12 = codegen(scala_2_12)
  lazy val codegen_2_13 = codegen(scala_2_13)

  lazy val sbtPlugin = project.in(file("sbt-overflowdb"))
  lazy val integrationTests = project.in(file("integration-tests"))
}
