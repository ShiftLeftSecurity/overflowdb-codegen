package overflowdb.codegen

import overflowdb.algorithm.LowestCommonAncestors
import overflowdb.schema._
import overflowdb.schema.Property.ValueType

import scala.annotation.tailrec

// TODO drop
object DefaultNodeTypes {
  /** root type for all nodes */
  val AbstractNodeName = "ABSTRACT_NODE"
  val AbstractNodeClassname = "AbstractNode"

  val StoredNodeName = "STORED_NODE"
  val StoredNodeClassname = "StoredNode"

  lazy val AllClassNames = Set(AbstractNodeClassname, StoredNodeClassname)
}

// TODO drop
object DefaultEdgeTypes {
  val ContainsNode = "CONTAINS_NODE"
}

object Helpers {

  /* surrounds input with `"` */
  def quoted(strings: Iterable[String]): Iterable[String] =
    strings.map(quote)

  def quote(string: String): String =
    s""""$string""""

  def stringToOption(s: String): Option[String] = s.trim match {
    case "" => None
    case nonEmptyString => Some(nonEmptyString)
  }

  def typeFor[A](property: Property[A]): String = {
    val isMandatory = property.isMandatory
    property.valueType match {
      case ValueType.Boolean => if (isMandatory) "Boolean" else "java.lang.Boolean"
      case ValueType.String => "String"
      case ValueType.Byte => if (isMandatory) "Byte" else "java.lang.Byte"
      case ValueType.Short => if (isMandatory) "Short" else "java.lang.Short"
      case ValueType.Int => if (isMandatory) "scala.Int" else "Integer"
      case ValueType.Long => if (isMandatory) "Long" else "java.lang.Long"
      case ValueType.Float => if (isMandatory) "Float" else "java.lang.Float"
      case ValueType.Double => if (isMandatory) "Double" else "java.lang.Double"
      case ValueType.Char => if (isMandatory) "scala.Char" else "Character"
      case ValueType.List => "Seq[_]"
      case ValueType.NodeRef => "overflowdb.NodeRef[_]"
      case ValueType.Unknown => "java.lang.Object"
    }
  }

  def isNodeBaseTrait(baseTraits: Seq[NodeBaseType], nodeName: String): Boolean =
    nodeName == DefaultNodeTypes.AbstractNodeName || baseTraits.map(_.name).contains(nodeName)

  def camelCaseCaps(snakeCase: String): String = camelCase(snakeCase).capitalize

  def camelCase(snakeCase: String): String = {
    val corrected = // correcting for internal keys, like "_KEY" -> drop leading underscore
      if (snakeCase.startsWith("_")) snakeCase.drop(1)
      else snakeCase

    val elements: Seq[String] = corrected.split("_").map(_.toLowerCase).toList match {
      case head :: tail => head :: tail.map(_.capitalize)
      case Nil          => Nil
    }
    elements.mkString
  }

  /**
   * Converts from camelCase to snake_case
   * e.g.: camelCase => camel_case
   *
   * copy pasted from https://gist.github.com/sidharthkuruvila/3154845#gistcomment-2622928
   */
  def snakeCase(camelCase: String): String = {
    @tailrec
    def go(accDone: List[Char], acc: List[Char]): List[Char] = acc match {
      case Nil => accDone
      case a::b::c::tail if a.isUpper && b.isUpper && c.isLower => go(accDone ++ List(a, '_', b, c), tail)
      case a::b::tail if a.isLower && b.isUpper => go(accDone ++ List(a, '_', b), tail)
      case a::tail => go(accDone :+ a, tail)
    }
    go(Nil, camelCase.toList).mkString.toLowerCase
  }

  def singularize(str: String): String = {
    if (str.endsWith("ies")) {
      // e.g. Strategies -> Strategy
      s"${str.dropRight(3)}y"
    } else {
      // e.g. Types -> Type
      str.dropRight(1)
    }
  }

  def getCompleteType[A](property: Property[_]): String = {
    import Property.Cardinality
    val valueType = typeFor(property)
    property.cardinality match {
      case Cardinality.One(_)   => valueType
      case Cardinality.ZeroOrOne => s"Option[$valueType]"
      case Cardinality.List   => s"Seq[$valueType]"
      case Cardinality.ISeq   => s"IndexedSeq[$valueType]"
    }
  }

  def getCompleteType(containedNode: ContainedNode): String = {
    val className = containedNode.nodeType.className
    val tpe = if (DefaultNodeTypes.AllClassNames.contains(className)) {
      className
    } else {
      className + "Base"
    }

    containedNode.cardinality match {
      case Property.Cardinality.ZeroOrOne => s"Option[$tpe]"
      case Property.Cardinality.One(_)    => tpe
      case Property.Cardinality.List      => s"Seq[$tpe]"
      case Property.Cardinality.ISeq => s"IndexedSeq[$tpe]"
    }
  }

