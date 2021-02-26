package overflowdb.codegen

import Helpers._

object JsonToScalaDsl extends App {
  val schema = new Schema("testschema.json")
//  nodeProperties()
//  edgeProperties()
//  edgeTypes()
//    nodeBaseTypes()
  nodeTypes()

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

  def edgeTypes() = {
    p("// edge types")
    schema.edgeTypes.foreach { edge =>
      val addPropertiesMaybe = {
        if (edge.keys.isEmpty) ""
        else {
          val properties = edge.keys.map(Helpers.camelCase).mkString(", ")
          s".addProperties($properties)"
        }
      }

      p(
        s"""val ${Helpers.camelCase(edge.name)} = builder.addEdgeType(
           |  name = "${edge.name}",
           |  comment = "${escape(edge.comment)}",
           |  protoId = ${edge.id}
           |)$addPropertiesMaybe
           |""".stripMargin
      )
    }
  }

  def nodeBaseTypes() = {
    p("// node base types")
    schema.nodeBaseTraits.foreach { nodeBaseType =>
      val addPropertiesMaybe = {
        if (nodeBaseType.hasKeys.isEmpty) ""
        else {
          val properties = nodeBaseType.hasKeys.map(Helpers.camelCase).mkString(", ")
          s".addProperties($properties)"
        }
      }

      p(
        s"""val ${Helpers.camelCase(nodeBaseType.name)} = builder.addNodeBaseType(
           |  name = "${nodeBaseType.name}",
           |  comment = "${escape(nodeBaseType.comment)}"
           |)$addPropertiesMaybe
           |""".stripMargin
      )
    }
  }

  def nodeTypes() = {
    p("// node types")
    schema.nodeTypes.foreach { nodeType =>
      val addPropertiesMaybe = {
        if (nodeType.keys.isEmpty) ""
        else {
          val properties = nodeType.keys.map(Helpers.camelCase).mkString(", ")
          s".addProperties($properties)"
        }
      }

      val extendsMaybe = {
        if (nodeType.is.getOrElse(Nil).isEmpty) ""
        else {
          val extendz = nodeType.is.get.map(Helpers.camelCase).mkString(", ")
          s".extendz($extendz)"
        }
      }

      p(
        s"""val ${Helpers.camelCase(nodeType.name)} = builder.addNodeType(
           |  name = "${nodeType.name}",
           |  comment = "${escape(nodeType.comment)}",
           |  protoId = ${nodeType.id}
           |)$addPropertiesMaybe
           |$extendsMaybe
           |""".stripMargin
      )
    }
  }

  def p(s: String): Unit = {
    println(s)
  }

  def escape(s: String): String =
    s.replace("\"", "\\\"")

  def escape(s: Option[String]): String =
    s.map(escape).getOrElse("")
}
