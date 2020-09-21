package overflowdb.codegen

import better.files._
import java.io.{File => JFile}

object CodeGen extends App {
  assert(args.size == 2, s"expected two arguments (path to schema json file, basePackage), but got ${args.length}")
  val schemaFile :: basePackage :: Nil = args.toList
  val outputDir = new java.io.File("target")
  new CodeGen(schemaFile, basePackage).run(outputDir)
}

/** Generates a domain model for OverflowDb traversals based on your domain-specific json schema.
  *
  * @param schemaFile: path to the schema (json file)
  * @param basePackage: specific for your domain, e.g. `com.example.mydomain`
  */
class CodeGen(schemaFile: String, basePackage: String) {
  import Helpers._
  val nodesPackage = s"$basePackage.nodes"
  val edgesPackage = s"$basePackage.edges"
  val schema = new Schema(schemaFile)

  def run(outputDir: JFile): List[JFile] =
    List(
      writeConstants(outputDir),
      writeEdgeFiles(outputDir),
      writeNodeFiles(outputDir),
      writeNewNodeFiles(outputDir))

  def writeConstants(outputDir: JFile): JFile = {
    val baseDir = File(outputDir.getPath + "/" + basePackage.replaceAll("\\.", "/")).createDirectories

    def writeConstantsFile(className: String, constants: List[Constant])(mkSrc: Constant => String): Unit = {
      val src = constants.map { constant =>
        val documentation = constant.comment.filter(_.nonEmpty).map(comment => s"""/** $comment */""").getOrElse("")
        s""" $documentation
           | ${mkSrc(constant)}
           |""".stripMargin
      }.mkString("\n")

      baseDir.createChild(s"$className.java").write(
        s"""package io.shiftleft.codepropertygraph.generated;
           |
           |import overflowdb.*;
           |
           |public class $className {
           |
           |$src
           |}""".stripMargin
      )
    }

    def writeStringConstants(className: String, constants: List[Constant]): Unit = {
      writeConstantsFile(className, constants) { constant =>
        s"""public static final String ${constant.name} = "${constant.value}";"""
      }
    }

    def writePropertyKeyConstants(className: String, constants: List[Constant]): Unit = {
      writeConstantsFile(className, constants) { constant =>
        val valueType = constant.valueType.getOrElse(throw new AssertionError(s"`valueType` must be defined for Key constant - not the case for $constant"))
        val cardinality = constant.cardinality.getOrElse(throw new AssertionError(s"`cardinality` must be defined for Key constant - not the case for $constant"))
        val baseType = valueType match {
          case "string"  => "String"
          case "int"     => "Integer"
          case "boolean" => "Boolean"
        }
        val completeType = Cardinality.fromName(cardinality) match {
          case Cardinality.One       => baseType
          case Cardinality.ZeroOrOne => baseType
          case Cardinality.List      => s"scala.collection.Seq<$baseType>"
        }
        s"""public static final PropertyKey<$completeType> ${constant.name} = new PropertyKey<>("${constant.value}");"""
      }
    }

    writeStringConstants("NodeKeyNames", schema.nodeKeys.map(Constant.fromProperty))
    writeStringConstants("EdgeKeyNames", schema.edgeKeys.map(Constant.fromProperty))
    writeStringConstants("NodeTypes", schema.nodeTypes.map(Constant.fromNodeType))
    writeStringConstants("EdgeTypes", schema.edgeTypes.map(Constant.fromEdgeType))

    List("dispatchTypes", "frameworks", "languages", "modifierTypes", "evaluationStrategies").foreach { element =>
      writeStringConstants(element.capitalize, schema.constantsFromElement(element))
    }
    List("edgeKeys", "nodeKeys").foreach { element =>
      writePropertyKeyConstants(s"${element.capitalize}", schema.constantsFromElement(element))
    }
    writeStringConstants("Operators", schema.constantsFromElement("operatorNames")(schema.constantReads("operator", "name")))

    outputDir
  }

