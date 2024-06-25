package overflowdb.codegen

import better.files._
import overflowdb.codegen.CodeGen.ConstantContext
import overflowdb.schema._
import overflowdb.schema.EdgeType.Cardinality
import overflowdb.schema.Property.ValueType

import java.lang.System.lineSeparator
import scala.collection.mutable

/** Generates a domain model for OverflowDb traversals for a given domain-specific schema. */
class CodeGen(schema: Schema) {
  import Helpers._
  val basePackage = schema.basePackage
  val nodesPackage = s"$basePackage.nodes"
  val edgesPackage = s"$basePackage.edges"
  val traversalsPackage = s"$basePackage.traversal"

  private var enableScalafmt = true
  private var scalafmtConfig: Option[File] = None

  def disableScalafmt: this.type = {
    enableScalafmt = false
    this
  }

  /** replace entire default scalafmt config (from Formatter.defaultScalafmtConfig) with custom config */
  def withScalafmtConfig(file: java.io.File): this.type = {
    this.scalafmtConfig = Option(file.toScala)
    this
  }

  def run(outputDir: java.io.File, deleteExistingFiles: Boolean = true): Seq[java.io.File] = {
    println(s"writing domain classes to $outputDir")
    if (deleteExistingFiles && outputDir.exists)
      deleteRecursively(outputDir)

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

    if (enableScalafmt) {
      val scalaSourceFiles = results.filter(_.extension == Some(".scala"))
      Formatter.run(scalaSourceFiles, scalafmtConfig)
    }
    results.map(_.toJava)
  }

  /* to provide feedback for potential schema optimisation: no need to redefine properties if they are already
   * defined in one of the parents */
  protected def warnForDuplicatePropertyDefinitions() = {
    val warnings = for {
      nodeType <- schema.allNodeTypes
      property <- nodeType.propertiesWithoutInheritance
      baseType <- nodeType.extendzRecursively
      if baseType.propertiesWithoutInheritance.contains(property) && !schema.noWarnList.contains((nodeType, property))
    } yield s"[info]: $nodeType wouldn't need to have property `${property.name}` added explicitly - $baseType already brings it in"

    if (warnings.size > 0) println(s"${warnings.size} warnings found:")
    warnings.sorted.foreach(println)
  }

