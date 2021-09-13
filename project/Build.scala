import sbt._

object Projects {
  val codegen = project.in(file("codegen"))
  val integrationTests = project.in(file("integration-tests"))
}

object Versions {
  val overflowdb = "1.62"
}