  def writeEdgeFiles(outputDir: JFile): JFile = {
    val staticHeader =
      s"""package $edgesPackage
         |
         |import java.lang.{Boolean => JBoolean, Long => JLong}
         |import java.util.{Set => JSet}
         |import java.util.{List => JList}
         |import overflowdb._
         |import scala.jdk.CollectionConverters._
         |""".stripMargin

    val packageObject = {
      val factories = {
        val edgeFactories: List[String] = schema.edgeTypes.map(edgeType => edgeType.className + ".factory")
        s"""object Factories {
           |  lazy val all: List[EdgeFactory[_]] = $edgeFactories
           |  lazy val allAsJava: java.util.List[EdgeFactory[_]] = all.asJava
           |}
           |""".stripMargin
      }

      s"""$staticHeader
         |$propertyErrorRegisterImpl
         |$factories
         |""".stripMargin
    }

    def generateEdgeSource(edgeType: EdgeType, keys: List[Property]) = {
      val edgeClassName = edgeType.className

      val keyNames = keys.map(p => camelCaseCaps(p.name))

      val propertyNameDefs = keys.map { p =>
        s"""val ${camelCaseCaps(p.name)} = "${p.name}" """
      }.mkString("\n|    ")

      val propertyDefs = keys.map { p =>
        val baseType = p.valueType match {
          case "string"  => "String"
          case "int"     => "Integer"
          case "boolean" => "Boolean"
        }
        propertyKeyDef(p.name, baseType, p.cardinality)
      }.mkString("\n|    ")

      val companionObject =
        s"""object $edgeClassName {
           |  val Label = "${edgeType.name}"
           |
           |  object PropertyNames {
           |    $propertyNameDefs
           |    val all: Set[String] = Set(${keyNames.mkString(", ")})
           |    val allAsJava: JSet[String] = all.asJava
           |  }
           |
           |  object Properties {
           |    $propertyDefs
           |  }
           |
           |  val layoutInformation = new EdgeLayoutInformation(Label, PropertyNames.allAsJava)
           |
           |  val factory = new EdgeFactory[$edgeClassName] {
           |    override val forLabel = $edgeClassName.Label
           |
           |    override def createEdge(graph: Graph, outNode: NodeRef[NodeDb], inNode: NodeRef[NodeDb]) =
           |      new $edgeClassName(graph, outNode, inNode)
           |  }
           |}
           |""".stripMargin

      def propertyBasedFieldAccessors(properties: List[Property]): String =
        properties.map { property =>
          val name = camelCase(property.name)
          val tpe = getCompleteType(property)

          getHigherType(property) match {
            case HigherValueType.None =>
              s"""def $name: $tpe = property("${property.name}").asInstanceOf[$tpe]"""
            case HigherValueType.Option =>
              s"""def $name: $tpe = Option(property("${property.name}")).asInstanceOf[$tpe]""".stripMargin
            case HigherValueType.List =>
              s"""private var _$name: $tpe = Nil
                 |def $name: $tpe = {
                 |  val p = property("${property.name}")
                 |  if (p != null) p.asInstanceOf[JList].asScala
                 |  else Nil
                 |}""".stripMargin
          }
        }.mkString("\n\n")

      val classImpl =
        s"""class $edgeClassName(_graph: Graph, _outNode: NodeRef[NodeDb], _inNode: NodeRef[NodeDb])
           |extends Edge(_graph, $edgeClassName.Label, _outNode, _inNode, $edgeClassName.PropertyNames.allAsJava) {
           |${propertyBasedFieldAccessors(keys)}
           |}
           |""".stripMargin

      s"""$staticHeader
         |$companionObject
         |$classImpl
         |""".stripMargin
    }

    val baseDir = File(outputDir.getPath + "/" + edgesPackage.replaceAll("\\.", "/"))
    if (baseDir.exists) baseDir.delete()
    baseDir.createDirectories()
    baseDir.createChild("package.scala").write(packageObject)
    schema.edgeTypes.foreach { edge =>
      val src = generateEdgeSource(edge, edge.keys.map(schema.edgePropertyByName))
      val srcFile = edge.className + ".scala"
      baseDir.createChild(srcFile).write(src)
    }
    println(s"generated edge sources in $baseDir (${baseDir.list.size} files)")
    baseDir.toJava
  }

  def neighborAccessorNameForEdge(edgeTypeName: String, direction: Direction.Value): String =
    "_" + camelCase(edgeTypeName + "_" + direction)