  protected def writeStarters(outputDir: File): Seq[File] = {
    val results = mutable.Buffer.empty[File]
    val baseDir = outputDir / basePackage.replaceAll("\\.", "/")
    baseDir.createDirectories()
    val domainShortName = schema.domainShortName

    def registerAdditionalSearchPackages: String = {
      schema.additionalTraversalsPackages.map { packageName =>
        s""".registerAdditionalSearchPackage("$packageName")"""
      }.mkString("")
    }

    val starters = mutable.ArrayBuffer[String]()
    for(typ <- schema.nodeTypes if typ.starterName.isDefined){
      starters.append(
        s"""@overflowdb.traversal.help.Doc(info = "All nodes of type ${typ.className}, i.e. with label ${typ.name}")
           |def ${typ.starterName.get}: Iterator[nodes.${typ.className}] = overflowdb.traversal.InitialTraversal.from[nodes.${typ.className}](wrapper.graph, "${typ.name}")""".stripMargin
      )
      typ.primaryKey match {
        case Some(property:Property[_]) =>
          val nameCamelCase = camelCase(property.name)
          val baseType = typeFor(property)
          starters.append(
            s"""@overflowdb.traversal.help.Doc(info = "Shorthand for ${typ.starterName.get}.${nameCamelCase}")
               |def ${typ.starterName.get}(key: ${baseType}): Iterator[nodes.${typ.className}] =
               |  new  ${traversalsPackage}.${typ.className}TraversalExtGen(${typ.starterName.get}).${nameCamelCase}(key)""".stripMargin)
        case _ =>
      }
    }
    for (typ <- schema.nodeBaseTypes if typ.starterName.isDefined) {
      val types = schema.nodeTypes.filter{_.extendzRecursively.contains(typ)}
      starters.append(
        s"""@overflowdb.traversal.help.Doc(info = "All nodes of type ${typ.className}, i.e. with label in ${types.map{_.name}.sorted.mkString(", ")}")
           |def ${typ.starterName.get}: Iterator[nodes.${typ.className}] =  wrapper.graph.nodes(${types.map{concrete => "\"" + concrete.name + "\""}.mkString(", ") }).asScala.asInstanceOf[Iterator[nodes.${typ.className}]]""".stripMargin)
    }
    val domainMain = baseDir.createChild(s"$domainShortName.scala").write(
      s"""package $basePackage
         |
         |import java.nio.file.{Path, Paths}
         |import overflowdb.{Config, Graph}
         |import overflowdb.traversal.help.{DocSearchPackages, TraversalHelp}
         |import overflowdb.traversal.help.Table.AvailableWidthProvider
         |import scala.jdk.javaapi.CollectionConverters.asJava
         |
         |object $domainShortName {
         |  implicit val defaultDocSearchPackage: DocSearchPackages = DocSearchPackages(getClass.getPackage.getName)
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
         |  def newDiffGraphBuilder: DiffGraphBuilder = new DiffGraphBuilder
         |}
         |
         |
         |/**
         |  * Domain-specific wrapper for graph, starting point for traversals.
         |  * @param graph the underlying graph. An empty graph is created if this parameter is omitted.
         |  */
         |class $domainShortName(private val _graph: Graph = $domainShortName.emptyGraph) extends AutoCloseable {
         |  def graph: Graph = _graph
         |
         |  def help(implicit searchPackageNames: DocSearchPackages, availableWidthProvider: AvailableWidthProvider): String =
         |    new TraversalHelp(searchPackageNames).forTraversalSources
         |
         |  override def close(): Unit =
         |    graph.close
         |
         |  override def toString(): String =
         |    String.format("$domainShortName (%s)", graph)
         |}
         |
         |class GeneratedNodeStarterExt(val wrapper: ${domainShortName}) extends AnyVal {
         |import scala.jdk.CollectionConverters.IteratorHasAsScala
         |${starters.mkString("\n\n")}
         |}
         |
         |/**
         |  * Domain-specific version of diffgraph builder. This is to allow schema checking before diffgraph application
         |  * in the future, as well as a schema-aware point for providing backwards compatibility in odbv2.
         |  */
         |class DiffGraphBuilder extends overflowdb.BatchedUpdate.DiffGraphBuilder {
         |  override def absorb(other: overflowdb.BatchedUpdate.DiffGraphBuilder): this.type = {super.absorb(other); this}
         |  override def addNode(node: overflowdb.DetachedNodeData): this.type = {super.addNode(node); this}
         |  override def addNode(label: String, keyvalues:Any*): this.type = {super.addNode(label, keyvalues: _*); this}
         |  override def addEdge(src: overflowdb.NodeOrDetachedNode, dst: overflowdb.NodeOrDetachedNode, label: String): this.type = {super.addEdge(src, dst, label); this}
         |  override def addEdge(src: overflowdb.NodeOrDetachedNode, dst: overflowdb.NodeOrDetachedNode, label: String, properties: Any*): this.type = {super.addEdge(src, dst, label, properties: _*); this}
         |  override def setNodeProperty(node: overflowdb.Node, label: String, property: Any): this.type = {super.setNodeProperty(node, label, property); this}
         |  override def removeNode(node: overflowdb.Node): this.type = {super.removeNode(node); this}
         |  override def removeEdge(edge: overflowdb.Edge): this.type = {super.removeEdge(edge); this}
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
        s"""$documentation
           |${constant.source}
           |""".stripMargin
      }.mkString(lineSeparator)
      val allConstantsSetType = if (constantsSource.contains("PropertyKey")) "PropertyKey<?>" else "String"
      val allConstantsBody = constants.map { constant =>
        s"add(${constant.name});"
      }.mkString(lineSeparator)
      val allConstantsSet =
        s"""
           |public static Set<$allConstantsSetType> ALL = new HashSet<$allConstantsSetType>() {{
           |$allConstantsBody
           |}};
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
           |  lazy val all: Seq[EdgeFactory[?]] = Seq($edgeFactories)
           |  lazy val allAsJava: java.util.List[EdgeFactory[?]] = all.asJava
           |}
           |""".stripMargin
      }

      s"""$staticHeader
         |$propertyErrorRegisterImpl
         |$factories
         |""".stripMargin
    }

    def generateEdgeSource(edgeType: EdgeType, properties: Seq[Property[?]]) = {
      val edgeClassName = edgeType.className

      val propertyNames = properties.map(_.className)

      val propertyNameDefs = properties.map { p =>
        s"""val ${p.className} = "${p.name}" """
      }.mkString(lineSeparator)

      val propertyDefinitions = properties.map { p =>
        propertyKeyDef(p.name, typeFor(p), p.cardinality)
      }.mkString(lineSeparator)

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

      def propertyBasedFieldAccessors(properties: Seq[Property[?]]): String = {
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
        }.mkString(lineSeparator)
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
      } yield s"def _$accessor: Iterator[StoredNode] = Iterator.empty"

      val markerTraits =
        schema.allNodeTypes
          .flatMap(_.markerTraits)
          .distinct
          .map { case MarkerTrait(name) => s"trait $name" }
          .sorted
          .mkString(lineSeparator)

      val factories = {
        val nodeFactories =
          schema.nodeTypes.map(nodeType => nodeType.className + ".factory").mkString(", ")
        s"""object Factories {
           |  lazy val all: Seq[NodeFactory[?]] = Seq($nodeFactories)
           |  lazy val allAsJava: java.util.List[NodeFactory[?]] = all.asJava
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
         |trait StaticType[+T]
         |
         |/** Abstract supertype for overflowdb.Node and NewNode */
         |trait AbstractNode extends overflowdb.NodeOrDetachedNode with StaticType[AnyRef]{
         |  def label: String
         |}
         |
         |/* A node that is stored inside an Graph (rather than e.g. DiffGraph) */
         |trait StoredNode extends Node with AbstractNode with Product {
         |  /* underlying Node in the graph.
         |   * since this is a StoredNode, this is always set */
         |  def underlying: Node = this
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
         |  ${genericNeighborAccessors.mkString(lineSeparator)}
         |}
         |
         |  $markerTraits
         |
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
      val propertyAccessors = Helpers.propertyAccessors(properties)

      val mixinsForBaseTypes = nodeBaseType.extendz.map { baseTrait =>
        s"with ${baseTrait.className}"
      }.mkString(" ")

      val mixinForBaseTypesNew = nodeBaseType.extendz.map { baseTrait =>
        s"with ${baseTrait.className}New"
      }.mkString(" ")

      val mixinsForBaseTypes2 = nodeBaseType.extendz.map { baseTrait =>
        s"with ${baseTrait.className}Base"
      }.mkString(" ")

      val mixinsForMarkerTraits = nodeBaseType.markerTraits.map { case MarkerTrait(name) =>
        s"with $name"
      }.mkString(" ")

      def abstractEdgeAccessors(nodeBaseType: NodeBaseType, direction: Direction.Value) = {
        val edgeAndNeighbors = nodeBaseType
          .edges(direction)
          .groupBy(_.viaEdge)
          .toSeq
          .map { case (edge, neighors) => (edge, neighors.sortBy(_.neighbor.name)) }
          .sortBy { case (edge, _) => edge.name }
        edgeAndNeighbors.map { case (edge, neighbors) =>
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
          val neighborNodesType = "? <: StoredNode"
          val genericEdgeAccessor = s"def $edgeAccessorName: Iterator[$neighborNodesType]"

          val specificNodeAccessors = neighbors.flatMap { adjacentNode =>
            val neighbor = adjacentNode.neighbor
            val entireNodeHierarchy: Set[AbstractNodeType] = neighbor.subtypes(schema.allNodeTypes.toSet) ++ (neighbor.extendzRecursively :+ neighbor)
            entireNodeHierarchy.map { neighbor =>
              val accessorName = adjacentNode.customStepName.getOrElse(
                s"_${camelCase(neighbor.name)}Via${edge.className.capitalize}${camelCaseCaps(direction.toString)}"
              )
              val accessorImpl0 = s"$edgeAccessorName.collectAll[${neighbor.className}]"
              val cardinality = adjacentNode.cardinality
              val accessorImpl1 = cardinality match {
                case EdgeType.Cardinality.One =>
                  s"""try { $accessorImpl0.next() } catch {
                     |  case e: java.util.NoSuchElementException =>
                     |    throw new overflowdb.SchemaViolationException("$direction edge with label ${adjacentNode.viaEdge.name} to an adjacent ${neighbor.name} is mandatory, but not defined for this ${nodeBaseType.name} node with id=" + id, e)
                     |}""".stripMargin
                case EdgeType.Cardinality.ZeroOrOne => s"$accessorImpl0.nextOption()"
                case _ => accessorImpl0
              }

              s"""/** ${adjacentNode.customStepDoc.getOrElse("")}
                 |  * Traverse to ${neighbor.name} via ${adjacentNode.viaEdge.name} $direction edge.
                 |  */ ${docAnnotationMaybe(adjacentNode.customStepDoc)}
                 |def $accessorName: ${fullScalaType(neighbor, cardinality)} =
                 |  $accessorImpl1
                 |  """.stripMargin
            }
          }.distinct.mkString(lineSeparator)

          s"""$genericEdgeAccessor
             |
             |$specificNodeAccessors""".stripMargin
        }.mkString(lineSeparator)
      }

      val companionObject = {
        val propertyNames = nodeBaseType.properties.map(_.name)
        val propertyNameDefs = propertyNames.map { name =>
          s"""val ${camelCaseCaps(name)} = "$name" """
        }.mkString(lineSeparator)

        val propertyDefinitions = properties.map { p =>
          propertyKeyDef(p.name, typeFor(p), p.cardinality)
        }.mkString(lineSeparator)

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

      val newNodePropertySetters = nodeBaseType.properties.map { property =>
        val camelCaseName = camelCase(property.name)
        val tpe = getCompleteType(property)
        s"def ${camelCaseName}_=(value: $tpe): Unit"
      }.mkString(lineSeparator)

      s"""package $nodesPackage
         |
         |$companionObject
         |
         |trait ${className}Base extends AbstractNode $mixinsForBaseTypes2 $mixinsForMarkerTraits {
         |  $propertyAccessors
         |}
         |
         |trait ${className}New extends NewNode $mixinForBaseTypesNew {
         |  $newNodePropertySetters
         |  $propertyAccessors
         |}
         |
         |trait $className extends StoredNode with ${className}Base
         |$mixinsForBaseTypes {
         |import overflowdb.traversal._
         |${abstractEdgeAccessors(nodeBaseType, Direction.OUT)}
         |${abstractEdgeAccessors(nodeBaseType, Direction.IN)}
         |}""".stripMargin
    }

    def generateNodeSource(nodeType: NodeType) = {
      val properties = nodeType.properties

      val propertyNames = (properties.map(_.name) ++ nodeType.containedNodes.map(_.localName)).distinct
      val propertyNameDefs = propertyNames.map { name =>
        s"""val ${camelCaseCaps(name)} = "$name" """
      }.mkString(lineSeparator)

      val propertyDefs = properties.map { p =>
        propertyKeyDef(p.name, typeFor(p), p.cardinality)
      }.mkString(lineSeparator)

      val propertyDefsForContainedNodes = nodeType.containedNodes.map { containedNode =>
        propertyKeyDef(containedNode.localName, containedNode.classNameForStoredNode, containedNode.cardinality)
      }.mkString(lineSeparator)

      val (neighborOutInfos, neighborInInfos) = {
        /** the offsetPos determines the index into the adjacent nodes array of a given node type
          * assigning numbers here must follow the same way as in NodeLayoutInformation, i.e. starting at 0,
          * first assign ids to the outEdges based on their order in the list, and then the same for inEdges */
        var _currOffsetPos = -1
        def nextOffsetPos(): Int = { _currOffsetPos += 1; _currOffsetPos }

        case class AjacentNodeWithInheritanceStatus(adjacentNode: AdjacentNode, isInherited: Boolean)

        /** For all adjacent nodes, figure out if they are inherited or not.
          *  Later on, we will create edge accessors for all inherited neighbors, but only create the node accessors
          *  on the base types (if they are defined there). Note: they may even be inherited with a different cardinality */
        def adjacentNodesWithInheritanceStatus(adjacentNodes: AbstractNodeType => Seq[AdjacentNode]): Seq[AjacentNodeWithInheritanceStatus] = {
          // nodes may have been linked from any of their parents, including `AnyNode`
          val allParentNodes = nodeType.extendzRecursively :+ schema.anyNode
          val inherited = allParentNodes.flatMap(adjacentNodes).map(AjacentNodeWithInheritanceStatus(_, true))

          // only edge and neighbor node matter, not the cardinality
          val inheritedLookup: Set[(EdgeType, AbstractNodeType)] =
            inherited.map(_.adjacentNode).map { adjacentNode => (adjacentNode.viaEdge, adjacentNode.neighbor) }.toSet

          val direct = adjacentNodes(nodeType).map { adjacentNode =>
            val isInherited = inheritedLookup.contains((adjacentNode.viaEdge, adjacentNode.neighbor))
            AjacentNodeWithInheritanceStatus(adjacentNode, isInherited)
          }
          (direct ++ inherited).distinct
        }

        def createNeighborInfos(neighborContexts: Seq[AjacentNodeWithInheritanceStatus], direction: Direction.Value): Seq[NeighborInfoForEdge] = {
          neighborContexts
            .groupBy(_.adjacentNode.viaEdge)
            .toSeq
            .sortBy { case (edge, _) => edge.name }
            .map { case (edge, neighborContexts) =>
              val neighborInfoForNodes =
                neighborContexts
                  .sortBy { x =>
                    (x.adjacentNode.neighbor.name,
                      x.adjacentNode.customStepName,
                      x.adjacentNode.viaEdge.toString,
                      x.adjacentNode.neighbor.name,
                      x.adjacentNode.cardinality.toString)
                  }
                  .map { case AjacentNodeWithInheritanceStatus(adjacentNode, isInherited) =>
                    NeighborInfoForNode(adjacentNode.neighbor, edge, direction, adjacentNode.cardinality, isInherited, adjacentNode.customStepName, adjacentNode.customStepDoc)
                  }
              NeighborInfoForEdge(edge, neighborInfoForNodes, nextOffsetPos())
            }
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
        }.mkString(s",$lineSeparator")
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

      val mixinsForExtendedNodes: String =
        nodeType.extendz.map { traitName =>
          s"with ${traitName.className}"
        }.mkString(" ")

      val mixinsForExtendedNodesBase: String =
        nodeType.extendz.map { traitName =>
          s"with ${traitName.className}Base"
        }.mkString(" ")

      val mixinsForMarkerTraits: String =
        nodeType.markerTraits.map { case MarkerTrait(name) =>
          s"with $name"
        }.mkString(" ")

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
        }.mkString(lineSeparator)

        val putContainedNodesImpl = {
          nodeType.containedNodes.map { cnt =>
            val memberName = cnt.localName
            cnt.cardinality match {
              case Cardinality.One(_) =>
                s"""properties.put("$memberName", this._$memberName)"""
              case Cardinality.ZeroOrOne =>
                s"""$memberName.map { value => properties.put("$memberName", value) }"""
              case Cardinality.List =>
                s"""if (this._$memberName != null && this._$memberName.nonEmpty) { properties.put("$memberName", this.$memberName) }"""
            }
          }
        }.mkString(lineSeparator)

        s"""{
           |  val properties = new java.util.HashMap[String, Any]
           |  $putKeysImpl
           |  $putContainedNodesImpl
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
        }.mkString(lineSeparator)

        val putContainedNodesImpl = {
          nodeType.containedNodes.map { cnt =>
            val memberName = cnt.localName
            cnt.cardinality match {
              case Cardinality.One(default) =>
                val isDefaultValueImpl = defaultValueCheckImpl(s"this._$memberName", default)
                s"""if (!($isDefaultValueImpl)) { properties.put("$memberName", this._$memberName) }"""
              case Cardinality.ZeroOrOne =>
                s"""$memberName.map { value => properties.put("$memberName", value) }"""
              case Cardinality.List =>
                s"""if (this._$memberName != null && this._$memberName.nonEmpty) { properties.put("$memberName", this.$memberName) }"""
            }
          }
        }.mkString(lineSeparator)


        s"""{
           |  val properties = new java.util.HashMap[String, Any]
           |  $putKeysImpl
           |  $putContainedNodesImpl
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
                s"""this._$memberName = $newNodeCasted.$memberName""".stripMargin
              case Cardinality.ZeroOrOne =>
                s"""this._$memberName = $newNodeCasted.$memberName.orNull""".stripMargin
              case Cardinality.List =>
                s"""this._$memberName = if ($newNodeCasted.$memberName != null) $newNodeCasted.$memberName else collection.immutable.ArraySeq.empty""".stripMargin
            }
          }
          lines.mkString(lineSeparator)
        }

        val initRefsImpl = {
          nodeType.containedNodes.map { containedNode =>
            import Property.Cardinality
            val memberName = containedNode.localName
            val containedNodeType = containedNode.classNameForStoredNode

            containedNode.cardinality match {
              case Cardinality.One(_) =>
                s"""this._$memberName = $newNodeCasted.$memberName match {
                   |  case null => null
                   |  case newNode: NewNode => mapping(newNode).asInstanceOf[$containedNodeType]
                   |  case oldNode: StoredNode => oldNode.asInstanceOf[$containedNodeType]
                   |  case _ => throw new MatchError("unreachable")
                   |}""".stripMargin
              case Cardinality.ZeroOrOne =>
                s"""this._$memberName = $newNodeCasted.$memberName match {
                   |  case null | None => null
                   |  case Some(newNode:NewNode) => mapping(newNode).asInstanceOf[$containedNodeType]
                   |  case Some(oldNode:StoredNode) => oldNode.asInstanceOf[$containedNodeType]
                   |  case _ => throw new MatchError("unreachable")
                   |}""".stripMargin
              case Cardinality.List =>
                s"""this._$memberName =
                   |  if ($newNodeCasted.$memberName == null || $newNodeCasted.$memberName.isEmpty) {
                   |    collection.immutable.ArraySeq.empty
                   |  } else {
                   |    collection.immutable.ArraySeq.unsafeWrapArray(
                   |      $newNodeCasted.$memberName.map {
                   |        case null => throw new NullPointerException("NullPointers forbidden in contained nodes")
                   |        case newNode:NewNode => mapping(newNode).asInstanceOf[$containedNodeType]
                   |        case oldNode:StoredNode => oldNode.asInstanceOf[$containedNodeType]
                   |        case _ => throw new MatchError("unreachable")
                   |      }.toArray
                   |    )
                   |  }
                   |""".stripMargin
            }
          }
        }.mkString(lineSeparator)

        val registerFullName = if(!properties.map{_.name}.contains("FULL_NAME")) "" else {
          s"""graph.indexManager.putIfIndexed("FULL_NAME", $newNodeCasted.fullName, this.ref)"""
        }

        s"""override def fromNewNode(newNode: NewNode, mapping: NewNode => StoredNode):Unit = {
           |  $initKeysImpl
           |  $initRefsImpl
           |  $registerFullName
           |}""".stripMargin
      }

