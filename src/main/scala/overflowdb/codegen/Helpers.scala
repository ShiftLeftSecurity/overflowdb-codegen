package overflowdb.codegen

object Helpers {

  def isNodeBaseTrait(baseTraits: List[NodeBaseTrait], nodeName: String): Boolean =
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

  def getHigherType(property: Property): HigherValueType.Value =
    Cardinality.fromName(property.cardinality) match {
      case Cardinality.One       => HigherValueType.None
      case Cardinality.ZeroOrOne => HigherValueType.Option
      case Cardinality.List      => HigherValueType.List
    }

  def getBaseType(schemaType: String): String = {
    schemaType match {
      case "string"  => "String"
      case "int"     => "Integer"
      case "boolean" => "JBoolean"
      case _         => "Nothing"
    }
  }

  def getBaseType(property: Property): String =
    getBaseType(property.valueType)

  def getCompleteType(property: Property): String =
    getHigherType(property) match {
      case HigherValueType.None   => getBaseType(property)
      case HigherValueType.Option => s"Option[${getBaseType(property)}]"
      case HigherValueType.List   => s"List[${getBaseType(property)}]"
    }

  def getCompleteType(containedNode: ContainedNode): String = {
    val tpe = if (containedNode.nodeType != DefaultNodeTypes.Node) {
      containedNode.nodeTypeClassName + "Base"
    } else {
      containedNode.nodeTypeClassName
    }

    Cardinality.fromName(containedNode.cardinality) match {
      case Cardinality.ZeroOrOne => s"Option[$tpe]"
      case Cardinality.One       => tpe
      case Cardinality.List      => s"List[$tpe]"
    }
  }

  def propertyBasedFields(properties: List[Property]): String =
    properties.map { property =>
      val name = camelCase(property.name)
      val tpe = getCompleteType(property)
      val unsetValue = propertyUnsetValue(property)

      s"""private var _$name: $tpe = $unsetValue
         |def $name(): $tpe = _$name""".stripMargin
    }.mkString("\n\n")

  def propertyUnsetValue(property: Property): String =
    getHigherType(property) match {
      case HigherValueType.None => "null"
      case HigherValueType.Option => "None"
      case HigherValueType.List => "Nil"
    }

  def propertyKeyDef(name: String, baseType: String, cardinality: String) = {
    val completeType = Cardinality.fromName(cardinality) match {
      case Cardinality.One       => baseType
      case Cardinality.ZeroOrOne => baseType
      case Cardinality.List      => s"Seq[$baseType]"
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
