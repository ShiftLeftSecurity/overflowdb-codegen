package overflowdb.codegen

import Helpers._

object JsonToScalaDsl extends App {
  val schema = new Schema("base.json")
//  val schema = new Schema("enhancements-internal.json")
  //  val schema = new Schema("enhancements-internal-dbg.json")
//  nodeProperties()
//  edgeProperties()
//  edgeTypes()
//  nodeBaseTypes()
  nodeTypes()
//  constants()

  def nodeProperties() = {
    if (schema.nodeKeys.nonEmpty) p("// node properties")
    schema.nodeKeys.foreach { key =>
      p(
        s"""val ${camelCase(key.name)} = builder.addNodeProperty(
           |  name = "${key.name}",
           |  valueType = "${getBaseType(key.valueType)}",
           |  cardinality = Cardinality.${key.cardinality.capitalize},
           |  comment = "${escape(key.comment)}"
           |).protoId(${key.id})
           |""".stripMargin
      )
    }
  }

  def edgeProperties() = {
    if (schema.edgeKeys.nonEmpty) p("// edge properties")
    schema.edgeKeys.foreach { key =>
      p(
        s"""val ${camelCase(key.name)} = builder.addEdgeProperty(
           |  name = "${key.name}",
           |  valueType = "${getBaseType(key.valueType)}",
           |  cardinality = Cardinality.${key.cardinality.capitalize},
           |  comment = "${escape(key.comment)}"
           |).protoId(${key.id})
           |""".stripMargin
      )
    }
  }

  def edgeTypes() = {
    if (schema.edgeTypes.nonEmpty) p("// edge types")
    schema.edgeTypes.foreach { edge =>
      val addPropertiesMaybe = {
        if (edge.keys.isEmpty) ""
        else {
          val properties = edge.keys.map(camelCase).mkString(", ")
          s".addProperties($properties)"
        }
      }

      p(
        s"""val ${camelCase(edge.name)} = builder.addEdgeType(
           |  name = "${edge.name}",
           |  comment = "${escape(edge.comment)}"
           |).protoId(${edge.id})
           |$addPropertiesMaybe
           |""".stripMargin
      )
    }
  }

  def nodeBaseTypes() = {
    if (schema.nodeBaseTraits.nonEmpty) p("// node base types")
    schema.nodeBaseTraits.foreach { nodeBaseType =>
      val addPropertiesMaybe = {
        if (nodeBaseType.hasKeys.isEmpty) ""
        else {
          val properties = nodeBaseType.hasKeys.map(camelCase).mkString(", ")
          s".addProperties($properties)"
        }
      }

      p(
        s"""val ${camelCase(nodeBaseType.name)} = builder.addNodeBaseType(
           |  name = "${nodeBaseType.name}",
           |  comment = "${escape(nodeBaseType.comment)}"
           |)$addPropertiesMaybe
           |""".stripMargin
      )
    }
  }

  def nodeTypes() = {
    if (schema.nodeTypes.nonEmpty) p("// node types")
    schema.nodeTypes.foreach { nodeType =>
      val addPropertiesMaybe = {
        if (nodeType.keys.isEmpty) ""
        else {
          val properties = nodeType.keys.getOrElse(Nil).map(camelCase).mkString(", ")
          s".addProperties($properties)"
        }
      }
      val extendsMaybe = {
        if (nodeType.is.getOrElse(Nil).isEmpty) ""
        else {
          val extendz = nodeType.is.get.map(camelCase).mkString(", ")
          s".extendz($extendz)"
        }
      }
      val outEdgesMaybe = nodeType.outEdges.getOrElse(Nil).flatMap { outEdge =>
        val edgeName = camelCase(outEdge.edgeName)
        outEdge.inNodes.map { inNode =>
          val cardinalityOut = inNode.cardinality match {
            case Some(c) if c.endsWith(":1") => "Cardinality.One"
            case Some(c) if c.endsWith(":0-1") => "Cardinality.ZeroOrOne"
            case _ => "Cardinality.List"
          }
          val cardinalityIn = inNode.cardinality match {
            case Some(c) if c.startsWith("1:") => "Cardinality.One"
            case Some(c) if c.startsWith("0-1:") => "Cardinality.ZeroOrOne"
            case _ => "Cardinality.List"
          }
          s".addOutEdge(edge = $edgeName, inNode = ${ensureNoReservedName(camelCase(inNode.name))}, cardinalityOut = $cardinalityOut, cardinalityIn = $cardinalityIn)"
        }
      }.mkString("\n")

      val containedNodesMaybe = nodeType.containedNodesList.map { containedNode =>
        s""".addContainedNode(${camelCase(containedNode.nodeType)}, "${containedNode.localName}", Cardinality.${containedNode.cardinality.capitalize})"""
      }.mkString("\n")

      val nodeName = ensureNoReservedName(camelCase(nodeType.name))
      nodeType.id match {
        case Some(protoId) =>
          p( s"""lazy val $nodeName: NodeType = builder.addNodeType(
               |  name = "${nodeType.name}",
               |  comment = "${escape(nodeType.comment)}"
               |).protoId($protoId)
               |""".stripMargin
          )
        case None => p(nodeName)
      }

      p(
        s"""$addPropertiesMaybe
           |$extendsMaybe
           |$outEdgesMaybe
           |$containedNodesMaybe
           |""".stripMargin
      )
    }
  }

  def constants() = {
    p("// constants")
    Seq("dispatchTypes", "frameworks", "languages", "modifierTypes", "evaluationStrategies").map { jsonName =>
      val constants = schema.constantsFromElement(jsonName)
      if (constants.nonEmpty) {
        p(s"""val $jsonName = builder.addConstants(category = "${jsonName.capitalize}", """)
        constants.foreach { constant =>
          val protoIdMaybe = constant.id.map { id =>
            s".protoId($id)"
          }.getOrElse("")
          p(
            s"""  Constant(name = "${constant.name}", value = "${constant.value}", valueType = "String", comment = "${escape(constant.comment)}")$protoIdMaybe,""".stripMargin
          )
        }
        p(")\n")
      }
    }
    val operators = schema.constantsFromElement("operatorNames")(schema.constantReads("operator", "name"))
    if (operators.nonEmpty) {
      p(s"""val operators = builder.addConstants(category = "Operators", """)
      operators.foreach { operator =>
        p(
          s"""  Constant(name = "${operator.name}", value = "${operator.value}", valueType = "String", comment = "${escape(operator.comment)}"),""".stripMargin
        )
      }
      p(")\n")
    }
  }

  def p(s: String): Unit = {
    println(s)
  }

  def escape(s: String): String =
    s.replace("\"", "\\\"")

  def escape(s: Option[String]): String =
    s.map(escape).getOrElse("")

  def ensureNoReservedName(s: String): String =
    s match {
      case "return" => "ret"
      case "type" => "tpe"
      case s => s
    }
}
