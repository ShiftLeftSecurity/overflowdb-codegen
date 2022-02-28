package overflowdb.codegen

import better.files.File.temporaryFile
import better.files.{Dispose, FileExtensions}

import java.io.File
import java.nio.file.{Files, Path}
import org.scalafmt.interfaces.Scalafmt

object Formatter {
  val defaultScalafmtConfig = """
      |version=3.4.3
      |runner.dialect=scala213
      |align.preset=some
      |maxColumn=120
      |""".stripMargin

  def run(sourceFiles: Seq[java.io.File], scalafmtConfig: Option[File]): Unit = {
    val configFile = scalafmtConfig.getOrElse {
      val tmpFile = Files.createTempFile("overflowdb-scalafmt", "conf").toFile
      tmpFile.toScala.write(defaultScalafmtConfig)
      tmpFile
    }
    
    val scalafmt = Scalafmt.create(getClass.getClassLoader)
    val scalafmtSession = scalafmt.createSession(configFile.toPath)

    sourceFiles.map(_.toScala).foreach { file =>
      val originalSource = file.lines.mkString("\n")
      val formattedSource = scalafmtSession.format(file.path, originalSource)
      file.writeText(formattedSource)
    }
  }

}
