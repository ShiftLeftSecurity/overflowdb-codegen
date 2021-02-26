package overflowdb.codegen

import better.files._

object JsonToScalaDsl extends App {
  val schema = new Schema("testschema.json")
  val out = File("out")
  if (out.exists) out.delete()
  out.createFile()

  out.appendLine("// node properties")
  schema.nodeKeys.foreach { key =>
    out.appendLine(
      s"""val ${Helpers.camelCase(key.name)} = builder.addNodeProperty("${key.name}", "String", Cardinality.One,
        "Name of represented object, e.g., method name (e.g. \"run\")")"""
    )
  }

  out.lines.foreach(println)
}