  def writeNodeFiles(outputDir: JFile): JFile = {
    val staticHeader =
      s"""package $nodesPackage
         |
         |import $basePackage.EdgeKeys
         |import $edgesPackage
         |import java.lang.{Boolean => JBoolean, Long => JLong}
         |import java.util.{Collections => JCollections, HashMap => JHashMap, Iterator => JIterator, Map => JMap, Set => JSet}
         |import overflowdb._
         |import overflowdb.traversal.Traversal
         |import scala.jdk.CollectionConverters._
         |""".stripMargin

    val rootTypeImpl = {
      val genericNeighborAccessors = for {
        direction <- Direction.all
        edgeType <- schema.edgeTypes
        accessor = neighborAccessorNameForEdge(edgeType.name, direction)
      } yield s"def $accessor(): JIterator[StoredNode] = { JCollections.emptyIterator() }"

      val keyBasedTraits =
        schema.nodeKeys.map { property =>
          val camelCaseName = camelCase(property.name)
          val camelCaseCapitalized = camelCaseName.capitalize
          val tpe = getCompleteType(property)
          s"trait Has$camelCaseCapitalized { def $camelCaseName: $tpe }"
        }.mkString("\n") + "\n"

      val factories = {
        val nodeFactories: List[String] =
          schema.nodeTypes.map(nodeType => nodeType.className + ".factory")
        s"""object Factories {
           |  lazy val all: List[NodeFactory[_]] = $nodeFactories
           |  lazy val allAsJava: java.util.List[NodeFactory[_]] = all.asJava
           |}
           |""".stripMargin
      }

      s"""$staticHeader
         |$propertyErrorRegisterImpl
         |
         |trait CpgNode {
         |  def label: String
         |}
         |
         |/* a node that stored inside an Graph (rather than e.g. DiffGraph) */
         |trait StoredNode extends Node with CpgNode with Product {
         |  /* underlying Node in the graph.
         |   * since this is a StoredNode, this is always set */
         |  def underlying: Node = this
         |
         |  /** labels of product elements, used e.g. for pretty-printing */
         |  def productElementLabel(n: Int): String
         |
         |  /* all properties plus label and id */
         |  def toMap: Map[String, Any] = {
         |    val map = valueMap
         |    map.put("_label", label)
         |    map.put("_id", id: java.lang.Long)
         |    map.asScala.toMap
         |  }
         |
         |  /*Sets fields from newNode*/
         |  def fromNewNode(newNode: NewNode, mapping: NewNode => StoredNode):Unit = ???
         |
         |  /* all properties */
         |  def valueMap: JMap[String, AnyRef]
         |
         |  ${genericNeighborAccessors.mkString("\n")}
         |}
         |
         |  $keyBasedTraits
         |  $factories
         |""".stripMargin
    }

    lazy val packageObject = {
      val implicitsForTraversals =
        schema.nodeTypes.map(_.className).sorted.map { name =>
          val traversalName = s"${name}Traversal"
          s"implicit def to$traversalName(trav: Traversal[$name]): $traversalName = new $traversalName(trav)"
        }.mkString("\n")

      s"""package $basePackage
         |
         |import overflowdb.traversal.Traversal
         |
         |package object nodes {
         |  $implicitsForTraversals
         |}
         |""".stripMargin
    }

    def generatePropertyTraversals(className: String, properties: Seq[Property]): String = {
      val propertyTraversals = properties.map { property =>
        val name = camelCase(property.name)
        val baseType = getBaseType(property)

        val mapOrFlatMap =  Cardinality.fromName(property.cardinality) match {
          case Cardinality.One => "map"
          case Cardinality.ZeroOrOne | Cardinality.List => "flatMap"
        }

        s"""
           |  /** Traverse to $name property */
           |  def $name: Traversal[$baseType] =
           |    traversal.$mapOrFlatMap(_.$name)
           |
           |  /**
           |    * Traverse to nodes where the $name matches the regular expression `value`
           |    * */
           |  def $name(value: $baseType): Traversal[$className] =
           |    ??? //StringPropertyAccessors.filter(traversal, NodeKeys.NAME, value)
           |
           |  /**
           |    * Traverse to nodes where the $name matches at least one of the regular expressions in `values`
           |    * */
           |  def $name(value: $baseType*): Traversal[$className] =
           |    ??? //StringPropertyAccessors.filterMultiple(traversal, NodeKeys.NAME, value: _*)
           |
           |  /**
           |    * Traverse to nodes where $name matches `value` exactly.
           |    * */
           |  def ${name}Exact(value: $baseType): Traversal[$className] =
           |    ??? //StringPropertyAccessors.filterExact(traversal, NodeKeys.NAME, value)
           |
           |  /**
           |    * Traverse to nodes where $name matches one of the elements in `values` exactly.
           |    * */
           |  def ${name}Exact(values: $baseType*): Traversal[$className] =
           |    ??? //StringPropertyAccessors.filterExactMultiple(traversal, NodeKeys.NAME, values: _*)
           |
           |  /**
           |    * Traverse to nodes where $name does not match the regular expression `value`.
           |    * */
           |  def ${name}Not(value: $baseType): Traversal[$className] =
           |    ??? //StringPropertyAccessors.filterNot(traversal, NodeKeys.NAME, value)
           |
           |  /**
           |    * Traverse to nodes where $name does not match any of the regular expressions in `values`.
           |    * */
           |  def ${name}Not(values: $baseType*): Traversal[$className] =
           |    ??? //StringPropertyAccessors.filterNotMultiple(traversal, NodeKeys.NAME, values: _*)
           |
           |""".stripMargin
      }.mkString("\n")

      s"""
         |/** Traversal steps for $className */
         |class ${className}Traversal(val traversal: Traversal[$className]) extends AnyVal {
         |
         |$propertyTraversals
         |
         |}""".stripMargin
    }

    def generateNodeBaseTypeSource(nodeBaseTrait: NodeBaseTrait): String = {
      val className = nodeBaseTrait.className
      val properties = nodeBaseTrait.hasKeys.map(schema.nodePropertyByName)

      val mixins = nodeBaseTrait.hasKeys.map { key =>
        s"with Has${camelCaseCaps(key)}"
      }.mkString(" ")

      val mixinTraits = nodeBaseTrait
        .extendz
        .getOrElse(Nil)
        .map { traitName =>
          s"with ${camelCaseCaps(traitName)}"
        }.mkString(" ")

      val mixinTraitsForBase = nodeBaseTrait
        .extendz
        .getOrElse(List())
        .map { traitName =>
          s"with ${camelCaseCaps(traitName)}Base"
        }.mkString(" ")

      s"""$staticHeader
         |
         |trait ${className}Base extends CpgNode $mixins $mixinTraitsForBase
         |
         |trait $className extends StoredNode with ${className}Base $mixinTraits
         |
         |${generatePropertyTraversals(className, properties)}
         |
         |""".stripMargin
    }

    def generateNodeSource(nodeType: NodeType, keys: List[Property]) = {
      val keyNames = keys.map(_.name) ++ nodeType.containedNodesList.map(_.localName)
      val propertyNameDefs = keyNames.map { name =>
        s"""val ${camelCaseCaps(name)} = "$name" """
      }.mkString("\n|    ")

      val propertyDefs = keys.map { p =>
        val baseType = p.valueType match {
          case "string"  => "String"
          case "int"     => "Integer"
          case "boolean" => "Boolean"
        }
        propertyKeyDef(p.name, baseType, p.cardinality)
      }.mkString("\n|    ")

      val propertyDefsForContainedNodes = nodeType.containedNodesList.map { containedNode =>
        propertyKeyDef(containedNode.localName, containedNode.nodeTypeClassName, containedNode.cardinality)
      }.mkString("\n|    ")

      val outEdgeNames: Seq[String] = nodeType.outEdges.map(_.edgeName)
      val inEdgeNames:  Seq[String] = schema.nodeToInEdgeContexts.getOrElse(nodeType.name, Seq.empty).map(_.edgeName)

      val outEdgeLayouts = outEdgeNames.map(edge => s"edges.${camelCaseCaps(edge)}.layoutInformation").mkString(", ")
      val inEdgeLayouts = inEdgeNames.map(edge => s"edges.${camelCaseCaps(edge)}.layoutInformation").mkString(", ")

      val className = nodeType.className
      val classNameDb = nodeType.classNameDb

      val companionObject =
        s"""object $className {
           |  def apply(graph: Graph, id: Long) = new $className(graph, id)
           |
           |  val Label = "${nodeType.name}"
           |  val LabelId: Int = ${nodeType.id}
           |
           |  object PropertyNames {
           |    $propertyNameDefs
           |    val all: Set[String] = Set(${keyNames.map(camelCaseCaps).mkString(", ")})
           |    val allAsJava: JSet[String] = all.asJava
           |  }
           |
           |  object Properties {
           |    $propertyDefs
           |    $propertyDefsForContainedNodes
           |  }
           |
           |  val layoutInformation = new NodeLayoutInformation(
           |    LabelId,
           |    PropertyNames.allAsJava,
           |    List($outEdgeLayouts).asJava,
           |    List($inEdgeLayouts).asJava)
           |
           |  object Edges {
           |    val In: Array[String] = Array(${quoted(inEdgeNames).mkString(",")})
           |    val Out: Array[String] = Array(${quoted(outEdgeNames).mkString(",")})
           |  }
           |
           |  val factory = new NodeFactory[$classNameDb] {
           |    override val forLabel = $className.Label
           |    override val forLabelId = $className.LabelId
           |
           |    override def createNode(ref: NodeRef[$classNameDb]) =
           |      new $classNameDb(ref.asInstanceOf[NodeRef[NodeDb]])
           |
           |    override def createNodeRef(graph: Graph, id: Long) = $className(graph, id)
           |  }
           |}
           |""".stripMargin

      val mixinTraits: String =
        nodeType.is
          .getOrElse(List())
          .map { traitName =>
            s"with ${camelCaseCaps(traitName)}"
          }
          .mkString(" ")

      val mixinTraitsForBase: String =
        nodeType.is
          .getOrElse(List())
          .map { traitName =>
            s"with ${camelCaseCaps(traitName)}Base"
          }
          .mkString(" ")

      val propertyBasedTraits = keys.map(key => s"with Has${camelCaseCaps(key.name)}").mkString(" ")

      val valueMapImpl = {
        val putKeysImpl = keys
          .map { key: Property =>
            val memberName = camelCase(key.name)
            Cardinality.fromName(key.cardinality) match {
              case Cardinality.One =>
                s"""if ($memberName != null) { properties.put("${key.name}", $memberName) }"""
              case Cardinality.ZeroOrOne =>
                s"""$memberName.map { value => properties.put("${key.name}", value) }"""
              case Cardinality.List => // need java list, e.g. for NodeSerializer
                s"""if ($memberName.nonEmpty) { properties.put("${key.name}", $memberName.asJava) }"""
            }
          }
          .mkString("\n")
        val putRefsImpl = {
          nodeType.containedNodesList.map { cnt =>
            val memberName = cnt.localName
            Cardinality.fromName(cnt.cardinality) match {
              case Cardinality.One =>
                s"""   if (this._$memberName != null) { properties.put("${memberName}", this._$memberName) }"""
              case Cardinality.ZeroOrOne =>
                s"""   if (this._$memberName.nonEmpty) { properties.put("${memberName}", this._$memberName.get) }"""
              case Cardinality.List => // need java list, e.g. for NodeSerializer
                s"""  if (this._$memberName.nonEmpty) { properties.put("${memberName}", this._$memberName.asJava) }"""
            }
          }
        }.mkString("\n")

        s""" {
        |  val properties = new JHashMap[String, AnyRef]
        |$putKeysImpl
        |$putRefsImpl
        |  properties
        |}""".stripMargin
      }

      val fromNew = {
        val initKeysImpl = keys
          .map { key: Property =>
            val memberName = camelCase(key.name)
            Cardinality.fromName(key.cardinality) match {
              case Cardinality.One =>
                s"""   this._$memberName = other.$memberName""".stripMargin
              case Cardinality.ZeroOrOne =>
                s"""   this._$memberName = if(other.$memberName != null) other.$memberName else None""".stripMargin
              case Cardinality.List =>
                s"""   this._$memberName = if(other.$memberName != null) other.$memberName else Nil""".stripMargin
            }
            s"""   this._$memberName = other.$memberName""".stripMargin
          }
          .mkString("\n")

        val initRefsImpl = {
          nodeType.containedNodesList.map { containedNode =>
            val memberName = containedNode.localName
            val containedNodeType = containedNode.nodeTypeClassName

            Cardinality.fromName(containedNode.cardinality) match {
              case Cardinality.One =>
                s"""  this._$memberName = other.$memberName match {
                   |    case null => null
                   |    case newNode: NewNode => mapping(newNode).asInstanceOf[$containedNodeType]
                   |    case oldNode: StoredNode => oldNode.asInstanceOf[$containedNodeType]
                   |    case _ => throw new MatchError("unreachable")
                   |  }""".stripMargin
              case Cardinality.ZeroOrOne =>
                s"""  this._$memberName = other.$memberName match {
                   |    case null | None => None
                   |    case Some(newNode:NewNode) => Some(mapping(newNode).asInstanceOf[$containedNodeType])
                   |    case Some(oldNode:StoredNode) => Some(oldNode.asInstanceOf[$containedNodeType])
                   |    case _ => throw new MatchError("unreachable")
                   |  }""".stripMargin
              case Cardinality.List => // need java list, e.g. for NodeSerializer
                s"""  this._$memberName = if(other.$memberName == null) Nil else other.$memberName.map { nodeRef => nodeRef match {
                   |    case null => throw new NullPointerException("Nullpointers forbidden in contained nodes")
                   |    case newNode:NewNode => mapping(newNode).asInstanceOf[$containedNodeType]
                   |    case oldNode:StoredNode => oldNode.asInstanceOf[$containedNodeType]
                   |    case _ => throw new MatchError("unreachable")
                   |  }}""".stripMargin
            }
          }
        }.mkString("\n")

        val registerFullName = if(!keys.map{_.name}.contains("FULL_NAME")) "" else {
          s"""  graph.indexManager.putIfIndexed("FULL_NAME", other.fullName, this.ref)"""
        }

        s"""override def fromNewNode(someNewNode: NewNode, mapping: NewNode => StoredNode):Unit = {
           |  //this will throw for bad types -- no need to check by hand, we don't have a better error message
           |  val other = someNewNode.asInstanceOf[New${nodeType.className}]
           |$initKeysImpl
           |$initRefsImpl
           |$registerFullName
           |}""".stripMargin
      }

      val containedNodesAsMembers =
        nodeType.containedNodesList
          .map { containedNode =>
            val containedNodeType = containedNode.nodeTypeClassName
            val cardinality = Cardinality.fromName(containedNode.cardinality)
            cardinality match {
              case Cardinality.One =>
                s"""
                   |private var _${containedNode.localName}: $containedNodeType = null
                   |def ${containedNode.localName}: $containedNodeType = this._${containedNode.localName}
                   |""".stripMargin
              case Cardinality.ZeroOrOne =>
                s"""
                   |private var _${containedNode.localName}: Option[$containedNodeType] = None
                   |def ${containedNode.localName}: Option[$containedNodeType] = this._${containedNode.localName}
                   |""".stripMargin
              case Cardinality.List =>
                s"""
                   |private var _${containedNode.localName}: List[$containedNodeType] = Nil
                   |def ${containedNode.localName}: List[$containedNodeType] = this._${containedNode.localName}
                   |""".stripMargin
            }
          }
          .mkString("\n")

      val productElements: List[ProductElement] = {
        var currIndex = -1
        def nextIdx = { currIndex += 1; currIndex }
        val forId = ProductElement("id", "id", nextIdx)
        val forKeys = keys.map { key =>
          val name = camelCase(key.name)
          ProductElement(name, name, nextIdx)
        }
        val forContainedNodes = nodeType.containedNodesList.map { containedNode =>
          ProductElement(
            containedNode.localName,
            containedNode.localName,
            nextIdx)
        }
        forId +: (forKeys ++ forContainedNodes)
      }

      val productElementLabels =
        productElements.map { case ProductElement(name, accessorSrc, index) =>
          s"""case $index => "$name" """
        }.mkString("\n")

      val productElementAccessors =
        productElements.map { case ProductElement(name, accessorSrc, index) =>
          s"case $index => $accessorSrc"
        }.mkString("\n")

      val abstractContainedNodeAccessors = nodeType.containedNodesList.map { containedNode =>
        s"""def ${containedNode.localName}: ${getCompleteType(containedNode)}"""
      }.mkString("\n")

      val delegatingContainedNodeAccessors = nodeType.containedNodesList.map { containedNode =>
        s"""  def ${containedNode.localName} = get().${containedNode.localName}"""
      }.mkString("\n")

      val nodeBaseImpl =
        s"""trait ${className}Base extends CpgNode $mixinTraitsForBase $propertyBasedTraits {
           |  def asStored : StoredNode = this.asInstanceOf[StoredNode]
           |
           |  $abstractContainedNodeAccessors
           |}
           |""".stripMargin

      val neighborInfos: List[NeighborInfo] = {
        /** the offsetPos determines the index into the adjacent nodes array of a given node type
         * assigning numbers here must follow the same way as in NodeLayoutInformation, i.e. starting at 0,
         * first assign ids to the outEdges based on their order in the list, and then the same for inEdges */
        var offsetPos = -1
        def nextOffsetPos = { offsetPos += 1; offsetPos }

        val inEdges = schema.nodeToInEdgeContexts.getOrElse(nodeType.name, Nil)

        def createNeighborNodeInfo(nodeName: String, neighborClassName: String, edgeAndDirection: String, cardinality: Cardinality) = {
          val accessorName = s"_${camelCase(nodeName)}Via${edgeAndDirection.capitalize}"
          NeighborNodeInfo(Helpers.escapeIfKeyword(accessorName), neighborClassName, cardinality)
        }

        val neighborOutInfos =
          nodeType.outEdges.map { case OutEdgeEntry(edgeName, inNodes) =>
            val viaEdgeAndDirection = camelCase(edgeName) + "Out"
            val neighborNodeInfos = inNodes.map { inNode =>
              val nodeName = inNode.name
              val cardinality = inNode.cardinality match {
                case Some(c) if c.endsWith(":1") => Cardinality.One
                case Some(c) if c.endsWith(":0-1") => Cardinality.ZeroOrOne
                case _ => Cardinality.List
              }
              createNeighborNodeInfo(nodeName, camelCaseCaps(nodeName), viaEdgeAndDirection, cardinality)
            }.toSet
            NeighborInfo(neighborAccessorNameForEdge(edgeName, Direction.OUT), neighborNodeInfos, nextOffsetPos)
          }

        val neighborInInfos =
          inEdges.map { case InEdgeContext(edgeName, neighborNodes) =>
            val viaEdgeAndDirection = camelCase(edgeName) + "In"
            val neighborNodeInfos = neighborNodes.map { neighborNode =>
              val neighborNodeClassName = schema.nodeTypeByName(neighborNode.name).className
              // note: cardinalities are defined on the 'other' side, i.e. on `outEdges.inEdges.cardinality`
              // therefor, here we're interested in the left side of the `:`
              val cardinality = neighborNode.cardinality match {
                case Some(c) if c.startsWith("1:") => Cardinality.One
                case Some(c) if c.startsWith("0-1:") => Cardinality.ZeroOrOne
                case _ => Cardinality.List
              }
              createNeighborNodeInfo(neighborNode.name, neighborNodeClassName, viaEdgeAndDirection, cardinality)
            }
            NeighborInfo(neighborAccessorNameForEdge(edgeName, Direction.IN), neighborNodeInfos, nextOffsetPos)
          }

        neighborOutInfos ++ neighborInInfos
      }

      val neighborDelegators = neighborInfos.flatMap { case NeighborInfo(accessorNameForEdge, nodeInfos, _) =>
        val genericEdgeBasedDelegators =
          s"override def $accessorNameForEdge(): JIterator[StoredNode] = get().$accessorNameForEdge"

        val specificNodeBasedDelegators = nodeInfos.filter(_.className != DefaultNodeTypes.NodeClassname).map {
          case NeighborNodeInfo(accessorNameForNode, className, cardinality)  =>
            val returnType = cardinality match {
              case Cardinality.List => s"Iterator[$className]"
              case Cardinality.ZeroOrOne => s"Option[$className]"
              case Cardinality.One => s"$className"
            }
            s"def $accessorNameForNode: $returnType = get().$accessorNameForNode"
          }
        specificNodeBasedDelegators + genericEdgeBasedDelegators
      }.mkString("\n")

      val nodeRefImpl = {
        val propertyDelegators = keys.map { key =>
          val name = camelCase(key.name)
          s"""  override def $name: ${getCompleteType(key)} = get().$name"""
        }.mkString("\n")

        s"""class $className(graph: Graph, id: Long) extends NodeRef[$classNameDb](graph, id)
           |  with ${className}Base
           |  with StoredNode
           |  $mixinTraits {
           |  $propertyDelegators
           |  $delegatingContainedNodeAccessors
           |  $neighborDelegators
           |  override def fromNewNode(newNode: NewNode, mapping: NewNode => StoredNode): Unit = get().fromNewNode(newNode, mapping)
           |  override def valueMap: JMap[String, AnyRef] = get.valueMap
           |  override def canEqual(that: Any): Boolean = get.canEqual(that)
           |  override def label: String = {
           |    $className.Label
           |  }
           |
           |  override def productElementLabel(n: Int): String =
           |    n match {
           |      $productElementLabels
           |    }
           |
           |  override def productElement(n: Int): Any =
           |    n match {
           |      $productElementAccessors
           |    }
           |
           |  override def productPrefix = "$className"
           |  override def productArity = ${productElements.size}
           |}
           |""".stripMargin
      }

      val neighborAccessors = neighborInfos.flatMap { case NeighborInfo(accessorNameForEdge, nodeInfos, offsetPos) =>
        val genericEdgeBasedAccessor =
          s"override def $accessorNameForEdge: JIterator[StoredNode] = createAdjacentNodeIteratorByOffSet($offsetPos).asInstanceOf[JIterator[StoredNode]]"

        val specificNodeBasedAccessors = nodeInfos.filter(_.className != DefaultNodeTypes.NodeClassname).map {
          case NeighborNodeInfo(accessorNameForNode, className, cardinality) =>
            cardinality match {
              case Cardinality.List =>
                s"def $accessorNameForNode: Iterator[$className] = $accessorNameForEdge.asScala.collect { case node: $className => node }"
              case Cardinality.ZeroOrOne =>
                s"def $accessorNameForNode: Option[$className] = $accessorNameForEdge.asScala.collect { case node: $className => node }.nextOption"
              case Cardinality.One =>
                s"def $accessorNameForNode: $className = $accessorNameForEdge.asScala.collect { case node: $className => node }.next"
            }
        }
        specificNodeBasedAccessors + genericEdgeBasedAccessor
      }.mkString("\n")

      val updateSpecificPropertyImpl: String = {
        def caseEntry(name: String, accessorName: String, cardinality: String, baseType: String) = {
          val setter = Cardinality.fromName(cardinality) match {
            case Cardinality.One =>
              s"value.asInstanceOf[$baseType]"
            case Cardinality.ZeroOrOne =>
              s"""value match {
                 |        case null | None => None
                 |        case someVal: $baseType => Some(someVal)
                 |      }""".stripMargin
            case Cardinality.List =>
              s"""value match {
                 |        case singleValue: $baseType => List(singleValue)
                 |        case null | None | Nil => Nil
                 |        case jCollection: java.lang.Iterable[_] => jCollection.asInstanceOf[java.util.Collection[$baseType]].iterator.asScala.toList
                 |        case lst: List[_] => value.asInstanceOf[List[$baseType]]
                 |      }""".stripMargin
          }
          s"""|      case "$name" => this._$accessorName = $setter"""
        }

        val forKeys = keys.map(key => caseEntry(key.name, camelCase(key.name), key.cardinality, getBaseType(key.valueType))).mkString("\n")

        val forContaintedNodes = nodeType.containedNodesList.map(containedNode =>
          caseEntry(containedNode.localName, containedNode.localName, containedNode.cardinality, containedNode.nodeTypeClassName)
        ).mkString("\n")

        s"""  override protected def updateSpecificProperty(key:String, value: Object): Unit = {
           |    key match {
           |    $forKeys
           |    $forContaintedNodes
           |      case _ => PropertyErrorRegister.logPropertyErrorIfFirst(getClass, key)
           |    }
           |  }""".stripMargin
      }

      val propertyImpl: String = {
        def caseEntry(name: String, accessorName: String, cardinality: String) = {
          Cardinality.fromName(cardinality) match {
            case Cardinality.One | Cardinality.List =>
              s"""|      case "$name" => this._$accessorName"""
            case Cardinality.ZeroOrOne =>
              s"""|      case "$name" => this._$accessorName.orNull"""
          }
        }

        val forKeys = keys.map(key =>
          caseEntry(key.name, camelCase(key.name), key.cardinality)
        ).mkString("\n")

        val forContainedKeys = nodeType.containedNodesList.map(containedNode =>
          caseEntry(containedNode.localName ,containedNode.localName, containedNode.cardinality)
        ).mkString("\n")

        s"""override def property(key:String): AnyRef = {
           |    key match {
           |      $forKeys
           |      $forContainedKeys
           |      case _ => null
           |    }
           |  }""".stripMargin
      }

      val classImpl =
        s"""class $classNameDb(ref: NodeRef[NodeDb]) extends NodeDb(ref) with StoredNode
           |  $mixinTraits with ${className}Base {
           |
           |  override def layoutInformation: NodeLayoutInformation = $className.layoutInformation
           |
           |${propertyBasedFields(keys)}
           |$containedNodesAsMembers
           |
           |  /* all properties */
           |  override def valueMap: JMap[String, AnyRef] = $valueMapImpl
           |
           |  $neighborAccessors
           |
           |  override def label: String = {
           |    $className.Label
           |  }
           |
           |  override def productElementLabel(n: Int): String =
           |    n match {
           |      $productElementLabels
           |    }
           |
           |  override def productElement(n: Int): Any =
           |    n match {
           |      $productElementAccessors
           |    }
           |
           |  override def productPrefix = "$className"
           |  override def productArity = ${productElements.size}
           |
           |  override def canEqual(that: Any): Boolean = that != null && that.isInstanceOf[$classNameDb]
           |
           |  $propertyImpl
           |
           |$updateSpecificPropertyImpl
           |
           |override def removeSpecificProperty(key: String): Unit =
           |  this.updateSpecificProperty(key, null)
           |
           |$fromNew
           |
           |}""".stripMargin

      s"""$staticHeader
         |$companionObject
         |$nodeBaseImpl
         |$nodeRefImpl
         |$classImpl
         |${generatePropertyTraversals(className, keys)}
         |""".stripMargin
    }

    val baseDir = File(outputDir.getPath + "/" + nodesPackage.replaceAll("\\.", "/"))
    if (baseDir.exists) baseDir.delete()
    baseDir.createDirectories()
    baseDir.createChild("package.scala").write(packageObject)
    baseDir.createChild("RootTypes.scala").write(rootTypeImpl)
    schema.nodeBaseTraits.foreach { nodeBaseTrait =>
      val src = generateNodeBaseTypeSource(nodeBaseTrait)
      val srcFile = nodeBaseTrait.className + ".scala"
      baseDir.createChild(srcFile).write(src)
    }
    schema.nodeTypes.foreach { nodeType =>
      val src = generateNodeSource(nodeType, nodeType.keys.map(schema.nodePropertyByName))
      val srcFile = nodeType.className + ".scala"
      baseDir.createChild(srcFile).write(src)
    }
    println(s"generated node sources in $baseDir (${baseDir.list.size} files)")
    baseDir.toJava
  }