      val containedNodesAsMembers =
        nodeType.containedNodes
          .map { containedNode =>
            import Property.Cardinality
            val containedNodeType = containedNode.classNameForStoredNode
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
          .mkString(lineSeparator)

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

      val productElementNames =
        productElements.map { case ProductElement(name, _, index) =>
          s"""case $index => "$name" """
        }.mkString(lineSeparator)

      val productElementAccessors =
        productElements.map { case ProductElement(_, accessorSrc, index) =>
          s"case $index => $accessorSrc"
        }.mkString(lineSeparator)

      val abstractContainedNodeAccessors = nodeType.containedNodes.map { containedNode =>
        s"""def ${containedNode.localName}: ${getCompleteType(containedNode)}"""
      }.mkString(lineSeparator)

      val delegatingContainedNodeAccessors = nodeType.containedNodes.map { containedNode =>
        import Property.Cardinality

        val tpe = containedNode.classNameForStoredNode
        val src = containedNode.cardinality match {
          case Cardinality.One(_) =>
            s"""def ${containedNode.localName}: $tpe = get().${containedNode.localName}"""
          case Cardinality.ZeroOrOne =>
            s"""def ${containedNode.localName}: Option[$tpe] = get().${containedNode.localName}"""
          case Cardinality.List =>
            s"""def ${containedNode.localName}: collection.immutable.IndexedSeq[$tpe] = get().${containedNode.localName}"""
        }

        s"""${docAnnotationMaybe(containedNode.comment)}
           |$src
           |""".stripMargin
      }.mkString(lineSeparator)

      val nodeBaseImpl =
        s"""trait ${className}Base extends AbstractNode $mixinsForExtendedNodesBase $mixinsForMarkerTraits {
           |  def asStored : StoredNode = this.asInstanceOf[StoredNode]
           |
           |  ${Helpers.propertyAccessors(properties)}
           |
           |  $abstractContainedNodeAccessors
           |}
           |""".stripMargin

      val neighborAccessorDelegators = neighborInfos.map { case (neighborInfo, direction) =>
        val edgeAccessorName = neighborAccessorNameForEdge(neighborInfo.edge, direction)
        val nodeDelegators = neighborInfo.nodeInfos.sortBy(_.neighborNode.name).collect {
          case neighborNodeInfo if !neighborNodeInfo.isInherited =>
            val accessorNameForNode = accessorName(neighborNodeInfo)
            s"""/** ${neighborNodeInfo.customStepDoc.getOrElse("")}
               |  * Traverse to ${neighborNodeInfo.neighborNode.name} via ${neighborNodeInfo.edge.name} $direction edge.
               |  */  ${docAnnotationMaybe(neighborNodeInfo.customStepDoc)}
               |def $accessorNameForNode: ${neighborNodeInfo.returnType} = get().$accessorNameForNode""".stripMargin
        }.mkString(lineSeparator)

        val neighborNodeClass = neighborInfo.deriveNeighborNodeType.getOrElse(schema.anyNode).className
        s"""def $edgeAccessorName: Iterator[$neighborNodeClass] = get().$edgeAccessorName
           |override def _$edgeAccessorName = get()._$edgeAccessorName
           |
           |$nodeDelegators
           |""".stripMargin
      }.mkString(lineSeparator)

      val nodeRefImpl = {
        val propertyDelegators = properties.map { key =>
          val name = camelCase(key.name)
          s"""override def $name: ${getCompleteType(key)} = get().$name"""
        }.mkString(lineSeparator)

        val propertyDefaultValues = propertyDefaultValueImpl(s"$className.PropertyDefaults", properties)

        s"""class $className(graph_4762: Graph, id_4762: Long /*cf https://github.com/scala/bug/issues/4762 */) extends NodeRef[$classNameDb](graph_4762, id_4762)
           |  with ${className}Base
           |  with StoredNode
           |  $mixinsForExtendedNodes {
           |  $propertyDelegators
           |  $propertyDefaultValues
           |  $delegatingContainedNodeAccessors
           |    $neighborAccessorDelegators
           |  // In view of https://github.com/scala/bug/issues/4762 it is advisable to use different variable names in
           |  // patterns like `class Base(x:Int)` and `class Derived(x:Int) extends Base(x)`.
           |  // This must become `class Derived(x_4762:Int) extends Base(x_4762)`.
           |  // Otherwise, it is very hard to figure out whether uses of the identifier `x` refer to the base class x
           |  // or the derived class x.
           |  // When using that pattern, the class parameter `x_47672` should only be used in the `extends Base(x_4762)`
           |  // clause and nowhere else. Otherwise, the compiler may well decide that this is not just a constructor
           |  // parameter but also a field of the class, and we end up with two `x` fields. At best, this wastes memory;
           |  // at worst both fields go out-of-sync for hard-to-debug correctness bugs.
           |
           |
           |    override def fromNewNode(newNode: NewNode, mapping: NewNode => StoredNode): Unit = get().fromNewNode(newNode, mapping)
           |    override def canEqual(that: Any): Boolean = get.canEqual(that)
           |    override def label: String = {
           |      $className.Label
           |    }
           |
           |    override def productElementName(n: Int): String =
           |      n match {
           |        $productElementNames
           |      }
           |
           |    override def productElement(n: Int): Any =
           |      n match {
           |        $productElementAccessors
           |      }
           |
           |    override def productPrefix = "$className"
           |    override def productArity = ${productElements.size}
           |}
           |""".stripMargin
      }

      val neighborAccessors = neighborInfos.map { case (neighborInfo, direction) =>
        val edgeAccessorName = neighborAccessorNameForEdge(neighborInfo.edge, direction)
        val neighborType = neighborInfo.deriveNeighborNodeType.getOrElse(schema.anyNode).className
        val offsetPosition = neighborInfo.offsetPosition

        val nodeAccessors = neighborInfo.nodeInfos.collect {
          case neighborNodeInfo if !neighborNodeInfo.isInherited =>
            val accessorImpl0 = s"$edgeAccessorName.collectAll[${neighborNodeInfo.neighborNode.className}]"
            val accessorImpl1 = neighborNodeInfo.consolidatedCardinality match {
              case EdgeType.Cardinality.One =>
                s"""try { $accessorImpl0.next() } catch {
                   |  case e: java.util.NoSuchElementException =>
                   |    throw new overflowdb.SchemaViolationException("$direction edge with label ${neighborNodeInfo.edge.name} to an adjacent ${neighborNodeInfo.neighborNode.name} is mandatory, but not defined for this ${nodeType.name} node with id=" + id, e)
                   |}""".stripMargin
              case EdgeType.Cardinality.ZeroOrOne => s"$accessorImpl0.nextOption()"
              case _ => accessorImpl0
            }
            s"def ${accessorName(neighborNodeInfo)}: ${neighborNodeInfo.returnType} = $accessorImpl1"
        }.mkString(lineSeparator)

        s"""def $edgeAccessorName: Iterator[$neighborType] = createAdjacentNodeScalaIteratorByOffSet[$neighborType]($offsetPosition)
           |override def _$edgeAccessorName = createAdjacentNodeScalaIteratorByOffSet[StoredNode]($offsetPosition)
           |$nodeAccessors
           |""".stripMargin
      }.mkString(lineSeparator)

      val updateSpecificPropertyImpl: String = {
        import Property.Cardinality
        def caseEntry(name: String, accessorName: String, cardinality: Cardinality, baseType: String) = {
          val setter = cardinality match {
            case Cardinality.One(_) | Cardinality.ZeroOrOne =>
              s"value.asInstanceOf[$baseType]"
            case Cardinality.List =>
              s"""value match {
                 |  case null => collection.immutable.ArraySeq.empty
                 |  case singleValue: $baseType => collection.immutable.ArraySeq(singleValue)
                 |  case coll: IterableOnce[Any] if coll.iterator.isEmpty => collection.immutable.ArraySeq.empty
                 |  case arr: Array[_] if arr.isEmpty => collection.immutable.ArraySeq.empty
                 |  case arr: Array[_] => collection.immutable.ArraySeq.unsafeWrapArray(arr).asInstanceOf[IndexedSeq[$baseType]]
                 |  case jCollection: java.lang.Iterable[_]  =>
                 |    if (jCollection.iterator.hasNext) {
                 |      collection.immutable.ArraySeq.unsafeWrapArray(
                 |        jCollection.asInstanceOf[java.util.Collection[$baseType]].iterator.asScala.toArray)
                 |    } else collection.immutable.ArraySeq.empty
                 |  case iter: Iterable[_] =>
                 |    if(iter.nonEmpty) {
                 |      collection.immutable.ArraySeq.unsafeWrapArray(iter.asInstanceOf[Iterable[$baseType]].toArray)
                 |    } else collection.immutable.ArraySeq.empty
                 |}""".stripMargin
          }
          s"""|case "$name" => this._$accessorName = $setter"""
        }

        val forKeys = properties.map(p => caseEntry(p.name, camelCase(p.name), p.cardinality, typeFor(p))).mkString(lineSeparator)

        val forContainedNodes = nodeType.containedNodes.map(containedNode =>
          caseEntry(containedNode.localName, containedNode.localName, containedNode.cardinality, containedNode.classNameForStoredNode)
        ).mkString(lineSeparator)

        s"""override protected def updateSpecificProperty(key:String, value: Object): Unit = {
           |  key match {
           |  $forKeys
           |  $forContainedNodes
           |    case _ => PropertyErrorRegister.logPropertyErrorIfFirst(getClass, key)
           |  }
           |}""".stripMargin
      }

      val propertyImpl: String = {
        val forKeys = properties.map(key =>
          s"""|      case "${key.name}" => this._${camelCase(key.name)}"""
        ).mkString(lineSeparator)

        val forContainedKeys = nodeType.containedNodes.map{containedNode =>
          val name = containedNode.localName
          s"""|      case "$name" => this._$name"""
        }.mkString(lineSeparator)

        s"""override def property(key:String): Any = {
           |  key match {
           |    $forKeys
           |    $forContainedKeys
           |    case _ => null
           |  }
           |}""".stripMargin
      }

      def propertyBasedFields(properties: Seq[Property[?]]): String = {
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
        }.mkString(lineSeparator)
      }

