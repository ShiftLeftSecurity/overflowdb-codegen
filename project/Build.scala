import sbt._

object Projects {
  val codegen = project.in(file("codegen"))
  val integrationTests = project.in(file("integration-tests"))
}

object Versions {
  val overflowdb = "0ac9cedc2765d799732a8cf24b67c78b1b4f41b0"
}
