package overflowdb.codegen

import better.files._
import overflowdb.codegen.CodeGen.ConstantContext
import overflowdb.schema.Property.ValueType
import overflowdb.schema._

import scala.collection.mutable

/** Generates a domain model for OverflowDb traversals for a given domain-specific schema. */
class CodeGen(schema: Schema) {
  import Helpers._
  val basePackage = schema.basePackage
  val nodesPackage = s"$basePackage.nodes"
  val edgesPackage = s"$basePackage.edges"
  val traversalsPackage = s"$basePackage.traversal"
  private val noWarnList: mutable.Set[(AbstractNodeType, Property[_])] = mutable.Set.empty

  def dontWarnForDuplicateProperty(nodeType: AbstractNodeType, property: Property[_]): CodeGen = {
    noWarnList.add((nodeType, property))
    this
  }

  def run(outputDir: java.io.File): Seq[java.io.File] = {
    warnForDuplicatePropertyDefinitions()
    val _outputDir = outputDir.toScala
    val results =
      writeStarters(_outputDir) ++
      writeConstants(_outputDir) ++
      writeEdgeFiles(_outputDir) ++
      writeNodeFiles(_outputDir) ++
      writeNodeTraversalFiles(_outputDir) :+
      writeNewNodeFile(_outputDir)
    println(s"generated ${results.size} files in ${_outputDir}")

    results.map(_.toJava)
  }

  /* to provide feedback for potential schema optimisation: no need to redefine properties if they are already
   * defined in one of the parents */
  protected def warnForDuplicatePropertyDefinitions() = {
    val warnings = for {
      nodeType <- schema.allNodeTypes
      property <- nodeType.propertiesWithoutInheritance
      baseType <- nodeType.extendzRecursively
      if baseType.propertiesWithoutInheritance.contains(property) && !noWarnList.contains((nodeType, property))
    } yield s"[info]: $nodeType wouldn't need to have $property added explicitly - $baseType already brings it in"

    if (warnings.size > 0) println(s"${warnings.size} warnings found:")
    warnings.sorted.foreach(println)
  }

  protected def writeStarters(outputDir: File): Seq[File] = {
    val results = mutable.Buffer.empty[File]
    val baseDir = outputDir / basePackage.replaceAll("\\.", "/")
    baseDir.createDirectories()
    val domainShortName = schema.domainShortName

    val domainMain = baseDir.createChild(s"$domainShortName.scala").write(
      s"""package $basePackage
         |
         |import java.nio.file.{Path, Paths}
         |import overflowdb.traversal.help.TraversalHelp
         |import overflowdb.{Config, Graph}
         |import scala.jdk.javaapi.CollectionConverters.asJava
         |
         |object $domainShortName {
         |
         |  /**
         |    * Syntactic sugar for `new $domainShortName(graph)`.
         |    * Usage:
         |    *   `$domainShortName(graph)` or simply `$domainShortName` if you have an `implicit Graph` in scope
         |    */
         |  def apply(implicit graph: Graph) = new $domainShortName(graph)
         |
         |  def empty: $domainShortName =
         |    new $domainShortName(emptyGraph)
         |
         |  /**
         |    * Instantiate $domainShortName with storage.
         |    * If the storage file already exists, it will load (a subset of) the data into memory. Otherwise it will create an empty $domainShortName.
         |    * In either case, configuring storage means that OverflowDb will be stored to disk on shutdown (`close`).
         |    * I.e. if you want to preserve state between sessions, just use this method to instantiate the $domainShortName and ensure to properly `close` it at the end.
         |    * @param path to the storage file, e.g. /home/user1/overflowdb.bin
         |    */
         |  def withStorage(path: Path): $domainShortName =
         |    withConfig(Config.withoutOverflow.withStorageLocation(path))
         |
         |  def withStorage(path: String): $domainShortName =
         |    withStorage(Paths.get(path))
         |
         |  def withConfig(config: overflowdb.Config): $domainShortName =
         |    new $domainShortName(
         |      Graph.open(config, nodes.Factories.allAsJava, edges.Factories.allAsJava, convertPropertyForPersistence))
         |
         |  def emptyGraph: Graph =
         |    Graph.open(Config.withoutOverflow, nodes.Factories.allAsJava, edges.Factories.allAsJava, convertPropertyForPersistence)
         |
         |  def convertPropertyForPersistence(property: Any): Any =
         |    property match {
         |      case arraySeq: scala.collection.immutable.ArraySeq[_] => arraySeq.unsafeArray
         |      case coll: IterableOnce[Any] => asJava(coll.iterator.toArray)
         |      case other => other
         |    }
         |
         |}
         |
         |
         |/**
         |  * Domain-specific wrapper for graph, starting point for traversals.
         |  * @param graph the underlying graph. An empty graph is created if this parameter is omitted.
         |  */
         |class $domainShortName(val graph: Graph = $domainShortName.emptyGraph) extends AutoCloseable {
         |
         |  lazy val help: String =
         |    new TraversalHelp("$basePackage").forTraversalSources
         |
         |  override def close(): Unit =
         |    graph.close
         |}
         |
         |""".stripMargin
    )
    results.append(domainMain)

    results.toSeq
  }

  protected def writeConstants(outputDir: File): Seq[File] = {
    val results = mutable.Buffer.empty[File]
    val baseDir = outputDir / basePackage.replaceAll("\\.", "/")
    baseDir.createDirectories()

    def writeConstantsFile(className: String, constants: Seq[ConstantContext]): Unit = {
      val constantsSource = constants.map { constant =>
        val documentation = constant.documentation.filter(_.nonEmpty).map(comment => s"""/** $comment */""").getOrElse("")
        s""" $documentation
           | ${constant.source}
           |""".stripMargin
      }.mkString("\n")
      val allConstantsSetType = if (constantsSource.contains("PropertyKey")) "PropertyKey<?>" else "String"
      val allConstantsBody = constants.map { constant =>
        s"     add(${constant.name});"
      }.mkString("\n").stripSuffix("\n")
      val allConstantsSet =
        s"""
           | public static Set<$allConstantsSetType> ALL = new HashSet<$allConstantsSetType>() {{
           |$allConstantsBody
           | }};
           |""".stripMargin
      val file = baseDir.createChild(s"$className.java").write(
        s"""package $basePackage;
           |
           |import overflowdb.*;
           |
           |import java.util.Collection;
           |import java.util.HashSet;
           |import java.util.Set;
           |
           |public class $className {
           |
           |$constantsSource
           |$allConstantsSet
           |}""".stripMargin
      )
      results.append(file)
    }

    writeConstantsFile("PropertyNames", schema.properties.map { property =>
      ConstantContext(property.name, s"""public static final String ${property.name} = "${property.name}";""", property.comment)
    })
    writeConstantsFile("NodeTypes", schema.nodeTypes.map { nodeType =>
      ConstantContext(nodeType.name, s"""public static final String ${nodeType.name} = "${nodeType.name}";""", nodeType.comment)
    })
    writeConstantsFile("EdgeTypes", schema.edgeTypes.map { edgeType =>
      ConstantContext(edgeType.name, s"""public static final String ${edgeType.name} = "${edgeType.name}";""", edgeType.comment)
    })
    schema.constantsByCategory.foreach { case (category, constants) =>
      writeConstantsFile(category, constants.map { constant =>
        ConstantContext(constant.name, s"""public static final String ${constant.name} = "${constant.value}";""", constant.comment)
      })
    }

    writeConstantsFile("Properties", schema.properties.map { property =>
      val src = {
        val valueType = typeFor(property)
        val cardinality = property.cardinality
        import Property.Cardinality
        val completeType = cardinality match {
          case Cardinality.One(_) => valueType
          case Cardinality.ZeroOrOne => valueType
          case Cardinality.List => s"scala.collection.IndexedSeq<$valueType>"
        }
        s"""public static final overflowdb.PropertyKey<$completeType> ${property.name} = new overflowdb.PropertyKey<>("${property.name}");"""
      }
      ConstantContext(property.name, src, property.comment)
    })

    results.toSeq
  }