      val classImpl =
        s"""class $classNameDb(ref: NodeRef[NodeDb]) extends NodeDb(ref) with StoredNode
           |  $mixinsForExtendedNodes with ${className}Base {
           |
           |  override def layoutInformation: NodeLayoutInformation = $className.layoutInformation
           |
           |  ${propertyBasedFields(properties)}
           |
           |  $containedNodesAsMembers
           |
           |  /** faster than the default implementation */
           |  override def propertiesMap: java.util.Map[String, Any] =
           |    $propertiesMapImpl
           |
           |  /** faster than the default implementation */
           |  override def propertiesMapForStorage: java.util.Map[String, Any] =
           |    $propertiesMapForStorageImpl
           |
           |  import overflowdb.traversal._
           |  $neighborAccessors
           |
           |  override def label: String = {
           |    $className.Label
           |  }
           |
           |  override def productElementName(n: Int): String =
           |    n match {
           |      $productElementNames
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
           |  override def removeSpecificProperty(key: String): Unit =
           |    this.updateSpecificProperty(key, null)
           |
           |override def _initializeFromDetached(data: overflowdb.DetachedNodeData, mapper: java.util.function.Function[overflowdb.DetachedNodeData, Node]) =
           |    fromNewNode(data.asInstanceOf[NewNode], nn=>mapper.apply(nn).asInstanceOf[StoredNode])
           |
           |  $fromNew
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
    lazy val nodeTraversalImplicits = {
      def implicitForNodeType(name: String): String = {
        val traversalName = s"${name}TraversalExtGen"
        s"implicit def to$traversalName[NodeType <: $name](trav: IterableOnce[NodeType]): ${traversalName}[NodeType] = new $traversalName(trav.iterator)"
      }

      val implicitsForNodeTraversals =
        schema.nodeTypes.map(_.className).sorted.map(implicitForNodeType).mkString(lineSeparator)

      val implicitsForNodeBaseTypeTraversals =
        schema.nodeBaseTypes.map(_.className).sorted.map(implicitForNodeType).mkString(lineSeparator)
      val implicitsForAnyNodeTraversals = implicitForNodeType("StoredNode")
      //todo: Relocate to specific file?
      val edgeExt = schema.edgeTypes.map{et =>
        val acc = s"${Helpers.camelCase(et.name)}"
        s"""def _${acc}Out: Iterator[StoredNode] = traversal.flatMap{_._${acc}Out}
           |def _${acc}In: Iterator[StoredNode] = traversal.flatMap{_._${acc}In} """.stripMargin
      }.mkString("\n")
      val anyNodeExtClass =
        s"""class StoredNodeTraversalExtGen[NodeType <: StoredNode](val traversal: Iterator[NodeType]) extends AnyVal {
           |  ${edgeExt}
           |}""".stripMargin

      s"""package $traversalsPackage
         |
         |import $nodesPackage._
         |
         |trait NodeTraversalImplicits extends NodeBaseTypeTraversalImplicits {
         |  $implicitsForAnyNodeTraversals
         |
         |  $implicitsForNodeTraversals
         |}
         |
         |// lower priority implicits for base types
         |trait NodeBaseTypeTraversalImplicits extends overflowdb.traversal.Implicits {
         |  $implicitsForNodeBaseTypeTraversals
         |}
         |
         |$anyNodeExtClass
         |""".stripMargin
    }

    def generateCustomStepNameTraversals(nodeType: AbstractNodeType): Seq[String] = {
      for {
        direction <- Seq(Direction.IN, Direction.OUT)
        case AdjacentNode(viaEdge, neighbor, cardinality, Some(customStepName), customStepDoc) <- nodeType.edges(direction).sortBy(_.customStepName)
      } yield {
        val mapOrFlatMap = cardinality match {
          case Cardinality.One => "map"
          case Cardinality.ZeroOrOne | Cardinality.List => "flatMap"
        }
        s"""/** ${customStepDoc.getOrElse("")}
           |  * Traverse to ${neighbor.name} via ${viaEdge.name} $direction edge.
           |  */ ${docAnnotationMaybe(customStepDoc)}
           |def $customStepName: Iterator[${neighbor.className}] =
           |  traversal.$mapOrFlatMap(_.$customStepName)
           |""".stripMargin
      }
    }

    def generatePropertyTraversals(properties: Seq[Property[?]]): Seq[String] = {
      import Property.Cardinality
      properties.map { property =>
        val nameCamelCase = camelCase(property.name)
        val baseType = typeFor(property)
        val cardinality = property.cardinality

        val mapOrFlatMap = cardinality match {
          case Cardinality.One(_) => "map"
          case Cardinality.ZeroOrOne | Cardinality.List => "flatMap"
        }

        val filterStepsForSingleString =
          s"""/**
             |  * Traverse to nodes where the $nameCamelCase matches the regular expression `value`
             |  * */
             |def $nameCamelCase(pattern: $baseType): Iterator[NodeType] = {
             |  if(!Misc.isRegex(pattern)){
             |    ${nameCamelCase}Exact(pattern)
             |  } else {
             |    overflowdb.traversal.filter.StringPropertyFilter.regexp(traversal)(_.$nameCamelCase, pattern)
             |  }
             |}
             |
             |/**
             |  * Traverse to nodes where the $nameCamelCase matches at least one of the regular expressions in `values`
             |  * */
             |def $nameCamelCase(patterns: $baseType*): Iterator[NodeType] =
             |  overflowdb.traversal.filter.StringPropertyFilter.regexpMultiple(traversal)(_.$nameCamelCase, patterns)
             |
             |/**
             |  * Traverse to nodes where $nameCamelCase matches `value` exactly.
             |  * */
             |def ${nameCamelCase}Exact(value: $baseType): Iterator[NodeType] = {
             |  val fastResult = traversal match {
             |    case init: overflowdb.traversal.InitialTraversal[NodeType] => init.getByIndex("${property.name}", value).getOrElse(null)
             |    case _ => null
             |  }
             |  if(fastResult != null) fastResult
             |  else traversal.filter{node => node.${nameCamelCase} == value}
             |  }
             |
             |/**
             |  * Traverse to nodes where $nameCamelCase matches one of the elements in `values` exactly.
             |  * */
             |def ${nameCamelCase}Exact(values: $baseType*): Iterator[NodeType] = {
             |  if (values.size == 1)
             |    ${nameCamelCase}Exact(values.head)
             |  else
             |    overflowdb.traversal.filter.StringPropertyFilter.exactMultiple[NodeType, $baseType](traversal, node => Some(node.$nameCamelCase), values, "${property.name}")
             |}
             |
             |
             |/**
             |  * Traverse to nodes where $nameCamelCase does not match the regular expression `value`.
             |  * */
             |def ${nameCamelCase}Not(pattern: $baseType): Iterator[NodeType] = {
             |  if(!Misc.isRegex(pattern)){
             |    traversal.filter{node => node.${nameCamelCase} != pattern}
             |  } else {
             |    overflowdb.traversal.filter.StringPropertyFilter.regexpNot(traversal)(_.$nameCamelCase, pattern)
             |  }
             |}
             |
             |/**
             |  * Traverse to nodes where $nameCamelCase does not match any of the regular expressions in `values`.
             |  * */
             |def ${nameCamelCase}Not(patterns: $baseType*): Iterator[NodeType] = {
             |    overflowdb.traversal.filter.StringPropertyFilter.regexpNotMultiple(traversal)(_.$nameCamelCase, patterns)
             | }
             |""".stripMargin

        val filterStepsForOptionalString =
          s"""/**
             |  * Traverse to nodes where the $nameCamelCase matches the regular expression `value`
             |  * */
             |def $nameCamelCase(pattern: $baseType): Iterator[NodeType] = {
             |  if(!Misc.isRegex(pattern)){
             |    traversal.filter{node => node.$nameCamelCase.isDefined && node.${nameCamelCase}.get == pattern}
             |  } else {
             |    overflowdb.traversal.filter.StringPropertyFilter.regexp(traversal.filter(_.$nameCamelCase.isDefined))(_.$nameCamelCase.get, pattern)
             |  }
             |}
             |
             |/**
             |  * Traverse to nodes where the $nameCamelCase matches at least one of the regular expressions in `values`
             |  * */
             |def $nameCamelCase(patterns: $baseType*): Iterator[NodeType] = {
             |  overflowdb.traversal.filter.StringPropertyFilter.regexpMultiple(traversal.filter(_.$nameCamelCase.isDefined))(_.$nameCamelCase.get, patterns)
             |}
             |
             |/**
             |  * Traverse to nodes where $nameCamelCase matches `value` exactly.
             |  * */
             |def ${nameCamelCase}Exact(value: $baseType): Iterator[NodeType] =
             |  traversal.filter{node => node.$nameCamelCase.contains(value)}
             |
             |/**
             |  * Traverse to nodes where $nameCamelCase matches one of the elements in `values` exactly.
             |  * */
             |def ${nameCamelCase}Exact(values: $baseType*): Iterator[NodeType] = {
             |  if (values.size == 1)
             |    ${nameCamelCase}Exact(values.head)
             |  else
             |    overflowdb.traversal.filter.StringPropertyFilter.exactMultiple[NodeType, $baseType](traversal, _.$nameCamelCase, values, "${property.name}")
             |}
             |
             |/**
             |  * Traverse to nodes where $nameCamelCase does not match the regular expression `value`.
             |  * */
             |def ${nameCamelCase}Not(pattern: $baseType): Iterator[NodeType] = {
             |  if(!Misc.isRegex(pattern)){
             |    traversal.filter{node => node.$nameCamelCase.isEmpty || node.${nameCamelCase}.get != pattern}
             |  } else {
             |    overflowdb.traversal.filter.StringPropertyFilter.regexpNot(traversal.filter(_.$nameCamelCase.isDefined))(_.$nameCamelCase.get, pattern)
             |  }
             |}
             |
             |/**
             |  * Traverse to nodes where $nameCamelCase does not match any of the regular expressions in `values`.
             |  * */
             |def ${nameCamelCase}Not(patterns: $baseType*): Iterator[NodeType] = {
             |  overflowdb.traversal.filter.StringPropertyFilter.regexpNotMultiple(traversal.filter(_.$nameCamelCase.isDefined))(_.$nameCamelCase.get, patterns)
             | }
             |""".stripMargin

        val filterStepsForSingleBoolean =
          s"""/**
             |  * Traverse to nodes where the $nameCamelCase equals the given `value`
             |  * */
             |def $nameCamelCase(value: $baseType): Iterator[NodeType] =
             |  traversal.filter{_.$nameCamelCase == value}
             |
             |/**
             |  * Traverse to nodes where $nameCamelCase is not equal to the given `value`.
             |  * */
             |def ${nameCamelCase}Not(value: $baseType): Iterator[NodeType] =
             |  traversal.filter{_.$nameCamelCase != value}
             |""".stripMargin

        val filterStepsForOptionalBoolean =
          s"""/**
             |  * Traverse to nodes where the $nameCamelCase equals the given `value`
             |  * */
             |def $nameCamelCase(value: $baseType): Iterator[NodeType] =
             |  traversal.filter{node => node.${nameCamelCase}.isDefined && node.$nameCamelCase.get == value}
             |
             |/**
             |  * Traverse to nodes where $nameCamelCase is not equal to the given `value`.
             |  * */
             |def ${nameCamelCase}Not(value: $baseType): Iterator[NodeType] =
             |  traversal.filter{node => !node.${nameCamelCase}.isDefined || node.$nameCamelCase.get == value}
             |""".stripMargin

        val filterStepsForSingleInt =
          s"""/**
             |  * Traverse to nodes where the $nameCamelCase equals the given `value`
             |  * */
             |def $nameCamelCase(value: $baseType): Iterator[NodeType] =
             |  traversal.filter{_.$nameCamelCase == value}
             |
             |/**
             |  * Traverse to nodes where the $nameCamelCase equals at least one of the given `values`
             |  * */
             |def $nameCamelCase(values: $baseType*): Iterator[NodeType] = {
             |  val vset = values.toSet
             |  traversal.filter{node => vset.contains(node.$nameCamelCase)}
             |}
             |
             |/**
             |  * Traverse to nodes where the $nameCamelCase is greater than the given `value`
             |  * */
             |def ${nameCamelCase}Gt(value: $baseType): Iterator[NodeType] =
             |  traversal.filter{_.$nameCamelCase > value}
             |
             |/**
             |  * Traverse to nodes where the $nameCamelCase is greater than or equal the given `value`
             |  * */
             |def ${nameCamelCase}Gte(value: $baseType): Iterator[NodeType] =
             |  traversal.filter{_.$nameCamelCase >= value}
             |
             |/**
             |  * Traverse to nodes where the $nameCamelCase is less than the given `value`
             |  * */
             |def ${nameCamelCase}Lt(value: $baseType): Iterator[NodeType] =
             |  traversal.filter{_.$nameCamelCase < value}
             |
             |/**
             |  * Traverse to nodes where the $nameCamelCase is less than or equal the given `value`
             |  * */
             |def ${nameCamelCase}Lte(value: $baseType): Iterator[NodeType] =
             |  traversal.filter{_.$nameCamelCase <= value}
             |
             |/**
             |  * Traverse to nodes where $nameCamelCase is not equal to the given `value`.
             |  * */
             |def ${nameCamelCase}Not(value: $baseType): Iterator[NodeType] =
             |  traversal.filter{_.$nameCamelCase != value}
             |
             |/**
             |  * Traverse to nodes where $nameCamelCase is not equal to any of the given `values`.
             |  * */
             |def ${nameCamelCase}Not(values: $baseType*): Iterator[NodeType] = {
             |  val vset = values.toSet
             |  traversal.filter{node => !vset.contains(node.$nameCamelCase)}
             |}
             |""".stripMargin

        val filterStepsForOptionalInt =
          s"""/**
             |  * Traverse to nodes where the $nameCamelCase equals the given `value`
             |  * */
             |def $nameCamelCase(value: $baseType): Iterator[NodeType] =
             |  traversal.filter{node => node.$nameCamelCase.isDefined && node.$nameCamelCase.get == value}
             |
             |/**
             |  * Traverse to nodes where the $nameCamelCase equals at least one of the given `values`
             |  * */
             |def $nameCamelCase(values: $baseType*): Iterator[NodeType] = {
             |  val vset = values.toSet
             |  traversal.filter{node => node.$nameCamelCase.isDefined && vset.contains(node.$nameCamelCase.get)}
             |}
             |
             |/**
             |  * Traverse to nodes where the $nameCamelCase is greater than the given `value`
             |  * */
             |def ${nameCamelCase}Gt(value: $baseType): Iterator[NodeType] =
             |  traversal.filter{node => node.$nameCamelCase.isDefined && node.$nameCamelCase.get > value}
             |
             |/**
             |  * Traverse to nodes where the $nameCamelCase is greater than or equal the given `value`
             |  * */
             |def ${nameCamelCase}Gte(value: $baseType): Iterator[NodeType] =
             |  traversal.filter{node => node.$nameCamelCase.isDefined && node.$nameCamelCase.get >= value}
             |
             |/**
             |  * Traverse to nodes where the $nameCamelCase is less than the given `value`
             |  * */
             |def ${nameCamelCase}Lt(value: $baseType): Iterator[NodeType] =
             |  traversal.filter{node => node.$nameCamelCase.isDefined && node.$nameCamelCase.get < value}
             |
             |/**
             |  * Traverse to nodes where the $nameCamelCase is less than or equal the given `value`
             |  * */
             |def ${nameCamelCase}Lte(value: $baseType): Iterator[NodeType] =
             |  traversal.filter{node => node.$nameCamelCase.isDefined && node.$nameCamelCase.get <= value}
             |
             |/**
             |  * Traverse to nodes where $nameCamelCase is not equal to the given `value`.
             |  * */
             |def ${nameCamelCase}Not(value: $baseType): Iterator[NodeType] =
             |  traversal.filter{node => !node.$nameCamelCase.isDefined || node.$nameCamelCase.get != value}
             |
             |/**
             |  * Traverse to nodes where $nameCamelCase is not equal to any of the given `values`.
             |  * */
             |def ${nameCamelCase}Not(values: $baseType*): Iterator[NodeType] = {
             |  val vset = values.toSet
             |  traversal.filter{node => !node.$nameCamelCase.isDefined || !vset.contains(node.$nameCamelCase.get)}
             |}
             |""".stripMargin

        val filterStepsGenericSingle =
          s"""/**
             |  * Traverse to nodes where the $nameCamelCase equals the given `value`
             |  * */
             |def $nameCamelCase(value: $baseType): Iterator[NodeType] =
             |  traversal.filter{_.$nameCamelCase == value}
             |
             |/**
             |  * Traverse to nodes where the $nameCamelCase equals at least one of the given `values`
             |  * */
             |def $nameCamelCase(values: $baseType*): Iterator[NodeType] = {
             |  val vset = values.toSet
             |  traversal.filter{node => !vset.contains(node.$nameCamelCase)}
             |}
             |
             |/**
             |  * Traverse to nodes where $nameCamelCase is not equal to the given `value`.
             |  * */
             |def ${nameCamelCase}Not(value: $baseType): Iterator[NodeType] =
             |  traversal.filter{_.$nameCamelCase != value}
             |
             |/**
             |  * Traverse to nodes where $nameCamelCase is not equal to any of the given `values`.
             |  * */
             |def ${nameCamelCase}Not(values: $baseType*): Iterator[NodeType] = {
             |  val vset = values.toSet
             |  traversal.filter{node => !vset.contains(node.$nameCamelCase)}
             |}
             |""".stripMargin

        val filterStepsGenericOption =
          s"""/**
             |  * Traverse to nodes where the $nameCamelCase equals the given `value`
             |  * */
             |def $nameCamelCase(value: $baseType): Iterator[NodeType] =
             |  traversal.filter{node => node.$nameCamelCase.isDefined && node.$nameCamelCase.get == value}
             |
             |/**
             |  * Traverse to nodes where the $nameCamelCase equals at least one of the given `values`
             |  * */
             |def $nameCamelCase(values: $baseType*): Iterator[NodeType] = {
             |  val vset = values.toSet
             |  traversal.filter{node => node.$nameCamelCase.isDefined && !vset.contains(node.$nameCamelCase.get)}
             |}
             |
             |/**
             |  * Traverse to nodes where $nameCamelCase is not equal to the given `value`.
             |  * */
             |def ${nameCamelCase}Not(value: $baseType): Iterator[NodeType] =
             |  traversal.filter{node => !node.$nameCamelCase.isDefined || node.$nameCamelCase.get != value}
             |
             |/**
             |  * Traverse to nodes where $nameCamelCase is not equal to any of the given `values`.
             |  * */
             |def ${nameCamelCase}Not(values: $baseType*): Iterator[NodeType] = {
             |  val vset = values.toSet
             |  traversal.filter{node => !node.$nameCamelCase.isDefined || !vset.contains(node.$nameCamelCase.get)}
             |}
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

