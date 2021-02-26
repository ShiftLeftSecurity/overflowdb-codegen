package overflowdb.codegen

object JsonToScalaDsl extends App {
  val schema = new Schema("testschema.json")

  println("// node properties")
  schema.nodeKeys.foreach { key =>
    println(
      s"""val ${Helpers.camelCase(key.name)} = builder.addNodeProperty("${key.name}", "String", Cardinality.One,
        "Name of represented object, e.g., method name (e.g. \"run\")")"""
    )
  }
}
