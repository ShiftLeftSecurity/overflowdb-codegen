package overflowdb.codegen

import Helpers._

object JsonToScalaDsl extends App {
  val schema = new Schema("testschema.json")

  p("// node properties")
  schema.nodeKeys.foreach { key =>
    p(
      s"""val ${Helpers.camelCase(key.name)} = builder.addNodeProperty(
         |  name = "${key.name}",
         |  valueType = "${getBaseType(key.valueType)}",
         |  cardinality = Cardinality.${key.cardinality.capitalize},
         |  comment = "${key.comment.getOrElse("")}"
         |)
         |""".stripMargin
    )
  }

  def p(s: String): Unit = {
    println(s)
  }
}
