import sbt._
import com.lucidchart.sbtcross.BaseProject

object Projects {
  /** scala cross version settings for codegen:
    * we need scala 2.12 for the sbt plugin and 2.13 for everything else */
  lazy val codegen = BaseProject(project.in(file("codegen"))).cross
  lazy val codegen_2_12 = codegen("2.12.15")
  lazy val codegen_2_13 = codegen("2.13.7")

  lazy val sbtPlugin = project.in(file("sbt-overflowdb"))
  lazy val integrationTests = project.in(file("integration-tests"))
}

object Versions {
  val overflowdb = "1.62"
}
