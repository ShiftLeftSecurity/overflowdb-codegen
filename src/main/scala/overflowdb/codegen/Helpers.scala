package overflowdb.codegen

import overflowdb.schema._

// TODO drop
object DefaultNodeTypes {
  /** root type for all nodes */
  val Node = "CPG_NODE"
  val NodeClassname = "CpgNode"
}

// TODO drop
object DefaultEdgeTypes {
  val ContainsNode = "CONTAINS_NODE"
}

object Helpers {

  /* surrounds input with `"` */
  def quoted(strings: Iterable[String]): Iterable[String] =
    strings.map(string => s""""$string"""")

  def stringToOption(s: String): Option[String] = s.trim match {
    case "" => None
    case nonEmptyString => Some(nonEmptyString)
  }

  def isNodeBaseTrait(baseTraits: List[NodeBaseType], nodeName: String): Boolean =
    nodeName == DefaultNodeTypes.Node || baseTraits.map(_.name).contains(nodeName)

  def camelCaseCaps(snakeCase: String): String = camelCase(snakeCase).capitalize

  def camelCase(snakeCase: String): String = {
    val corrected = // correcting for internal keys, like "_KEY" -> drop leading underscore
      if (snakeCase.startsWith("_")) snakeCase.drop(1)
      else snakeCase

    val elements: List[String] = corrected.split("_").map(_.toLowerCase).toList match {
      case head :: tail => head :: tail.map(_.capitalize)
      case Nil          => Nil
    }
    elements.mkString
  }

  def getHigherType(cardinality: Cardinality): HigherValueType.Value =
    cardinality match {
      case Cardinality.One       => HigherValueType.None
      case Cardinality.ZeroOrOne => HigherValueType.Option
      case Cardinality.List      => HigherValueType.List
      case  Cardinality.ISeq => ???
    }

  def getCompleteType(property: Property): String =
    getHigherType(property.cardinality) match {
      case HigherValueType.None   => property.valueType
      case HigherValueType.Option => s"Option[${property.valueType}]"
      case HigherValueType.List   => s"List[${property.valueType}]"
    }

  def getCompleteType(containedNode: ContainedNode): String = {
    val tpe = if (containedNode.nodeType.name != DefaultNodeTypes.Node) {
      containedNode.nodeType.className + "Base"
    } else {
      containedNode.nodeType.className
    }

    containedNode.cardinality match {
      case Cardinality.ZeroOrOne => s"Option[$tpe]"
      case Cardinality.One       => tpe
      case Cardinality.List      => s"List[$tpe]"
      case Cardinality.ISeq => s"IndexedSeq[$tpe]"
    }
  }

  def propertyBasedFields(properties: Seq[Property]): String =
    properties.map { property =>
      val name = camelCase(property.name)
      val tpe = getCompleteType(property)
      val unsetValue = propertyUnsetValue(property.cardinality)

      s"""private var _$name: $tpe = $unsetValue
         |def $name: $tpe = _$name""".stripMargin
    }.mkString("\n\n")

  def propertyUnsetValue(cardinality: Cardinality): String =
    getHigherType(cardinality) match {
      case HigherValueType.None => "null"
      case HigherValueType.Option => "None"
      case HigherValueType.List => "Nil"
    }

  def propertyKeyDef(name: String, baseType: String, cardinality: Cardinality) = {
    val completeType = cardinality match {
      case Cardinality.One       => baseType
      case Cardinality.ZeroOrOne => baseType
      case Cardinality.List      => s"Seq[$baseType]"
      case Cardinality.ISeq=> s"IndexedSeq[${baseType}]"
    }
    s"""val ${camelCaseCaps(name)} = new PropertyKey[$completeType]("$name") """
  }

  val propertyErrorRegisterImpl =
    s"""object PropertyErrorRegister {
       |  private var errorMap = Set[(Class[_], String)]()
       |  private val logger = org.slf4j.LoggerFactory.getLogger(getClass)
       |
       |  def logPropertyErrorIfFirst(clazz: Class[_], propertyName: String): Unit = {
       |    if (!errorMap.contains((clazz, propertyName))) {
       |      logger.warn("Property " + propertyName + " is deprecated for " + clazz.getName + ".")
       |      errorMap += ((clazz, propertyName))
       |    }
       |  }
       |}
       |""".stripMargin

  /** obtained from repl via
   * {{{
   * :power
   * nme.keywords
   * }}}
   */
  val scalaReservedKeywords = Set(
    "abstract", ">:", "if", ".", "catch", "protected", "final", "super", "while", "true", "val", "do", "throw",
    "<-", "package", "_", "macro", "@", "object", "false", "this", "then", "var", "trait", "with", "def", "else",
    "class", "type", "#", "lazy", "null", "=", "<:", "override", "=>", "private", "sealed", "finally", "new",
    "implicit", "extends", "for", "return", "case", "import", "forSome", ":", "yield", "try", "match", "<%")

  def escapeIfKeyword(value: String) =
    if (scalaReservedKeywords.contains(value)) s"`$value`"
    else value

}