  protected def writeEdgeFiles(outputDir: File): Seq[File] = {
    val staticHeader =
      s"""package $edgesPackage
         |
         |import overflowdb._
         |import scala.jdk.CollectionConverters._
         |""".stripMargin

    val packageObject = {
      val factories = {
        val edgeFactories = schema.edgeTypes.map(edgeType => edgeType.className + ".factory").mkString(", ")
        s"""object Factories {
           |  lazy val all: Seq[EdgeFactory[_]] = Seq($edgeFactories)
           |  lazy val allAsJava: java.util.List[EdgeFactory[_]] = all.asJava
           |}
           |""".stripMargin
      }

      s"""$staticHeader
         |$propertyErrorRegisterImpl
         |$factories
         |""".stripMargin
    }

    def generateEdgeSource(edgeType: EdgeType, properties: Seq[Property[_]]) = {
      val edgeClassName = edgeType.className

      val propertyNames = properties.map(_.className)

      val propertyNameDefs = properties.map { p =>
        s"""val ${p.className} = "${p.name}" """
      }.mkString("\n|    ")

      val propertyDefinitions = properties.map { p =>
        propertyKeyDef(p.name, typeFor(p), p.cardinality)
      }.mkString("\n|    ")

      val companionObject =
        s"""object $edgeClassName {
           |  val Label = "${edgeType.name}"
           |
           |  object PropertyNames {
           |    $propertyNameDefs
           |    val all: Set[String] = Set(${propertyNames.mkString(", ")})
           |    val allAsJava: java.util.Set[String] = all.asJava
           |  }
           |
           |  object Properties {
           |    $propertyDefinitions
           |  }
           |
           |  object PropertyDefaults {
           |    ${propertyDefaultCases(properties)}
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

      def propertyBasedFieldAccessors(properties: Seq[Property[_]]): String = {
        import Property.Cardinality
        properties.map { property =>
          val name = property.name
          val nameCamelCase = camelCase(name)
          val tpe = getCompleteType(property)

          property.cardinality match {
            case Cardinality.One(_) =>
              s"""def $nameCamelCase: $tpe = property("$name").asInstanceOf[$tpe]"""
            case Cardinality.ZeroOrOne =>
              s"""def $nameCamelCase: $tpe = Option(property("$name")).asInstanceOf[$tpe]""".stripMargin
            case Cardinality.List =>
              val returnType = s"IndexedSeq[${typeFor(property)}]"
              s"""def $nameCamelCase: $tpe = {
                 |  property("$name") match {
                 |    case null => collection.immutable.ArraySeq.empty
                 |    case arr: Array[_] if arr.isEmpty => collection.immutable.ArraySeq.empty
                 |    case arr: Array[_] => scala.collection.immutable.ArraySeq.unsafeWrapArray(arr).asInstanceOf[$returnType]
                 |    case iterable: IterableOnce[_] => iterable.iterator.to(IndexedSeq).asInstanceOf[$returnType]
                 |    case jList: java.util.List[_] => jList.asScala.to(IndexedSeq).asInstanceOf[$returnType]
                 |  }
                 |}""".stripMargin
          }
        }.mkString("\n\n")
      }

      val propertyDefaultValues = propertyDefaultValueImpl(s"$edgeClassName.PropertyDefaults", properties)
      val classImpl =
        s"""class $edgeClassName(_graph: Graph, _outNode: NodeRef[NodeDb], _inNode: NodeRef[NodeDb])
           |extends Edge(_graph, $edgeClassName.Label, _outNode, _inNode, $edgeClassName.PropertyNames.allAsJava) {
           |
           |  ${propertyBasedFieldAccessors(properties)}
           |
           |  $propertyDefaultValues
           |
           |}
           |""".stripMargin

      s"""$staticHeader
         |$companionObject
         |$classImpl
         |""".stripMargin
    }

    val baseDir = outputDir / edgesPackage.replaceAll("\\.", "/")
    if (baseDir.exists) baseDir.delete()
    baseDir.createDirectories()
    val pkgObjFile = baseDir.createChild("package.scala").write(packageObject)
    val edgeTypeFiles = schema.edgeTypes.map { edge =>
      val src = generateEdgeSource(edge, edge.properties)
      val srcFile = edge.className + ".scala"
      baseDir.createChild(srcFile).write(src)
    }
    pkgObjFile +: edgeTypeFiles
  }

  protected def neighborAccessorNameForEdge(edge: EdgeType, direction: Direction.Value): String =
    camelCase(edge.name + "_" + direction)

  protected def writeNodeFiles(outputDir: File): Seq[File] = {
    val rootTypeImpl = {
      val genericNeighborAccessors = for {
        direction <- Direction.all
        edgeType <- schema.edgeTypes
        accessor = neighborAccessorNameForEdge(edgeType, direction)
      } yield s"def _$accessor: java.util.Iterator[StoredNode] = { java.util.Collections.emptyIterator() }"

      val keyBasedTraits =
        schema.nodeProperties.map { property =>
          val camelCaseName = camelCase(property.name)
          val tpe = getCompleteType(property)
          val traitName = s"Has${property.className}"
          s"""trait $traitName {
             |  def $camelCaseName: $tpe
             |}
             |trait ${traitName}Mutable extends $traitName {
             |  def ${camelCaseName}_=(value: $tpe)
             |}
             |""".stripMargin

        }.mkString("\n") + "\n"

      val factories = {
        val nodeFactories =
          schema.nodeTypes.map(nodeType => nodeType.className + ".factory").mkString(", ")
        s"""object Factories {
           |  lazy val all: Seq[NodeFactory[_]] = Seq($nodeFactories)
           |  lazy val allAsJava: java.util.List[NodeFactory[_]] = all.asJava
           |}
           |""".stripMargin
      }
      val reChars = "[](){}*+&|?.,\\\\$"
      s"""package $nodesPackage
         |
         |import overflowdb._
         |import scala.jdk.CollectionConverters._
         |
         |$propertyErrorRegisterImpl
         |
         |object Misc {
         |  val reChars = "$reChars"
         |  def isRegex(pattern: String): Boolean = pattern.exists(reChars.contains(_))
         |}
         |
         |/** Abstract supertype for overflowdb.Node and NewNode */
         |trait AbstractNode {
         |  def label: String
         |}
         |
         |/* A node that is stored inside an Graph (rather than e.g. DiffGraph) */
         |trait StoredNode extends Node with AbstractNode with Product {
         |  /* underlying Node in the graph.
         |   * since this is a StoredNode, this is always set */
         |  def underlying: Node = this
         |
         |  /** labels of product elements, used e.g. for pretty-printing */
         |  def productElementLabel(n: Int): String
         |
         |  /* all properties plus label and id */
         |  def toMap: Map[String, Any] = {
         |    val map = propertiesMap()
         |    map.put("_label", label)
         |    map.put("_id", id: java.lang.Long)
         |    map.asScala.toMap
         |  }
         |
         |  /*Sets fields from newNode*/
         |  def fromNewNode(newNode: NewNode, mapping: NewNode => StoredNode):Unit = ???
         |
         |  ${genericNeighborAccessors.mkString("\n")}
         |}
         |
         |  $keyBasedTraits
         |  $factories
         |""".stripMargin
    }

    val staticHeader =
      s"""package $nodesPackage
         |
         |import overflowdb._
         |import scala.jdk.CollectionConverters._
         |""".stripMargin

    def generateNodeBaseTypeSource(nodeBaseType: NodeBaseType): String = {
      val className = nodeBaseType.className
      val properties = nodeBaseType.properties

      val mixins = nodeBaseType.properties.map { property =>
        s"with Has${property.className}"
      }.mkString(" ")

      val mixinTraits = nodeBaseType.extendz.map { baseTrait =>
        s"with ${baseTrait.className}"
      }.mkString(" ")

      val mixinsNew = nodeBaseType.properties.map { property =>
        s"with Has${property.className}Mutable"
      }.mkString(" ")

      val mixinTraitsNew = nodeBaseType.extendz.map { baseTrait =>
        s"with ${baseTrait.className}New"
      }.mkString(" ")

      val mixinTraitsForBase = nodeBaseType.extendz.map { baseTrait =>
        s"with ${baseTrait.className}Base"
      }.mkString(" ")

      def abstractEdgeAccessors(nodeBaseType: NodeBaseType, direction: Direction.Value) = {
        nodeBaseType.edges(direction).groupBy(_.viaEdge).map { case (edge, neighbors) =>
          val edgeAccessorName = neighborAccessorNameForEdge(edge, direction)
          /** TODO bring this back, but not as direct accessors on the type, but via extension methods
            * context: in complex schema hierarchies, type inheritance between base nodes
            * and nodes can lead to very convoluted accessor types. E.g. in TestSchema03c in the integration tests:
            *
            * AbstractNode1.edge1In: Traversal[AbstractNode1]
            * Node1.edge1In:         Traversal[AbstractNode1]
            * Node2.edge1In:         Traversal[NodeExt]
            *
            * which is technically correct based on the schema, but the JVM doesn't allow this because
            * both Node1 and Node2 extend AbstractNode, and therefor Node2.edge1In doesn't compile
            */
//          val neighborNodesType = {
//            val subtypesWithSameEdgeAndDirection =
//              nodeBaseType.subtypes(schema.allNodeTypes.toSet)
//                .flatMap(_.edges(direction).filter(_.viaEdge == edge))
//
//            val relevantNeighbors = (neighbors ++ subtypesWithSameEdgeAndDirection).map(_.neighbor).toSet
//            deriveCommonRootType(relevantNeighbors)
//          }
          val neighborNodesType = "_ <: StoredNode"
          val genericEdgeAccessor = s"def $edgeAccessorName: overflowdb.traversal.Traversal[$neighborNodesType]"

          val specificNodeAccessors = neighbors.flatMap { adjacentNode =>
            val neighbor = adjacentNode.neighbor
            val entireNodeHierarchy: Set[AbstractNodeType] = neighbor.subtypes(schema.allNodeTypes.toSet) ++ (neighbor.extendzRecursively :+ neighbor)
            entireNodeHierarchy.map { neighbor =>
              val accessorName = s"_${camelCase(neighbor.name)}Via${edge.className.capitalize}${camelCaseCaps(direction.toString)}"
              val cardinality = adjacentNode.cardinality
              val appendix = cardinality match {
                case EdgeType.Cardinality.One => ".next()"
                case EdgeType.Cardinality.ZeroOrOne => s".nextOption()"
                case _ => ""
              }
              s"def $accessorName: ${fullScalaType(neighbor, cardinality)} = $edgeAccessorName.collectAll[${neighbor.className}]$appendix"
            }
          }.distinct.mkString("\n\n")

          s"""$genericEdgeAccessor
             |
             |$specificNodeAccessors""".stripMargin
        }.mkString("\n")
      }


      val companionObject = {
        val propertyNames = nodeBaseType.properties.map(_.name)
        val propertyNameDefs = propertyNames.map { name =>
          s"""val ${camelCaseCaps(name)} = "$name" """
        }.mkString("\n|    ")

        val propertyDefinitions = properties.map { p =>
          propertyKeyDef(p.name, typeFor(p), p.cardinality)
        }.mkString("\n|    ")

        val Seq(outEdgeNames, inEdgeNames) =
          Seq(nodeBaseType.outEdges, nodeBaseType.inEdges).map { edges =>
            edges.map(_.viaEdge.name).sorted.map(quote).mkString(",")
          }

        s"""object $className {
           |  object PropertyNames {
           |    $propertyNameDefs
           |    val all: Set[String] = Set(${propertyNames.map(camelCaseCaps).mkString(", ")})
           |  }
           |
           |  object Properties {
           |    $propertyDefinitions
           |  }
           |
           |  object PropertyDefaults {
           |    ${propertyDefaultCases(properties)}
           |  }
           |
           |  object Edges {
           |    val Out: Array[String] = Array($outEdgeNames)
           |    val In: Array[String] = Array($inEdgeNames)
           |  }
           |
           |}""".stripMargin
      }

      s"""package $nodesPackage
         |
         |$companionObject
         |
         |trait ${className}Base extends AbstractNode
         |$mixins
         |$mixinTraitsForBase
         |
         |trait ${className}New extends NewNode
         |$mixinsNew
         |$mixinTraitsNew
         |
         |trait $className extends StoredNode with ${className}Base
         |$mixinTraits {
         |${abstractEdgeAccessors(nodeBaseType, Direction.OUT)}
         |${abstractEdgeAccessors(nodeBaseType, Direction.IN)}
         |}""".stripMargin
    }

    def generateNodeSource(nodeType: NodeType) = {
      val properties = nodeType.properties

      val propertyNames = (properties.map(_.name) ++ nodeType.containedNodes.map(_.localName)).distinct
      val propertyNameDefs = propertyNames.map { name =>
        s"""val ${camelCaseCaps(name)} = "$name" """
      }.mkString("\n|    ")

      val propertyDefs = properties.map { p =>
        propertyKeyDef(p.name, typeFor(p), p.cardinality)
      }.mkString("\n|    ")

      val propertyDefsForContainedNodes = nodeType.containedNodes.map { containedNode =>
        propertyKeyDef(containedNode.localName, containedNode.nodeType.className, containedNode.cardinality)
      }.mkString("\n|    ")

      val (neighborOutInfos, neighborInInfos) = {
        /** the offsetPos determines the index into the adjacent nodes array of a given node type
          * assigning numbers here must follow the same way as in NodeLayoutInformation, i.e. starting at 0,
          * first assign ids to the outEdges based on their order in the list, and then the same for inEdges */
        var _currOffsetPos = -1
        def nextOffsetPos: Int = { _currOffsetPos += 1; _currOffsetPos }

        case class AjacentNodeWithInheritanceStatus(adjacentNode: AdjacentNode, isInherited: Boolean)

        /** For all adjacent nodes, figure out if they are inherited or not.
          *  Later on, we will create edge accessors for all inherited neighbors, but only create the node accessors
          *  on the base types (if they are defined there). Note: they may even be inherited with a different cardinality */
        def adjacentNodesWithInheritanceStatus(adjacentNodes: AbstractNodeType => Seq[AdjacentNode]): Seq[AjacentNodeWithInheritanceStatus] = {
          val inherited = nodeType.extendzRecursively
            .flatMap(adjacentNodes)
            .map(AjacentNodeWithInheritanceStatus(_, true))

          // only edge and neighbor node matter, not the cardinality
          val inheritedLookup: Set[(EdgeType, AbstractNodeType)] =
            inherited.map(_.adjacentNode).map { case AdjacentNode(viaEdge, neighbor, _) => (viaEdge, neighbor) }.toSet

          val direct = adjacentNodes(nodeType).map { adjacentNode =>
            val isInherited = inheritedLookup.contains((adjacentNode.viaEdge, adjacentNode.neighbor))
            AjacentNodeWithInheritanceStatus(adjacentNode, isInherited)
          }
          (direct ++ inherited).distinct
        }

        def createNeighborInfos(neighborContexts: Seq[AjacentNodeWithInheritanceStatus], direction: Direction.Value): Seq[NeighborInfoForEdge] = {
          neighborContexts.groupBy(_.adjacentNode.viaEdge).map { case (edge, neighborContexts) =>
            val neighborInfoForNodes = neighborContexts.map { case AjacentNodeWithInheritanceStatus(adjacentNode, isInherited) =>
              NeighborInfoForNode(adjacentNode.neighbor, edge, direction, adjacentNode.cardinality, isInherited)
            }
            NeighborInfoForEdge(edge, neighborInfoForNodes, nextOffsetPos)
          }.toSeq
        }

        val neighborOutInfos = createNeighborInfos(adjacentNodesWithInheritanceStatus(_.outEdges), Direction.OUT)
        val neighborInInfos = createNeighborInfos(adjacentNodesWithInheritanceStatus(_.inEdges), Direction.IN)
        (neighborOutInfos, neighborInInfos)
      }

      val neighborInfos: Seq[(NeighborInfoForEdge, Direction.Value)] =
        neighborOutInfos.map((_, Direction.OUT)) ++ neighborInInfos.map((_, Direction.IN))

      def toLayoutInformationEntry(neighborInfos: Seq[NeighborInfoForEdge]): String = {
        neighborInfos.sortBy(_.offsetPosition).map { neighborInfo =>
          val edgeClass = neighborInfo.edge.className
          s"$edgesPackage.$edgeClass.layoutInformation"
        }.mkString(",\n")
      }
      val List(outEdgeLayouts, inEdgeLayouts) = List(neighborOutInfos, neighborInInfos).map(toLayoutInformationEntry)

      val className = nodeType.className
      val classNameDb = nodeType.classNameDb

      val companionObject =
        s"""object $className {
           |  def apply(graph: Graph, id: Long) = new $className(graph, id)
           |
           |  val Label = "${nodeType.name}"
           |
           |  object PropertyNames {
           |    $propertyNameDefs
           |    val all: Set[String] = Set(${propertyNames.map(camelCaseCaps).mkString(", ")})
           |    val allAsJava: java.util.Set[String] = all.asJava
           |  }
           |
           |  object Properties {
           |    $propertyDefs
           |    $propertyDefsForContainedNodes
           |  }
           |
           |  object PropertyDefaults {
           |    ${propertyDefaultCases(properties)}
           |  }
           |
           |  val layoutInformation = new NodeLayoutInformation(
           |    Label,
           |    PropertyNames.allAsJava,
           |    List($outEdgeLayouts).asJava,
           |    List($inEdgeLayouts).asJava)
           |
           |
           |  object Edges {
           |    val Out: Array[String] = Array(${quoted(neighborOutInfos.map(_.edge.name).sorted).mkString(",")})
           |    val In: Array[String] = Array(${quoted(neighborInInfos.map(_.edge.name).sorted).mkString(",")})
           |  }
           |
           |  val factory = new NodeFactory[$classNameDb] {
           |    override val forLabel = $className.Label
           |
           |    override def createNode(ref: NodeRef[$classNameDb]) =
           |      new $classNameDb(ref.asInstanceOf[NodeRef[NodeDb]])
           |
           |    override def createNodeRef(graph: Graph, id: Long) = $className(graph, id)
           |  }
           |}
           |""".stripMargin

      val mixinTraits: String =
        nodeType.extendz
          .map { traitName =>
            s"with ${traitName.className}"
          }
          .mkString(" ")

      val mixinTraitsForBase: String =
        nodeType.extendz
          .map { traitName =>
            s"with ${traitName.className}Base"
          }
          .mkString(" ")

      val propertyBasedTraits = properties.map(p => s"with Has${p.className}").mkString(" ")

      val propertiesMapImpl = {
        import Property.Cardinality
        val putKeysImpl = properties.map { key =>
          val memberName = camelCase(key.name)
          key.cardinality match {
            case Cardinality.One(_) =>
              s"""properties.put("${key.name}", $memberName)"""
            case Cardinality.ZeroOrOne =>
              s"""$memberName.map { value => properties.put("${key.name}", value) }"""
            case Cardinality.List =>
              s"""if (this._$memberName != null && this._$memberName.nonEmpty) { properties.put("${key.name}", $memberName) }"""
          }
        }.mkString("\n")

        val putContainedNodesImpl = {
          nodeType.containedNodes.map { cnt =>
            val memberName = cnt.localName
            cnt.cardinality match {
              case Cardinality.One(_) =>
                s"""properties.put("$memberName", this._$memberName)"""
              case Cardinality.ZeroOrOne =>
                s"""   $memberName.map { value => properties.put("$memberName", value) }"""
              case Cardinality.List =>
                s"""  if (this._$memberName != null && this._$memberName.nonEmpty) { properties.put("$memberName", this.$memberName) }"""
            }
          }
        }.mkString("\n")

        s""" {
           |  val properties = new java.util.HashMap[String, Any]
           |$putKeysImpl
           |$putContainedNodesImpl
           |  properties
           |}""".stripMargin
      }

      val propertiesMapForStorageImpl = {
        import Property.Cardinality
        val putKeysImpl = properties.map { key =>
          val memberName = camelCase(key.name)
          key.cardinality match {
            case Cardinality.One(default) =>
              val isDefaultValueImpl = defaultValueCheckImpl(memberName, default)
              s"""if (!($isDefaultValueImpl)) { properties.put("${key.name}", $memberName) }"""
            case Cardinality.ZeroOrOne =>
              s"""$memberName.map { value => properties.put("${key.name}", value) }"""
            case Cardinality.List =>
              s"""if (this._$memberName != null && this._$memberName.nonEmpty) { properties.put("${key.name}", $memberName) }"""
          }
        }.mkString("\n")

        val putContainedNodesImpl = {
          nodeType.containedNodes.map { cnt =>
            val memberName = cnt.localName
            cnt.cardinality match {
              case Cardinality.One(default) =>
                val isDefaultValueImpl = defaultValueCheckImpl(s"this._$memberName", default)
                s"""   if (!($isDefaultValueImpl)) { properties.put("$memberName", this._$memberName) }"""
              case Cardinality.ZeroOrOne =>
                s"""   $memberName.map { value => properties.put("$memberName", value) }"""
              case Cardinality.List =>
                s"""  if (this._$memberName != null && this._$memberName.nonEmpty) { properties.put("$memberName", this.$memberName) }"""
            }
          }
        }.mkString("\n")


        s""" {
           |  val properties = new java.util.HashMap[String, Any]
           |$putKeysImpl
           |$putContainedNodesImpl
           |  properties
           |}""".stripMargin
      }

      val fromNew = {
        val newNodeCasted = s"newNode.asInstanceOf[New${nodeType.className}]"

        val initKeysImpl = {
          val lines = properties.map { key =>
            import Property.Cardinality
            val memberName = camelCase(key.name)
            key.cardinality match {
              case Cardinality.One(_) =>
                s"""   this._$memberName = $newNodeCasted.$memberName""".stripMargin
              case Cardinality.ZeroOrOne =>
                s"""   this._$memberName = $newNodeCasted.$memberName.orNull""".stripMargin
              case Cardinality.List =>
                s"""   this._$memberName = if ($newNodeCasted.$memberName != null) $newNodeCasted.$memberName else collection.immutable.ArraySeq.empty""".stripMargin
            }
          }
          lines.mkString("\n")
        }

        val initRefsImpl = {
          nodeType.containedNodes.map { containedNode =>
            import Property.Cardinality
            val memberName = containedNode.localName
            val containedNodeType = containedNode.nodeType.className

            containedNode.cardinality match {
              case Cardinality.One(_) =>
                s"""  this._$memberName = $newNodeCasted.$memberName match {
                   |    case null => null
                   |    case newNode: NewNode => mapping(newNode).asInstanceOf[$containedNodeType]
                   |    case oldNode: StoredNode => oldNode.asInstanceOf[$containedNodeType]
                   |    case _ => throw new MatchError("unreachable")
                   |  }""".stripMargin
              case Cardinality.ZeroOrOne =>
                s"""  this._$memberName = $newNodeCasted.$memberName match {
                   |    case null | None => null
                   |    case Some(newNode:NewNode) => mapping(newNode).asInstanceOf[$containedNodeType]
                   |    case Some(oldNode:StoredNode) => oldNode.asInstanceOf[$containedNodeType]
                   |    case _ => throw new MatchError("unreachable")
                   |  }""".stripMargin
              case Cardinality.List =>
                s"""  this._$memberName =
                   |    if ($newNodeCasted.$memberName == null || $newNodeCasted.$memberName.isEmpty) {
                   |      collection.immutable.ArraySeq.empty
                   |    } else {
                   |     collection.immutable.ArraySeq.unsafeWrapArray(
                   |       $newNodeCasted.$memberName.map {
                   |         case null => throw new NullPointerException("NullPointers forbidden in contained nodes")
                   |         case newNode:NewNode => mapping(newNode).asInstanceOf[$containedNodeType]
                   |         case oldNode:StoredNode => oldNode.asInstanceOf[$containedNodeType]
                   |         case _ => throw new MatchError("unreachable")
                   |       }.toArray
                   |     )
                   |    }
                   |""".stripMargin
            }
          }
        }.mkString("\n\n")

        val registerFullName = if(!properties.map{_.name}.contains("FULL_NAME")) "" else {
          s"""  graph.indexManager.putIfIndexed("FULL_NAME", $newNodeCasted.fullName, this.ref)"""
        }

        s"""override def fromNewNode(newNode: NewNode, mapping: NewNode => StoredNode):Unit = {
           |$initKeysImpl
           |$initRefsImpl
           |$registerFullName
           |}""".stripMargin
      }

      val containedNodesAsMembers =
        nodeType.containedNodes
          .map { containedNode =>
            import Property.Cardinality
            val containedNodeType = containedNode.nodeType.className
            containedNode.cardinality match {
              case Cardinality.One(default) =>
                s"""
                   |private var _${containedNode.localName}: $containedNodeType = ${defaultValueImpl(default)}
                   |def ${containedNode.localName}: $containedNodeType = this._${containedNode.localName}
                   |""".stripMargin
              case Cardinality.ZeroOrOne =>
                s"""
                   |private var _${containedNode.localName}: $containedNodeType = null
                   |def ${containedNode.localName}: Option[$containedNodeType] = Option(this._${containedNode.localName})
                   |""".stripMargin
              case Cardinality.List =>
                s"""
                   |private var _${containedNode.localName}: IndexedSeq[$containedNodeType] = collection.immutable.ArraySeq.empty
                   |def ${containedNode.localName}: IndexedSeq[$containedNodeType] = this._${containedNode.localName}
                   |""".stripMargin
            }
          }
          .mkString("\n")

      val productElements: Seq[ProductElement] = {
        var currIndex = -1
        def nextIdx = { currIndex += 1; currIndex }
        val forId = ProductElement("id", "id", nextIdx)
        val forKeys = properties.map { key =>
          val name = camelCase(key.name)
          ProductElement(name, name, nextIdx)
        }
        val forContainedNodes = nodeType.containedNodes.map { containedNode =>
          ProductElement(
            containedNode.localName,
            containedNode.localName,
            nextIdx)
        }
        forId +: (forKeys ++ forContainedNodes)
      }

      val productElementLabels =
        productElements.map { case ProductElement(name, _, index) =>
          s"""case $index => "$name" """
        }.mkString("\n")

      val productElementAccessors =
        productElements.map { case ProductElement(_, accessorSrc, index) =>
          s"case $index => $accessorSrc"
        }.mkString("\n")

      val abstractContainedNodeAccessors = nodeType.containedNodes.map { containedNode =>
        s"""def ${containedNode.localName}: ${getCompleteType(containedNode)}"""
      }.mkString("\n")

      val delegatingContainedNodeAccessors = nodeType.containedNodes.map { containedNode =>
        import Property.Cardinality
        containedNode.cardinality match {
          case Cardinality.One(_) =>
            s"""  def ${containedNode.localName}: ${containedNode.nodeType.className} = get().${containedNode.localName}"""
          case Cardinality.ZeroOrOne =>
            s"""  def ${containedNode.localName}: Option[${containedNode.nodeType.className}] = get().${containedNode.localName}"""
          case Cardinality.List =>
            s"""  def ${containedNode.localName}: collection.immutable.IndexedSeq[${containedNode.nodeType.className}] = get().${containedNode.localName}"""
        }
      }.mkString("\n")

      val nodeBaseImpl =
        s"""trait ${className}Base extends AbstractNode $mixinTraitsForBase $propertyBasedTraits {
           |  def asStored : StoredNode = this.asInstanceOf[StoredNode]
           |
           |  $abstractContainedNodeAccessors
           |}
           |""".stripMargin

      val neighborAccessorDelegators = neighborInfos.map { case (neighborInfo, direction) =>
        val edgeAccessorName = neighborAccessorNameForEdge(neighborInfo.edge, direction)
        val nodeDelegators = neighborInfo.nodeInfos.collect {
          case neighborNodeInfo if !neighborNodeInfo.isInherited =>
            val accessorNameForNode = neighborNodeInfo.accessorName
            s"def $accessorNameForNode: ${neighborNodeInfo.returnType} = get().$accessorNameForNode"
        }.mkString("\n")

        s"""def $edgeAccessorName = get().$edgeAccessorName
           |override def _$edgeAccessorName = get()._$edgeAccessorName
           |$nodeDelegators
           |""".stripMargin
      }.mkString("\n")

      val nodeRefImpl = {
        val propertyDelegators = properties.map { key =>
          val name = camelCase(key.name)
          s"""  override def $name: ${getCompleteType(key)} = get().$name"""
        }.mkString("\n")

        val propertyDefaultValues = propertyDefaultValueImpl(s"$className.PropertyDefaults", properties)

        s"""class $className(graph: Graph, id: Long) extends NodeRef[$classNameDb](graph, id)
           |  with ${className}Base
           |  with StoredNode
           |  $mixinTraits {
           |  $propertyDelegators
           |  $propertyDefaultValues
           |  $delegatingContainedNodeAccessors
           |  $neighborAccessorDelegators
           |
           |  override def fromNewNode(newNode: NewNode, mapping: NewNode => StoredNode): Unit = get().fromNewNode(newNode, mapping)
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

      val neighborAccessors = neighborInfos.map { case (neighborInfo, direction) =>
        val edgeAccessorName = neighborAccessorNameForEdge(neighborInfo.edge, direction)
        val neighborType = neighborInfo.deriveNeighborNodeType
        val offsetPosition = neighborInfo.offsetPosition

        val nodeAccessors = neighborInfo.nodeInfos.collect {
          case neighborNodeInfo if !neighborNodeInfo.isInherited =>
            val appendix = neighborNodeInfo.consolidatedCardinality match {
              case EdgeType.Cardinality.One => ".next()"
              case EdgeType.Cardinality.ZeroOrOne => s".nextOption()"
              case _ => ""
            }
            s"def ${neighborNodeInfo.accessorName}: ${neighborNodeInfo.returnType} = $edgeAccessorName.collectAll[${neighborNodeInfo.neighborNode.className}]$appendix"
        }.mkString("\n")

        s"""def $edgeAccessorName: overflowdb.traversal.Traversal[$neighborType] = overflowdb.traversal.Traversal(createAdjacentNodeIteratorByOffSet[$neighborType]($offsetPosition))
           |override def _$edgeAccessorName = createAdjacentNodeIteratorByOffSet[StoredNode]($offsetPosition)
           |$nodeAccessors
           |""".stripMargin
      }.mkString("\n")

      val updateSpecificPropertyImpl: String = {
        import Property.Cardinality
        def caseEntry(name: String, accessorName: String, cardinality: Cardinality, baseType: String) = {
          val setter = cardinality match {
            case Cardinality.One(_) | Cardinality.ZeroOrOne =>
              s"value.asInstanceOf[$baseType]"
            case Cardinality.List =>
              s"""value match {
                 |        case null => collection.immutable.ArraySeq.empty
                 |        case singleValue: $baseType => collection.immutable.ArraySeq(singleValue)
                 |        case coll: IterableOnce[Any] if coll.iterator.isEmpty => collection.immutable.ArraySeq.empty
                 |        case arr: Array[_] if arr.isEmpty => collection.immutable.ArraySeq.empty
                 |        case arr: Array[_] => collection.immutable.ArraySeq.unsafeWrapArray(arr).asInstanceOf[IndexedSeq[$baseType]]
                 |        case jCollection: java.lang.Iterable[_]  =>
                 |          if (jCollection.iterator.hasNext) {
                 |            collection.immutable.ArraySeq.unsafeWrapArray(
                 |              jCollection.asInstanceOf[java.util.Collection[$baseType]].iterator.asScala.toArray)
                 |          } else collection.immutable.ArraySeq.empty
                 |        case iter: Iterable[_] =>
                 |          if(iter.nonEmpty) {
                 |            collection.immutable.ArraySeq.unsafeWrapArray(iter.asInstanceOf[Iterable[$baseType]].toArray)
                 |          } else collection.immutable.ArraySeq.empty
                 |      }""".stripMargin
          }
          s"""|      case "$name" => this._$accessorName = $setter"""
        }

        val forKeys = properties.map(p => caseEntry(p.name, camelCase(p.name), p.cardinality, typeFor(p))).mkString("\n")

        val forContainedNodes = nodeType.containedNodes.map(containedNode =>
          caseEntry(containedNode.localName, containedNode.localName, containedNode.cardinality, containedNode.nodeType.className)
        ).mkString("\n")

        s"""  override protected def updateSpecificProperty(key:String, value: Object): Unit = {
           |    key match {
           |    $forKeys
           |    $forContainedNodes
           |      case _ => PropertyErrorRegister.logPropertyErrorIfFirst(getClass, key)
           |    }
           |  }""".stripMargin
      }

      val propertyImpl: String = {
        val forKeys = properties.map(key =>
          s"""|      case "${key.name}" => this._${camelCase(key.name)}"""
        ).mkString("\n")

        val forContainedKeys = nodeType.containedNodes.map{containedNode =>
          val name = containedNode.localName
          s"""|      case "$name" => this._$name"""
        }.mkString("\n")

        s"""override def property(key:String): Any = {
           |    key match {
           |      $forKeys
           |      $forContainedKeys
           |      case _ => null
           |    }
           |  }""".stripMargin
      }

      def propertyBasedFields(properties: Seq[Property[_]]): String = {
        import Property.Cardinality
        properties.map { property =>
          val publicName = camelCase(property.name)
          val fieldName = s"_$publicName"
          val (publicType, tpeForField, fieldAccessor, defaultValue) = {
            val valueType = typeFor(property)
            property.cardinality match {
              case Cardinality.One(_)  =>
                (valueType, valueType, fieldName, s"$className.PropertyDefaults.${property.className}")
              case Cardinality.ZeroOrOne => (s"Option[$valueType]", valueType, s"Option($fieldName)", "null")
              case Cardinality.List   => (s"IndexedSeq[$valueType]", s"IndexedSeq[$valueType]", fieldName, "collection.immutable.ArraySeq.empty")
            }
          }

          s"""private var $fieldName: $tpeForField = $defaultValue
             |def $publicName: $publicType = $fieldAccessor""".stripMargin
        }.mkString("\n\n")
      }

      val classImpl =
        s"""class $classNameDb(ref: NodeRef[NodeDb]) extends NodeDb(ref) with StoredNode
           |  $mixinTraits with ${className}Base {
           |
           |  override def layoutInformation: NodeLayoutInformation = $className.layoutInformation
           |
           |${propertyBasedFields(properties)}
           |
           |$containedNodesAsMembers
           |
           |  /** faster than the default implementation */
           |  override def propertiesMap: java.util.Map[String, Any] =
           |    $propertiesMapImpl
           |
           |  /** faster than the default implementation */
           |  override def propertiesMapForStorage: java.util.Map[String, Any] =
           |    $propertiesMapForStorageImpl
           |
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
         |""".stripMargin
    }

    val results = mutable.Buffer.empty[File]
    val baseDir = outputDir / nodesPackage.replaceAll("\\.", "/")
    if (baseDir.exists) baseDir.delete()
    baseDir.createDirectories()
    results.append(baseDir.createChild("RootTypes.scala").write(rootTypeImpl))
    schema.nodeBaseTypes.foreach { nodeBaseTrait =>
      val src = generateNodeBaseTypeSource(nodeBaseTrait)
      val srcFile = nodeBaseTrait.className + ".scala"
      results.append(baseDir.createChild(srcFile).write(src))
    }
    schema.nodeTypes.foreach { nodeType =>
      val src = generateNodeSource(nodeType)
      val srcFile = nodeType.className + ".scala"
      results.append(baseDir.createChild(srcFile).write(src))
    }
    results.toSeq
  }

  protected def writeNodeTraversalFiles(outputDir: File): Seq[File] = {
    val staticHeader =
      s"""package $traversalsPackage
         |
         |import overflowdb.traversal.Traversal
         |import $nodesPackage._
         |""".stripMargin

    lazy val nodeTraversalImplicits = {
      def implicitForNodeType(name: String) = {
        val traversalName = s"${name}TraversalExtGen"
        s"implicit def to$traversalName[NodeType <: $name](trav: Traversal[NodeType]): ${traversalName}[NodeType] = new $traversalName(trav)"
      }

      val implicitsForNodeTraversals =
        schema.nodeTypes.map(_.className).sorted.map(implicitForNodeType).mkString("\n")

      val implicitsForNodeBaseTypeTraversals =
        schema.nodeBaseTypes.map(_.className).sorted.map(implicitForNodeType).mkString("\n")

      s"""package $traversalsPackage
         |
         |import overflowdb.traversal.Traversal
         |import $nodesPackage._
         |
         |trait NodeTraversalImplicits extends NodeBaseTypeTraversalImplicits {
         |  $implicitsForNodeTraversals
         |}
         |
         |// lower priority implicits for base types
         |trait NodeBaseTypeTraversalImplicits {
         |  $implicitsForNodeBaseTypeTraversals
         |}
         |""".stripMargin
    }

    def generatePropertyTraversals(className: String, properties: Seq[Property[_]]): String = {
      import Property.Cardinality
      val propertyTraversals = properties.map { property =>
        val nameCamelCase = camelCase(property.name)
        val baseType = typeFor(property)
        val cardinality = property.cardinality

        val mapOrFlatMap = cardinality match {
          case Cardinality.One(_) => "map"
          case Cardinality.ZeroOrOne | Cardinality.List => "flatMap"
        }

        val filterStepsForSingleString =
          s"""  /**
             |    * Traverse to nodes where the $nameCamelCase matches the regular expression `value`
             |    * */
             |  def $nameCamelCase(pattern: $baseType): Traversal[NodeType] = {
             |    if(!Misc.isRegex(pattern)){
             |      traversal.filter{node => node.${nameCamelCase} == pattern}
             |    } else {
             |    val matcher = java.util.regex.Pattern.compile(pattern).matcher("")
             |    traversal.filter{node =>  matcher.reset(node.$nameCamelCase); matcher.matches()}
             |    }
             |  }
             |
             |  /**
             |    * Traverse to nodes where the $nameCamelCase matches at least one of the regular expressions in `values`
             |    * */
             |  def $nameCamelCase(patterns: $baseType*): Traversal[NodeType] = {
             |    val matchers = patterns.map{pattern => java.util.regex.Pattern.compile(pattern).matcher("")}.toArray
             |    traversal.filter{node => matchers.exists{ matcher => matcher.reset(node.$nameCamelCase); matcher.matches()}}
             |   }
             |
             |  /**
             |    * Traverse to nodes where $nameCamelCase matches `value` exactly.
             |    * */
             |  def ${nameCamelCase}Exact(value: $baseType): Traversal[NodeType] =
             |    traversal.filter{node => node.${nameCamelCase} == value}
             |
             |  /**
             |    * Traverse to nodes where $nameCamelCase matches one of the elements in `values` exactly.
             |    * */
             |  def ${nameCamelCase}Exact(values: $baseType*): Traversal[NodeType] = {
             |    val vset = values.to(Set)
             |    traversal.filter{node => vset.contains(node.${nameCamelCase})}
             |  }
             |
             |  /**
             |    * Traverse to nodes where $nameCamelCase does not match the regular expression `value`.
             |    * */
             |  def ${nameCamelCase}Not(pattern: $baseType): Traversal[NodeType] = {
             |    if(!Misc.isRegex(pattern)){
             |      traversal.filter{node => node.${nameCamelCase} != pattern}
             |    } else {
             |    val matcher = java.util.regex.Pattern.compile(pattern).matcher("")
             |    traversal.filter{node =>  matcher.reset(node.$nameCamelCase); !matcher.matches()}
             |    }
             |  }
             |
             |  /**
             |    * Traverse to nodes where $nameCamelCase does not match any of the regular expressions in `values`.
             |    * */
             |  def ${nameCamelCase}Not(patterns: $baseType*): Traversal[NodeType] = {
             |    val matchers = patterns.map{pattern => java.util.regex.Pattern.compile(pattern).matcher("")}.toArray
             |    traversal.filter{node => !matchers.exists{ matcher => matcher.reset(node.$nameCamelCase); matcher.matches()}}
             |   }
             |
             |""".stripMargin

        val filterStepsForOptionalString =
          s"""  /**
             |    * Traverse to nodes where the $nameCamelCase matches the regular expression `value`
             |    * */
             |  def $nameCamelCase(pattern: $baseType): Traversal[NodeType] = {
             |    if(!Misc.isRegex(pattern)){
             |      traversal.filter{node => node.$nameCamelCase.isDefined && node.${nameCamelCase}.get == pattern}
             |    } else {
             |    val matcher = java.util.regex.Pattern.compile(pattern).matcher("")
             |    traversal.filter{node => node.$nameCamelCase.isDefined && {matcher.reset(node.$nameCamelCase.get); matcher.matches()}}
             |    }
             |  }
             |
             |  /**
             |    * Traverse to nodes where the $nameCamelCase matches at least one of the regular expressions in `values`
             |    * */
             |  def $nameCamelCase(patterns: $baseType*): Traversal[NodeType] = {
             |    val matchers = patterns.map{pattern => java.util.regex.Pattern.compile(pattern).matcher("")}.toArray
             |    traversal.filter{node => node.$nameCamelCase.isDefined && matchers.exists{ matcher => matcher.reset(node.$nameCamelCase.get); matcher.matches()}}
             |   }
             |
             |  /**
             |    * Traverse to nodes where $nameCamelCase matches `value` exactly.
             |    * */
             |  def ${nameCamelCase}Exact(value: $baseType): Traversal[NodeType] =
             |    traversal.filter{node => node.$nameCamelCase.isDefined && node.${nameCamelCase}.get == value}
             |
             |  /**
             |    * Traverse to nodes where $nameCamelCase matches one of the elements in `values` exactly.
             |    * */
             |  def ${nameCamelCase}Exact(values: $baseType*): Traversal[NodeType] = {
             |    val vset = values.to(Set)
             |    traversal.filter{node => node.$nameCamelCase.isDefined && vset.contains(node.${nameCamelCase}.get)}
             |  }
             |
             |  /**
             |    * Traverse to nodes where $nameCamelCase does not match the regular expression `value`.
             |    * */
             |  def ${nameCamelCase}Not(pattern: $baseType): Traversal[NodeType] = {
             |    if(!Misc.isRegex(pattern)){
             |      traversal.filter{node => node.$nameCamelCase.isEmpty || node.${nameCamelCase}.get != pattern}
             |    } else {
             |    val matcher = java.util.regex.Pattern.compile(pattern).matcher("")
             |    traversal.filter{node => node.$nameCamelCase.isEmpty || {matcher.reset(node.$nameCamelCase.get); !matcher.matches()}}
             |    }
             |  }
             |
             |  /**
             |    * Traverse to nodes where $nameCamelCase does not match any of the regular expressions in `values`.
             |    * */
             |  def ${nameCamelCase}Not(patterns: $baseType*): Traversal[NodeType] = {
             |    val matchers = patterns.map{pattern => java.util.regex.Pattern.compile(pattern).matcher("")}.toArray
             |    traversal.filter{node => node.$nameCamelCase.isEmpty || !matchers.exists{ matcher => matcher.reset(node.$nameCamelCase.get); matcher.matches()}}
             |   }
             |
             |""".stripMargin

        val filterStepsForSingleBoolean =
          s"""  /**
             |    * Traverse to nodes where the $nameCamelCase equals the given `value`
             |    * */
             |  def $nameCamelCase(value: $baseType): Traversal[NodeType] =
             |    traversal.filter{_.$nameCamelCase == value}
             |
             |  /**
             |    * Traverse to nodes where $nameCamelCase is not equal to the given `value`.
             |    * */
             |  def ${nameCamelCase}Not(value: $baseType): Traversal[NodeType] =
             |    traversal.filter{_.$nameCamelCase != value}
             |""".stripMargin

        val filterStepsForOptionalBoolean =
          s"""  /**
             |    * Traverse to nodes where the $nameCamelCase equals the given `value`
             |    * */
             |  def $nameCamelCase(value: $baseType): Traversal[NodeType] =
             |    traversal.filter{node => node.${nameCamelCase}.isDefined && node.$nameCamelCase.get == value}
             |
             |  /**
             |    * Traverse to nodes where $nameCamelCase is not equal to the given `value`.
             |    * */
             |  def ${nameCamelCase}Not(value: $baseType): Traversal[NodeType] =
             |    traversal.filter{node => !node.${nameCamelCase}.isDefined || node.$nameCamelCase.get == value}
             |""".stripMargin

        val filterStepsForSingleInt =
          s"""  /**
             |    * Traverse to nodes where the $nameCamelCase equals the given `value`
             |    * */
             |  def $nameCamelCase(value: $baseType): Traversal[NodeType] =
             |    traversal.filter{_.$nameCamelCase == value}
             |
             |  /**
             |    * Traverse to nodes where the $nameCamelCase equals at least one of the given `values`
             |    * */
             |  def $nameCamelCase(values: $baseType*): Traversal[NodeType] = {
             |    val vset = values.toSet
             |    traversal.filter{node => vset.contains(node.$nameCamelCase)}
             |  }
             |
             |  /**
             |    * Traverse to nodes where the $nameCamelCase is greater than the given `value`
             |    * */
             |  def ${nameCamelCase}Gt(value: $baseType): Traversal[NodeType] =
             |    traversal.filter{_.$nameCamelCase > value}
             |
             |  /**
             |    * Traverse to nodes where the $nameCamelCase is greater than or equal the given `value`
             |    * */
             |  def ${nameCamelCase}Gte(value: $baseType): Traversal[NodeType] =
             |    traversal.filter{_.$nameCamelCase >= value}
             |
             |  /**
             |    * Traverse to nodes where the $nameCamelCase is less than the given `value`
             |    * */
             |  def ${nameCamelCase}Lt(value: $baseType): Traversal[NodeType] =
             |    traversal.filter{_.$nameCamelCase < value}
             |
             |  /**
             |    * Traverse to nodes where the $nameCamelCase is less than or equal the given `value`
             |    * */
             |  def ${nameCamelCase}Lte(value: $baseType): Traversal[NodeType] =
             |    traversal.filter{_.$nameCamelCase <= value}
             |
             |  /**
             |    * Traverse to nodes where $nameCamelCase is not equal to the given `value`.
             |    * */
             |  def ${nameCamelCase}Not(value: $baseType): Traversal[NodeType] =
             |    traversal.filter{_.$nameCamelCase != value}
             |
             |  /**
             |    * Traverse to nodes where $nameCamelCase is not equal to any of the given `values`.
             |    * */
             |  def ${nameCamelCase}Not(values: $baseType*): Traversal[NodeType] = {
             |    val vset = values.toSet
             |    traversal.filter{node => !vset.contains(node.$nameCamelCase)}
             |  }
             |""".stripMargin

        val filterStepsForOptionalInt =
          s"""  /**
             |    * Traverse to nodes where the $nameCamelCase equals the given `value`
             |    * */
             |  def $nameCamelCase(value: $baseType): Traversal[NodeType] =
             |    traversal.filter{node => node.$nameCamelCase.isDefined && node.$nameCamelCase.get == value}
             |
             |  /**
             |    * Traverse to nodes where the $nameCamelCase equals at least one of the given `values`
             |    * */
             |  def $nameCamelCase(values: $baseType*): Traversal[NodeType] = {
             |    val vset = values.toSet
             |    traversal.filter{node => node.$nameCamelCase.isDefined && vset.contains(node.$nameCamelCase.get)}
             |  }
             |
             |  /**
             |    * Traverse to nodes where the $nameCamelCase is greater than the given `value`
             |    * */
             |  def ${nameCamelCase}Gt(value: $baseType): Traversal[NodeType] =
             |    traversal.filter{node => node.$nameCamelCase.isDefined && node.$nameCamelCase.get > value}
             |
             |  /**
             |    * Traverse to nodes where the $nameCamelCase is greater than or equal the given `value`
             |    * */
             |  def ${nameCamelCase}Gte(value: $baseType): Traversal[NodeType] =
             |    traversal.filter{node => node.$nameCamelCase.isDefined && node.$nameCamelCase.get >= value}
             |
             |  /**
             |    * Traverse to nodes where the $nameCamelCase is less than the given `value`
             |    * */
             |  def ${nameCamelCase}Lt(value: $baseType): Traversal[NodeType] =
             |    traversal.filter{node => node.$nameCamelCase.isDefined && node.$nameCamelCase.get < value}
             |
             |  /**
             |    * Traverse to nodes where the $nameCamelCase is less than or equal the given `value`
             |    * */
             |  def ${nameCamelCase}Lte(value: $baseType): Traversal[NodeType] =
             |    traversal.filter{node => node.$nameCamelCase.isDefined && node.$nameCamelCase.get <= value}
             |
             |  /**
             |    * Traverse to nodes where $nameCamelCase is not equal to the given `value`.
             |    * */
             |  def ${nameCamelCase}Not(value: $baseType): Traversal[NodeType] =
             |    traversal.filter{node => !node.$nameCamelCase.isDefined || node.$nameCamelCase.get != value}
             |
             |  /**
             |    * Traverse to nodes where $nameCamelCase is not equal to any of the given `values`.
             |    * */
             |  def ${nameCamelCase}Not(values: $baseType*): Traversal[NodeType] = {
             |    val vset = values.toSet
             |    traversal.filter{node => !node.$nameCamelCase.isDefined || !vset.contains(node.$nameCamelCase.get)}
             |  }
             |""".stripMargin

        val filterStepsGenericSingle =
          s"""  /**
             |    * Traverse to nodes where the $nameCamelCase equals the given `value`
             |    * */
             |  def $nameCamelCase(value: $baseType): Traversal[NodeType] =
             |    traversal.filter{_.$nameCamelCase == value}
             |
             |  /**
             |    * Traverse to nodes where the $nameCamelCase equals at least one of the given `values`
             |    * */
             |  def $nameCamelCase(values: $baseType*): Traversal[NodeType] = {
             |    val vset = values.toSet
             |    traversal.filter{node => !vset.contains(node.$nameCamelCase)}
             |  }
             |
             |  /**
             |    * Traverse to nodes where $nameCamelCase is not equal to the given `value`.
             |    * */
             |  def ${nameCamelCase}Not(value: $baseType): Traversal[NodeType] =
             |    traversal.filter{_.$nameCamelCase != value}
             |
             |  /**
             |    * Traverse to nodes where $nameCamelCase is not equal to any of the given `values`.
             |    * */
             |  def ${nameCamelCase}Not(values: $baseType*): Traversal[NodeType] = {
             |    val vset = values.toSet
             |    traversal.filter{node => !vset.contains(node.$nameCamelCase)}
             |  }
             |""".stripMargin

        val filterStepsGenericOption =
          s"""  /**
             |    * Traverse to nodes where the $nameCamelCase equals the given `value`
             |    * */
             |  def $nameCamelCase(value: $baseType): Traversal[NodeType] =
             |    traversal.filter{node => node.$nameCamelCase.isDefined && node.$nameCamelCase.get == value}
             |
             |  /**
             |    * Traverse to nodes where the $nameCamelCase equals at least one of the given `values`
             |    * */
             |  def $nameCamelCase(values: $baseType*): Traversal[NodeType] = {
             |    val vset = values.toSet
             |    traversal.filter{node => node.$nameCamelCase.isDefined && !vset.contains(node.$nameCamelCase.get)}
             |  }
             |
             |  /**
             |    * Traverse to nodes where $nameCamelCase is not equal to the given `value`.
             |    * */
             |  def ${nameCamelCase}Not(value: $baseType): Traversal[NodeType] =
             |    traversal.filter{node => !node.$nameCamelCase.isDefined || node.$nameCamelCase.get != value}
             |
             |  /**
             |    * Traverse to nodes where $nameCamelCase is not equal to any of the given `values`.
             |    * */
             |  def ${nameCamelCase}Not(values: $baseType*): Traversal[NodeType] = {
             |    val vset = values.toSet
             |    traversal.filter{node => !node.$nameCamelCase.isDefined || !vset.contains(node.$nameCamelCase.get)}
             |  }
             |""".stripMargin

        val filterSteps = (cardinality, property.valueType) match {
          case (Cardinality.List, _) => ""
          case (Cardinality.One(_), ValueType.String) => filterStepsForSingleString
          case (Cardinality.ZeroOrOne, ValueType.String) => filterStepsForOptionalString
          case (Cardinality.One(_), ValueType.Boolean) => filterStepsForSingleBoolean
          case (Cardinality.ZeroOrOne, ValueType.Boolean) => filterStepsForOptionalBoolean
          case (Cardinality.One(_), ValueType.Int) => filterStepsForSingleInt
          case (Cardinality.ZeroOrOne, ValueType.Int) => filterStepsForOptionalInt
          case (Cardinality.One(_), _) => filterStepsGenericSingle
          case (Cardinality.ZeroOrOne, _) => filterStepsGenericOption
          case _ => ""
        }

        s"""  /** Traverse to $nameCamelCase property */
           |  def $nameCamelCase: Traversal[$baseType] =
           |    traversal.$mapOrFlatMap(_.$nameCamelCase)
           |
           |  $filterSteps
           |""".stripMargin
      }.mkString("\n")

      s"""
         |/** Traversal steps for $className */
         |class ${className}TraversalExtGen[NodeType <: $className](val traversal: Traversal[NodeType]) extends AnyVal {
         |
         |$propertyTraversals
         |
         |}""".stripMargin
    }

    def generateNodeBaseTypeSource(nodeBaseType: NodeBaseType): String = {
      s"""package $traversalsPackage
         |
         |import overflowdb.traversal.Traversal
         |import $nodesPackage._
         |
         |${generatePropertyTraversals(nodeBaseType.className, nodeBaseType.properties)}
         |
         |""".stripMargin
    }

    def generateNodeSource(nodeType: NodeType) = {
      s"""$staticHeader
         |${generatePropertyTraversals(nodeType.className, nodeType.properties)}
         |""".stripMargin
    }

    val packageObject =
      s"""package $basePackage
         |package object traversal extends NodeTraversalImplicits
         |""".stripMargin

    val results = mutable.Buffer.empty[File]
    val baseDir = outputDir / traversalsPackage.replaceAll("\\.", "/")
    if (baseDir.exists) baseDir.delete()
    baseDir.createDirectories()
    results.append(baseDir.createChild("package.scala").write(packageObject))
    results.append(baseDir.createChild("NodeTraversalImplicits.scala").write(nodeTraversalImplicits))
    schema.nodeBaseTypes.foreach { nodeBaseTrait =>
      val src = generateNodeBaseTypeSource(nodeBaseTrait)
      val srcFile = nodeBaseTrait.className + ".scala"
      results.append(baseDir.createChild(srcFile).write(src))
    }
    schema.nodeTypes.foreach { nodeType =>
      val src = generateNodeSource(nodeType)
      val srcFile = nodeType.className + ".scala"
      results.append(baseDir.createChild(srcFile).write(src))
    }
    results.toSeq
  }

  /** generates classes to easily add new nodes to the graph
    * this ability could have been added to the existing nodes, but it turned out as a different specialisation,
    * since e.g. `id` is not set before adding it to the graph */
  protected def writeNewNodeFile(outputDir: File): File = {
    val staticHeader =
      s"""package $nodesPackage
         |
         |/** base type for all nodes that can be added to a graph, e.g. the diffgraph */
         |trait NewNode extends AbstractNode with Product {
         |  def properties: Map[String, Any]
         |  def copy: this.type
         |}
         |""".stripMargin

    def generateNewNodeSource(nodeType: NodeType, properties: Seq[Property[_]]) = {
      import Property.Cardinality
      case class FieldDescription(name: String, valueType: String, fullType: String, cardinality: Cardinality)
      val fieldDescriptions = mutable.ArrayBuffer.empty[FieldDescription]

      for (property <- properties) {
        fieldDescriptions += FieldDescription(
          camelCase(property.name),
          typeFor(property),
          getCompleteType(property),
          property.cardinality)
      }

      for (containedNode <- nodeType.containedNodes) {
        fieldDescriptions += FieldDescription(
          containedNode.localName,
          typeFor(containedNode),
          getCompleteType(containedNode),
          containedNode.cardinality)
      }

      val memberVariables = fieldDescriptions.reverse.map {
        case FieldDescription(name, _, fullType, cardinality) =>
          val defaultValue = cardinality match {
            case Cardinality.One(default) => defaultValueImpl(default)
            case Cardinality.ZeroOrOne => "None"
            case Cardinality.List => "collection.immutable.ArraySeq.empty"
          }
          s"var $name: $fullType = $defaultValue"

      }.mkString(", ")

      val propertiesMapImpl = {
        val putKeysImpl = properties
          .map { key =>
            val memberName = camelCase(key.name)
            import Property.Cardinality
            key.cardinality match {
              case Cardinality.One(default) =>
                val isDefaultValueImpl = defaultValueCheckImpl(memberName, default)
                s"""  if (!($isDefaultValueImpl)) { res += "${key.name}" -> $memberName }"""
              case Cardinality.ZeroOrOne =>
                s"""  $memberName.map { value => res += "${key.name}" -> value }"""
              case Cardinality.List =>
                s"""  if ($memberName != null && $memberName.nonEmpty) { res += "${key.name}" -> $memberName }"""
            }
          }
      val putRefsImpl = nodeType.containedNodes.map { key =>
          import Property.Cardinality
          val memberName = key.localName
          key.cardinality match {
            case Cardinality.One(default) =>
              val isDefaultValueImpl = defaultValueCheckImpl(memberName, default)
              s"""  if (!($isDefaultValueImpl)) { res += "$memberName" -> $memberName }"""
            case Cardinality.ZeroOrOne =>
              s"""  $memberName.map { value => res += "$memberName" -> value }"""
            case Cardinality.List =>
              s"""  if ($memberName != null && $memberName.nonEmpty) { res += "$memberName" -> $memberName }"""
          }
        }

        val propertiesImpl = {
          val lines = putKeysImpl ++ putRefsImpl
          if (lines.nonEmpty) {
            s"""  var res = Map[String, Any]()
               |${lines.mkString("\n")}
               |  res""".stripMargin
          } else {
            "Map.empty"
          }
        }

        s"""override def properties: Map[String, Any] = {
           |$propertiesImpl
           |}""".stripMargin
      }

      val nodeClassName = nodeType.className

      val mixins = nodeType.extendz.map{baseType => s"with ${baseType.className}New"}.mkString(" ")

      val propertySettersImpl = fieldDescriptions.map {
        case FieldDescription(name, valueType , _, cardinality) =>
          cardinality match {
            case Cardinality.One(_) =>
              s"""def $name(value: $valueType): this.type = {
                 |  this.$name = value
                 |  this
                 |}
                 |""".stripMargin

            case Cardinality.ZeroOrOne =>
              s"""def $name(value: $valueType): this.type = {
                 |  this.$name = Option(value)
                 |  this
                 |}
                 |
                 |def $name(value: Option[$valueType]): this.type = $name(value.orNull)
                 |""".stripMargin

            case Cardinality.List =>
              s"""def $name(value: IterableOnce[$valueType]): this.type = {
                 |  this.$name = value.iterator.to(collection.immutable.ArraySeq)
                 |  this
                 |}
                 |""".stripMargin
          }
      }.mkString("\n")

      val copyPropertiesImpl = properties.map { property =>
        val memberName = camelCase(property.name)
        s"newInstance.$memberName = this.$memberName"
      }.mkString("\n")

      val classNameNewNode = s"New$nodeClassName"

      val productElementAccessors = fieldDescriptions.reverse.zipWithIndex.map  {
        case (fieldDescription, index) =>
          s"case $index => this.${fieldDescription.name}"
      }.mkString("\n")

      s"""object $classNameNewNode {
         |  def apply(): $classNameNewNode = new $classNameNewNode
         |}
         |
         |class $classNameNewNode($memberVariables)
         |  extends NewNode with ${nodeClassName}Base $mixins {
         |
         |  override def label: String = "${nodeType.name}"
         |
         |  override def copy: this.type = {
         |    val newInstance = new New$nodeClassName
         |    $copyPropertiesImpl
         |    newInstance.asInstanceOf[this.type]
         |  }
         |
         |  $propertySettersImpl
         |
         |  $propertiesMapImpl
         |
         |  override def productElement(n: Int): Any =
         |    n match {
         |      $productElementAccessors
         |      case _ => null
         |    }
         |
         |  override def productPrefix = "$classNameNewNode"
         |  override def productArity = ${fieldDescriptions.size}
         |
         |  override def canEqual(that: Any): Boolean = that != null && that.isInstanceOf[$classNameNewNode]
         |}
         |""".stripMargin
    }

    val outfile = outputDir / nodesPackage.replaceAll("\\.", "/") / "NewNodes.scala"
    if (outfile.exists) outfile.delete()
    outfile.createFile()
    val src = schema.nodeTypes.map { nodeType =>
      generateNewNodeSource(nodeType, nodeType.properties)
    }.mkString("\n")
    outfile.write(s"""$staticHeader
                     |$src
                     |""".stripMargin)
  }
}

object CodeGen {
  case class ConstantContext(name: String, source: String, documentation: Option[String])
}
