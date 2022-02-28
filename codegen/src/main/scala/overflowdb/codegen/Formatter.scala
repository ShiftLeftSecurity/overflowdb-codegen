package overflowdb.codegen

import better.files._
import java.nio.file.{Files, Path}
import org.scalafmt.interfaces.Scalafmt

object Formatter {
  val defaultScalafmtConfig = """
      |version=3.4.3
      |runner.dialect=scala213
      |align.preset=some
      |maxColumn=120
      |""".stripMargin

  def run(sourceFiles: Seq[File], scalafmtConfig: Option[File]): Unit = {
    val configFile: File = scalafmtConfig.getOrElse(
      Files
        .createTempFile("overflowdb-scalafmt", "conf")
        .toFile
        .toScala
        .write(defaultScalafmtConfig)
    )
    
    val scalafmt = Scalafmt.create(getClass.getClassLoader)
    val scalafmtSession = scalafmt.createSession(configFile.path)

    sourceFiles.foreach { file =>
      val originalSource = file.lines.mkString("\n")
      val formattedSource = scalafmtSession.format(file.path, originalSource)
      file.writeText(formattedSource)
    }
  }

}