  /** generates classes to easily add new nodes to the graph
    * this ability could have been added to the existing nodes, but it turned out as a different specialisation,
    * since e.g. `id` is not set before adding it to the graph */
  def writeNewNodeFiles(outputDir: JFile): JFile = {
    val staticHeader =
      s"""package $nodesPackage
         |
         |import java.lang.{Boolean => JBoolean, Long => JLong}
         |import java.util.{Map => JMap, Set => JSet}
         |
         |/** base type for all nodes that can be added to a graph, e.g. the diffgraph */
         |trait NewNode extends CpgNode {
         |  def properties: Map[String, Any]
         |}
         |""".stripMargin

    def generateNewNodeSource(nodeType: NodeType, keys: List[Property]) = {
      var fieldDescriptions = List[(String, String, Option[String])]() // fieldName, type, default
      for (key <- keys) {
        val optionalDefault =
          if (getHigherType(key) == HigherValueType.Option) Some("None")
          else if (key.valueType == "int") Some("-1")
          else if (getHigherType(key) == HigherValueType.None && key.valueType == "string")
            Some("\"\"")
          else if (getHigherType(key) == HigherValueType.None && key.valueType == "boolean")
            Some("false")
          else if (getHigherType(key) == HigherValueType.List) Some("List()")
          else None
        val typ = getCompleteType(key)
        fieldDescriptions = (camelCase(key.name), typ, optionalDefault) :: fieldDescriptions
      }
      for (containedNode <- nodeType.containedNodesList) {
        val optionalDefault =
          Cardinality.fromName(containedNode.cardinality) match {
            case Cardinality.List      => Some("List()")
            case Cardinality.ZeroOrOne => Some("None")
            case _                     => None
          }
        val typ = getCompleteType(containedNode)
        fieldDescriptions = (containedNode.localName, typ, optionalDefault) :: fieldDescriptions
      }
      fieldDescriptions = fieldDescriptions.reverse
      val defaultsVal = fieldDescriptions
        .map {case (name, typ, Some(default)) => s"var $name: $typ = $default"
              case (name, typ, None)          => s"var $name: $typ"}
        .mkString(", ")

      val defaultsNoVal = fieldDescriptions
        .map {case (name, typ, Some(default)) => s"$name: $typ = $default"
              case (name, typ, None)          => s"$name: $typ"}
        .mkString(", ")

      val paramId = fieldDescriptions
        .map {case (name, _, _) => s"$name = $name"}
        .mkString(", ")

      val valueMapImpl = {
        val putKeysImpl = keys
          .map { key: Property =>
            val memberName = camelCase(key.name)
            Cardinality.fromName(key.cardinality) match {
              case Cardinality.One =>
                s"""  if ($memberName != null) { res += "${key.name}" -> $memberName }"""
              case Cardinality.ZeroOrOne =>
                s"""  if ($memberName != null && $memberName.isDefined) { res += "${key.name}" -> $memberName.get }"""
              case Cardinality.List =>
                s"""  if ($memberName != null && $memberName.nonEmpty) { res += "${key.name}" -> $memberName }"""
            }
          }
          .mkString("\n")
      val putRefsImpl = nodeType.containedNodesList.map { key =>
          val memberName = key.localName
          Cardinality.fromName(key.cardinality) match {
            case Cardinality.One =>
              s"""  if ($memberName != null) { res += "$memberName" -> $memberName }"""
            case Cardinality.ZeroOrOne =>
              s"""  if ($memberName != null && $memberName.isDefined) { res += "$memberName" -> $memberName.get }"""
            case Cardinality.List =>
              s"""  if ($memberName != null && $memberName.nonEmpty) { res += "$memberName" -> $memberName }"""
          }
        }
        .mkString("\n")


        s"""override def properties: Map[String, Any] = {
           |  var res = Map[String, Any]()
           |$putKeysImpl
           |$putRefsImpl
           |  res
           |}""".stripMargin
      }



      s"""object New${nodeType.className}{
         |  def apply(${defaultsNoVal}): New${nodeType.className} = new New${nodeType.className}($paramId)
         |
         |}
         |
         |// fixme: This should never have been a case class. Softly deprecate that.
         |case class New${nodeType.className}($defaultsVal) extends NewNode with ${nodeType.className}Base {
         |  override def label:String = "${nodeType.name}"
         |
         |  $valueMapImpl
         |}
         |""".stripMargin
    }


    val outfile = File(outputDir.getPath + "/" + nodesPackage.replaceAll("\\.", "/") + "/NewNodes.scala")
    if (outfile.exists) outfile.delete()
    outfile.createFile()
    val src = schema.nodeTypes.map { nodeType =>
      generateNewNodeSource(nodeType, nodeType.keys.map(schema.nodePropertyByName))
    }.mkString("\n")
    outfile.write(s"""$staticHeader
                     |$src
                     |""".stripMargin)
    println(s"generated NewNode sources in $outfile")
    outfile.toJava
  }

  /* surrounds input with `"` */
  def quoted(strings: Iterable[String]): Iterable[String] =
    strings.map(string => s""""$string"""")
}
