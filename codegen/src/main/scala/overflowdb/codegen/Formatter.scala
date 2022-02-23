package overflowdb.codegen

import better.files.File.usingTemporaryFile
import better.files._
import java.nio.file.Path
import org.scalafmt.interfaces.Scalafmt

object Formatter {
  val defaultScalafmtConfig = Map(
    "version" -> "3.4.3",
    "runner.dialect" -> "scala213",
    "align.preset" -> "some",
    "maxColumn" -> "120",
  )

  def apply(sourceFiles: Seq[java.io.File], scalafmtConfig: Map[String, String] = defaultScalafmtConfig): Unit = {
    val scalafmt = Scalafmt.create(getClass.getClassLoader)
      usingTemporaryFile("overflowdb-scalafmt", "conf") { configFile =>
        val scalafmtConfigString = scalafmtConfig.map { case (key, value) => s"$key = $value" }.mkString("\n")
        configFile.write(scalafmtConfigString)
        val scalafmtSession = scalafmt.createSession(configFile.path)

        sourceFiles.map(_.toScala).foreach { file =>
          val originalSource = file.lines.mkString("\n")
          val formattedSource = scalafmtSession.format(file.path, originalSource)
          file.writeText(formattedSource)
        }
      }

  }

}