        s"""/** Traverse to $nameCamelCase property */
           |def $nameCamelCase: Iterator[$baseType] =
           |  traversal.$mapOrFlatMap(_.$nameCamelCase)
           |
           |$filterSteps
           |""".stripMargin
      }
    }

    def generateNodeTraversalExt(nodeType: AbstractNodeType): String = {
      val customStepNameTraversals = generateCustomStepNameTraversals(nodeType)
      val propertyTraversals = generatePropertyTraversals(nodeType.properties)
      val className = nodeType.className

      s"""package $traversalsPackage
         |
         |import overflowdb.traversal._
         |import $nodesPackage._
         |
         |/** Traversal steps for $className */
         |class ${className}TraversalExtGen[NodeType <: $className](val traversal: Iterator[NodeType]) extends AnyVal {
         |
         |${customStepNameTraversals.mkString(System.lineSeparator)}
         |
         |${propertyTraversals.mkString(System.lineSeparator)}
         |
         |}""".stripMargin
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
    schema.allNodeTypes.foreach { nodeType =>
      val src = generateNodeTraversalExt(nodeType)
      val srcFile = nodeType.className + ".scala"
      results.append(baseDir.createChild(srcFile).write(src))
    }
    results.toSeq
  }

  /**
   * Useful string extensions to avoid Scala version incompatible interpolations.
   */
  implicit class StringExt(s: String) {

    def quote: String = {
      s""""$s""""
    }

  }

  /** generates classes to easily add new nodes to the graph
    * this ability could have been added to the existing nodes, but it turned out as a different specialisation,
    * since e.g. `id` is not set before adding it to the graph */
  protected def writeNewNodeFile(outputDir: File): File = {
    val staticHeader =
      s"""package $nodesPackage
         |
         |/** base type for all nodes that can be added to a graph, e.g. the diffgraph */
         |abstract class NewNode extends AbstractNode with overflowdb.DetachedNodeData with Product {
         |  def properties: Map[String, Any]
         |  def copy: this.type
         |  type StoredType <: StoredNode
         |  private var refOrId: Object = null
         |  override def getRefOrId(): Object = refOrId
         |  override def setRefOrId(r: Object): Unit = {this.refOrId = r}
         |  def stored: Option[StoredType] = if(refOrId != null && refOrId.isInstanceOf[StoredNode]) Some(refOrId).asInstanceOf[Option[StoredType]] else None
         |  def isValidOutNeighbor(edgeLabel: String, n: NewNode): Boolean
         |  def isValidInNeighbor(edgeLabel: String, n: NewNode): Boolean
         |}
         |""".stripMargin

    def generateNewNodeSource(nodeType: NodeType, properties: Seq[Property[?]], inEdges: Map[String, Set[String]], outEdges: Map[String, Set[String]]) = {
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

      }.mkString("\n")

      val propertiesMapImpl = {
        val putKeysImpl = properties
          .map { key =>
            val memberName = camelCase(key.name)
            import Property.Cardinality
            key.cardinality match {
              case Cardinality.One(default) =>
                val isDefaultValueImpl = defaultValueCheckImpl(memberName, default)
                s"""if (!($isDefaultValueImpl)) { res += "${key.name}" -> $memberName }"""
              case Cardinality.ZeroOrOne =>
                s"""$memberName.map { value => res += "${key.name}" -> value }"""
              case Cardinality.List =>
                s"""if ($memberName != null && $memberName.nonEmpty) { res += "${key.name}" -> $memberName }"""
            }
          }
        val putRefsImpl = nodeType.containedNodes.map { key =>
          import Property.Cardinality
          val memberName = key.localName
          key.cardinality match {
            case Cardinality.One(default) =>
              val isDefaultValueImpl = defaultValueCheckImpl(memberName, default)
              s"""if (!($isDefaultValueImpl)) { res += "$memberName" -> $memberName }"""
            case Cardinality.ZeroOrOne =>
              s"""$memberName.map { value => res += "$memberName" -> value }"""
            case Cardinality.List =>
              s"""if ($memberName != null && $memberName.nonEmpty) { res += "$memberName" -> $memberName }"""
          }
        }

        val propertiesImpl = {
          val lines = putKeysImpl ++ putRefsImpl
          if (lines.nonEmpty) {
            s"""var res = Map[String, Any]()
               |${lines.mkString(lineSeparator)}
               |res""".stripMargin
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
      }.mkString(lineSeparator)

      val copyFieldsImpl = fieldDescriptions.map { field =>
        val memberName = field.name
        s"newInstance.$memberName = this.$memberName"
      }.mkString(lineSeparator)

      val classNameNewNode = s"New$nodeClassName"

      val productElements = fieldDescriptions.reverse.zipWithIndex
      val productElementAccessors = productElements.map {
        case (fieldDescription, index) =>
          s"case $index => this.${fieldDescription.name}"
      }.mkString(lineSeparator)
      val productElementNames = productElements.map {
        case (fieldDescription, index) =>
          s"""case $index => "${fieldDescription.name}""""
      }.mkString(lineSeparator)

      def neighborEdgeStr(es: Map[String, Set[String]]): String =
        es.toSeq.sortBy(_._1).map { case (k, vs) => s"$k -> Set(${vs.toSeq.sorted.mkString(", ")})" }.mkString(", ")

      s"""object $classNameNewNode {
         |  def apply(): $classNameNewNode = new $classNameNewNode
         |
         |  private val outNeighbors: Map[String, Set[String]] = Map(${neighborEdgeStr(outEdges)})
         |  private val inNeighbors: Map[String, Set[String]] = Map(${neighborEdgeStr(inEdges)})
         |
         |}
         |
         |class $classNameNewNode
         |  extends NewNode with ${nodeClassName}Base $mixins {
         |  type StoredType = $nodeClassName
         |
         |  $memberVariables
         |
         |  override def label: String = "${nodeType.name}"
         |
         |  override def copy: this.type = {
         |    val newInstance = new New$nodeClassName
         |    $copyFieldsImpl
         |    newInstance.asInstanceOf[this.type]
         |  }
         |
         |  $propertySettersImpl
         |
         |  $propertiesMapImpl
         |
         |  import $classNameNewNode.{outNeighbors, inNeighbors}
         |
         |  override def isValidOutNeighbor(edgeLabel: String, n: NewNode): Boolean =
         |    outNeighbors.getOrElse(edgeLabel, Set.empty).contains(n.label)
         |
         |  override def isValidInNeighbor(edgeLabel: String, n: NewNode): Boolean =
         |    inNeighbors.getOrElse(edgeLabel, Set.empty).contains(n.label)
         |
         |  override def productElement(n: Int): Any =
         |    n match {
         |      $productElementAccessors
         |      case _ => null
         |    }
         |
         |  override def productElementName(n: Int): String =
         |    n match {
         |      $productElementNames
         |      case _ => ""
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
    val allNodeTypes = schema.allNodeTypes.toSet

    def flattenNeighbors(x: AbstractNodeType) = x.extendzRecursively.flatMap(y => y.subtypes(schema.allNodeTypes.toSet))

    def neighborMapping(x: AdjacentNode): Set[(String, String)] = {
      val edge = x.viaEdge.name.quote
      (x.neighbor +: flattenNeighbors(x.neighbor)).map(y => (edge, y.name.quote)).toSet
    }

    def edgeNeighborToMap(xs: Set[(String, String)]): Map[String, Set[String]] =
      xs.groupBy(_._1).map { case (edge, edgeNeighbors) => edge -> edgeNeighbors.map(_._2)}

    val src = schema.nodeTypes.map { nodeType =>
      val baseTypeInEdges = nodeType
        .extendzRecursively
        .flatMap(_.subtypes(allNodeTypes))
        .flatMap(_.inEdges)
        .flatMap(neighborMapping)
        .toSet
      val inEdges =  nodeType.inEdges.map(x => (x.viaEdge.name.quote, x.neighbor.name.quote)).toSet
      val baseTypeOutEdges = nodeType
        .extendzRecursively
        .flatMap(_.subtypes(allNodeTypes))
        .flatMap(_.outEdges)
        .flatMap(neighborMapping)
        .toSet
      val outEdges =  nodeType.outEdges.map(x => (x.viaEdge.name.quote, x.neighbor.name.quote)).toSet
      generateNewNodeSource(
        nodeType,
        nodeType.properties,
        edgeNeighborToMap(baseTypeInEdges ++ inEdges),
        edgeNeighborToMap(baseTypeOutEdges ++ outEdges)
      )
    }.mkString(lineSeparator)
    outfile.write(s"""$staticHeader
                     |$src
                     |""".stripMargin)
  }

  private def deleteRecursively(file: java.io.File): Unit = {
    if (file.isDirectory)
      file.listFiles.foreach(deleteRecursively)
    if (file.exists)
      file.delete()
  }
}

object CodeGen {
  case class ConstantContext(name: String, source: String, documentation: Option[String])
}
