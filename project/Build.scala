import sbt._
import com.lucidchart.sbtcross.BaseProject

object Projects {
  lazy val codegen = BaseProject(project.in(file("codegen"))).cross
  lazy val codegen_2_12 = codegen("2.12.4")
  lazy val codegen_2_13 = codegen("2.13.6")
  lazy val integrationTests = project.in(file("integration-tests"))
  lazy val sbtPlugin = project.in(file("sbt-overflowdb"))
}

object Versions {
  val overflowdb = "1.62"
}
