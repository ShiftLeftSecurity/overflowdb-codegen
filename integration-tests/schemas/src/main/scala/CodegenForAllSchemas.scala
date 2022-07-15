import overflowdb.codegen.CodeGen

import java.io.File

object CodegenForAllSchemas {

  def main(args: Array[String]) = {
    val scalaVersion =
      if (isScala3) "scala-3"
      else "scala-2.13"

    val outputDir = new File(s"integration-tests/schemas/target/$scalaVersion/odb-codegen")

    Seq(
      new TestSchema01,
      new TestSchema02,
      new TestSchema03a,
      new TestSchema03b,
      new TestSchema03c,
      new TestSchema04,
      new TestSchema05,
      new TestSchema06,
    ).foreach { schema =>
      new CodeGen(schema.instance).disableScalafmt.run(outputDir)
    }
  }

  lazy val isScala3: Boolean =
    classpathUrls(getClass.getClassLoader)
      .exists(_.toString.contains("scala3-library_3"))

  def classpathUrls(cl: ClassLoader): Array[java.net.URL] = cl match {
    case u: java.net.URLClassLoader => u.getURLs() ++ classpathUrls(cl.getParent)
    case _ => Array.empty
  }
}
