package overflowdb.codegen

import Helpers._

object JsonToScalaDsl extends App {
  val schema = new Schema("testschema.json")
  nodeProperties()
  edgeProperties()
//  edgeTypes()

  def nodeProperties() = {
    p("// node properties")
    schema.nodeKeys.foreach { key =>
      p(
        s"""val ${Helpers.camelCase(key.name)} = builder.addNodeProperty(
           |  name = "${key.name}",
           |  valueType = "${getBaseType(key.valueType)}",
           |  cardinality = Cardinality.${key.cardinality.capitalize},
           |  comment = "${escape(key.comment)}",
           |  protoId = ${key.id})
           |""".stripMargin
      )
    }
  }

  def edgeProperties() = {
    p("// edge properties")
    schema.edgeKeys.foreach { key =>
      p(
        s"""val ${Helpers.camelCase(key.name)} = builder.addEdgeProperty(
           |  name = "${key.name}",
           |  valueType = "${getBaseType(key.valueType)}",
           |  cardinality = Cardinality.${key.cardinality.capitalize},
           |  comment = "${escape(key.comment)}",
           |  protoId = ${key.id})
           |""".stripMargin
      )
    }
  }

//  def edgeTypes() = {
//    p("// edge types")
//    schema.edgeTypes.foreach { edge =>
//      p(
//        s"""val ${Helpers.camelCase(edge.name)} = builder.addEdgeType(
//           |  name = "${edge.name}",
//           |  valueType = "${getBaseType(edge.valueType)}",
//           |  cardinality = Cardinality.${edge.cardinality.capitalize},
//           |  protoId = ${edge.id},
//           |  comment = "${escape(edge.comment)}")
//           |""".stripMargin
//      )
//    }
//  }

  def p(s: String): Unit = {
    println(s)
  }

  def escape(s: String): String =
    s.replace("\"", "\\\"")

  def escape(s: Option[String]): String =
    s.map(escape).getOrElse("")
}