  def propertyKeyDef(name: String, baseType: String, cardinality: Property.Cardinality) = {
    val completeType = cardinality match {
      case Property.Cardinality.One(_)    => baseType
      case Property.Cardinality.ZeroOrOne => baseType
      case Property.Cardinality.List      => s"Seq[$baseType]"
      case Property.Cardinality.ISeq=> s"IndexedSeq[$baseType]"
    }
    s"""val ${camelCaseCaps(name)} = new overflowdb.PropertyKey[$completeType]("$name") """
  }

  def defaultValueImpl[A](default: Property.Default[A]): String =
    default.value match {
      case str: String => s"\"$str\""
      case char: Char => s"'$char'"
      case byte: Byte => s"$byte: Byte"
      case short: Short => s"$short: Short"
      case int: Int => s"$int: Int"
      case long: Long => s"$long: Long"
      case float: Float if float.isNaN => "Float.NaN"
      case float: Float => s"${float}f"
      case double: Double if double.isNaN => "Double.NaN"
      case double: Double => s"${double}d"
      case other => s"$other"
    }

  def defaultValueCheckImpl[A](memberName: String, default: Property.Default[A]): String = {
    val defaultValueSrc = defaultValueImpl(default)
    default.value match {
      case float: Float if float.isNaN => s"$memberName.isNaN"
      case double: Double if double.isNaN => s"$memberName.isNaN"
      case _ => s"($defaultValueSrc) == $memberName"
    }
  }

  def propertyDefaultValueImpl(propertyDefaultsPath: String, properties: Seq[Property[_]]): String = {
    val propertyDefaultValueCases = properties.collect {
      case property if property.hasDefault =>
        s"""case "${property.name}" => $propertyDefaultsPath.${property.className}"""
    }.mkString("\n|    ")

    s"""
       |  override def propertyDefaultValue(propertyKey: String) =
       |    propertyKey match {
       |      $propertyDefaultValueCases
       |      case _ => super.propertyDefaultValue(propertyKey)
       |  }
       |""".stripMargin
  }

  def propertyDefaultCases(properties: Seq[Property[_]]): String =
    properties.collect {
      case p if p.hasDefault =>
        s"""val ${p.className} = ${defaultValueImpl(p.default.get)}"""
    }.mkString("\n|    ")

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

  def fullScalaType(neighborNode: AbstractNodeType, cardinality: EdgeType.Cardinality): String = {
    val neighborNodeClass = neighborNode.className
    cardinality match {
      case EdgeType.Cardinality.List => s"overflowdb.traversal.Traversal[$neighborNodeClass]"
      case EdgeType.Cardinality.ZeroOrOne => s"Option[$neighborNodeClass]"
      case EdgeType.Cardinality.One => s"$neighborNodeClass"
    }
  }

  // TODO return AbstractNodeType - need DefaultNodeTypes.StoredNode: AbstractNodeType
  def deriveCommonRootType(neighborNodeInfos: Set[AbstractNodeType]): String = {
    lowestCommonAncestor(neighborNodeInfos)
      .orElse(findSharedRoot(neighborNodeInfos))
      .map(_.className)
      .getOrElse(DefaultNodeTypes.StoredNodeClassname)
  }

  /** in theory there can be multiple candidates - we're just returning one of those for now */
  def lowestCommonAncestor(nodes: Set[AbstractNodeType]): Option[AbstractNodeType] = {
    LowestCommonAncestors(nodes)(_.extendzRecursively.toSet).headOption
  }

  /** from the given node types, find one that is part of the complete type hierarchy of *all* other node types */
  def findSharedRoot(nodeTypes: Set[AbstractNodeType]): Option[AbstractNodeType] = {
    if (nodeTypes.size == 1) {
      Some(nodeTypes.head)
    } else if (nodeTypes.size > 1) {
      // trying to keep it deterministic...
      val sorted = nodeTypes.toSeq.sortBy(_.className)
      val (first, otherNodes) = (sorted.head, sorted.tail)
      completeTypeHierarchy(first).find { candidate =>
        otherNodes.forall { otherNode =>
          completeTypeHierarchy(otherNode).contains(candidate)
        }
      }
    } else {
      None
    }
  }

  def completeTypeHierarchy(node: AbstractNodeType): Seq[AbstractNodeType] =
    node +: node.extendzRecursively

}
