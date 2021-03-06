import overflowdb.codegen.CodeGen

import java.io.File

object CodegenForAllSchemas extends App {
  val outputDir =
    args.headOption.map(new File(_)).getOrElse(throw new AssertionError("please pass outputDir as first parameter"))

  Seq(
    new TestSchema01,
    new TestSchema02,
    new TestSchema03a,
    new TestSchema03b,
    new TestSchema03c,
  ).foreach { schema =>
    new CodeGen(schema.instance).run(outputDir)
  }
}
