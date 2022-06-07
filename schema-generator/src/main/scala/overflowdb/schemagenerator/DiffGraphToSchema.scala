package overflowdb.schemagenerator

import overflowdb.BatchedUpdate._
import overflowdb.DetachedNodeGeneric

import java.lang.System.lineSeparator
import java.nio.charset.Charset
import java.nio.file.{Files, Path}
import scala.collection.immutable.ArraySeq
import scala.collection.mutable
import scala.jdk.CollectionConverters.IterableHasAsScala

/**
  * Create a base schema definition from a DiffGraph, as e.g. generated by overflowdb.formats.GraphMLImporter
  * This will give you a baseline for creating an overflowdb schema, so you can generate domain types etc.
  *
  * You will need to fill in some gaps later, e.g.
  * - properties: cardinalities: defaults is `optional - you can make it `.mandatory(someDefaultValue)` or `.asList`
  * - properties: reuse across different nodes/edges, comments
  * - edges: cardinalities, comments
  * - nodes: hierarchy: introduce node base types
  * - refactor for readability: order, split into files etc.
  * - many more
  *
  * Note: this isn't optimised for performance and not tested on large input diffgraphs.
  */
class DiffGraphToSchema(domainName: String, schemaPackage: String, targetPackage: String) {
  import DiffGraphToSchema._

  def toFile(diffGraph: DiffGraph, outFile: Path): Unit = {
    val sourceString = asSourceString(diffGraph)
    Files.write(outFile, sourceString.getBytes(Charset.forName("UTF-8")))
  }

  def asSourceString(diffGraph: DiffGraph): String = {
    /** To keep things simple and have a bearable memory footprint, we use mutable datastructures
      * that we build up as we iterate over nodes and edges. We may need to adjust a few names to
      * resolve disambiguities later, so we first collect everything we need and generate the sources
      * in a second step. */
    val scopeBuilder = new ScopeBuilder

    diffGraph.iterator().forEachRemaining {
      case node: DetachedNodeGeneric => parseNode(node, scopeBuilder)
      case edge: CreateEdge => parseEdge(edge, scopeBuilder)
    }

    val scope = scopeBuilder.build()

    /** properties: may have the same name between different nodes / edges...
      * if there's no ambiguities, just use the property name for the schema, e.g. `val property1 = ...`,
      * otherwise, prefix the property with the identifier of the element, e.g. `val nodeAProperty1 = ...`
      * n.b. we need to make sure we refer to that exact property name when we create the nodes/edges in the schema
      */
    val propertiesSrcs = Seq.newBuilder[String] // added to as we write nodes / edges

   val nodes = scope.nodeTypes.map { nodeTypeDetails =>
     val label = nodeTypeDetails.label
     val schemaNodeName =
       if (scope.nameHasAmbiguities(label)) camelCase(s"${label}Node")
       else camelCase(label)
     val nodeProperties = nodeTypeDetails.propertyByName.values.toSeq
     val maybeAddProperties = nodeProperties.sortBy(_.name) match {
       case seq if seq.isEmpty => ""
       case properties =>
         // TODO extract as method
         val propertySchemaNames = properties.map { case Property(name, valueType, isList, _) =>
           val propertySchemaName = {
             val disambiguatedName = if (scope.nameHasAmbiguities(name)) {
               s"${label}_node_$name"
             } else name
             camelCase(disambiguatedName)
           }
           // TODO extract to method, again...
           val asListAppendixMaybe = if (isList) ".asList()" else ""
           propertiesSrcs.addOne(
             s"""val $propertySchemaName = builder.addProperty(name = "$name", valueType = $valueType, comment = "")$asListAppendixMaybe"""
           )
           propertySchemaName
         }.mkString(", ")
         s".addProperties($propertySchemaNames)"
     }
     s"""val $schemaNodeName = builder.addNodeType(name = "$label", comment = "")$maybeAddProperties"""
   }.mkString(s"$lineSeparator$lineSeparator")

   val edges = scope.edgeTypes.map { edgeTypeDetails =>
     val label = edgeTypeDetails.label
     val schemaEdgeName =
       if (scope.nameHasAmbiguities(label)) camelCase(s"${label}Edge")
       else camelCase(label)
     val edgeProperties = edgeTypeDetails.propertyByName.values.toSeq
     val maybeAddProperties = edgeProperties.sortBy(_.name) match {
       case seq if seq.isEmpty => ""
       case properties =>
         // TODO extract as method
         val propertySchemaNames = properties.map { case Property(name, valueType, isList, _) =>
           val propertySchemaName = {
             val disambiguatedName = if (scope.nameHasAmbiguities(name)) {
               s"${label}_edge_$name"
             } else name
             camelCase(disambiguatedName)
           }
           // TODO extract to method, again...
           val asListAppendixMaybe = if (isList) ".asList()" else ""
           propertiesSrcs.addOne(
             s"""val $propertySchemaName = builder.addProperty(name = "$name", valueType = $valueType, comment = "")$asListAppendixMaybe"""
           )
           propertySchemaName
         }.mkString(", ")
         s".addProperties($propertySchemaNames)"
     }
     s"""val $schemaEdgeName = builder.addEdgeType(name = "$label", comment = "")$maybeAddProperties"""
   }.mkString(s"$lineSeparator$lineSeparator")

   val relationships = scope.edgeTypes.flatMap { edgeTypeDetails =>
     val schemaEdgeName = camelCase(edgeTypeDetails.label)
     edgeTypeDetails.srcDstNodes.toSeq.sorted.map { case (src, dst) =>
       val schemaSrcName = camelCase(src)
       val schemaDstName = camelCase(dst)
       s"""$schemaSrcName.addOutEdge(edge = $schemaEdgeName, inNode = $schemaDstName, cardinalityOut = Cardinality.List, cardinalityIn = Cardinality.List, stepNameOut = "", stepNameIn = "")"""
     }
   }.mkString(s"$lineSeparator$lineSeparator")

    val properties = propertiesSrcs.result().mkString(lineSeparator)

    s"""package $schemaPackage
       |
       |import overflowdb.schema.{Schema, SchemaBuilder}
       |import overflowdb.schema.EdgeType.Cardinality
       |import overflowdb.schema.Property.ValueType
       |
       |object ${domainName}Schema {
       |  val builder = new SchemaBuilder(
       |      domainShortName = "$domainName",
       |      basePackage = "$targetPackage"
       |  )
       |
       |  /* <properties start> */
       |  $properties
       |  /* <properties end> */
       |
       |  /* <nodes start> */
       |  $nodes
       |  /* <nodes end> */
       |
       |  /* <edges start> */
       |  $edges
       |  /* <edges end> */
       |
       |  /* <relationships start> */
       |  $relationships
       |  /* <relationships end> */
       |
       |  val instance: Schema = builder.build()
       |}
       |
       |""".stripMargin
  }

  private def parseNode(node: DetachedNodeGeneric, scope: ScopeBuilder): Unit = {
    val nodeDetails = scope.nodeTypesByLabel.getOrElseUpdate(node.label, new NodeTypeDetails(node.label))
    val elementReference = ElementReference(ElementType.Node, node.label)
    node.keyvalues.sliding(2, 2).foreach {
      case Array(key: String, value) if !nodeDetails.propertyByName.contains(key) =>
        if (isList(value.getClass)) {
          iterableForList(value).headOption.map { value =>
            nodeDetails.propertyByName.update(key, Property(key, valueTypeByRuntimeClass(value.getClass), isList = true, elementReference))
          }
        } else {
          nodeDetails.propertyByName.update(key, Property(key, valueTypeByRuntimeClass(value.getClass),  isList = false, elementReference))
        }
    }
  }

  private def parseEdge(edge: CreateEdge, scope: ScopeBuilder): Unit = {
    val edgeDetails = scope.edgeTypesByLabel.getOrElseUpdate(edge.label, new EdgeTypeDetails(edge.label))
    val srcDstCombination = (edge.src.label, edge.dst.label)
    edgeDetails.srcDstNodes.addOne(srcDstCombination)
    val elementReference = ElementReference(ElementType.Edge, edge.label)

    edge.propertiesAndKeys.sliding(2, 2).foreach {
      case Array(key: String, value) if !edgeDetails.propertyByName.contains(key) =>
        if (isList(value.getClass)) {
          iterableForList(value).headOption.map { value =>
            edgeDetails.propertyByName.update(key, Property(key, valueTypeByRuntimeClass(value.getClass), isList = true, elementReference))
          }
        } else {
          edgeDetails.propertyByName.update(key, Property(key, valueTypeByRuntimeClass(value.getClass),  isList = false, elementReference))
        }
    }
  }

  /** choose one of `overflowdb.schema.Property.ValueType` based on the runtime class of a scalar (non-list) value */
  private def valueTypeByRuntimeClass(clazz: Class[_]): String = {
    if (clazz.isAssignableFrom(classOf[Boolean]) || clazz.isAssignableFrom(classOf[java.lang.Boolean]))
      "ValueType.Boolean"
    else if (clazz.isAssignableFrom(classOf[String]))
      "ValueType.String"
    else if (clazz.isAssignableFrom(classOf[Byte]) || clazz.isAssignableFrom(classOf[java.lang.Byte]))
      "ValueType.Byte"
    else if (clazz.isAssignableFrom(classOf[Short]) || clazz.isAssignableFrom(classOf[java.lang.Short]))
      "ValueType.Short"
    else if (clazz.isAssignableFrom(classOf[Int]) || clazz.isAssignableFrom(classOf[Integer]))
      "ValueType.Int"
    else if (clazz.isAssignableFrom(classOf[Long]) || clazz.isAssignableFrom(classOf[java.lang.Long]))
      "ValueType.Long"
    else if (clazz.isAssignableFrom(classOf[Float]) || clazz.isAssignableFrom(classOf[java.lang.Float]))
      "ValueType.Float"
    else if (clazz.isAssignableFrom(classOf[Double]) || clazz.isAssignableFrom(classOf[java.lang.Double]))
      "ValueType.Double"
    else if (clazz.isAssignableFrom(classOf[Char]) || clazz.isAssignableFrom(classOf[Character]))
      "ValueType.Char"
    else {
      System.err.println(s"warning: unable to derive a ValueType for runtime class $clazz - defaulting to `Unknown`")
      "ValueType.Unknown"
    }
  }

  /**
    * @return true if the given class is either array or a (subclass of) Java Iterable or Scala IterableOnce
    */
  private def isList(clazz: Class[_]): Boolean = {
    clazz.isArray ||
      classOf[java.lang.Iterable[_]].isAssignableFrom(clazz) ||
      classOf[IterableOnce[_]].isAssignableFrom(clazz)
  }

  private def iterableForList(list: AnyRef): Iterable[_] = {
    list match {
      case it: Iterable[_]           => it
      case it: IterableOnce[_]       => it.iterator.toSeq
      case it: java.lang.Iterable[_] => it.asScala
      case arr: Array[_]             => ArraySeq.unsafeWrapArray(arr)
      case other => throw new NotImplementedError(s"unhandled list of type ${other.getClass}")
    }
  }
}

object DiffGraphToSchema {
  private class NodeTypeDetails(val label: String, val propertyByName: mutable.Map[String, Property] = mutable.Map.empty)

  private class EdgeTypeDetails(val label: String,
                                val srcDstNodes: mutable.Set[(String, String)] = mutable.Set.empty,
                                val propertyByName: mutable.Map[String, Property] = mutable.Map.empty)

  private case class Property(name: String, valueType: String, isList: Boolean, on: ElementReference)

  case class ElementReference(elementType: ElementType.Value, label: String)
  object ElementType extends Enumeration {
    val Node, Edge = Value
  }

  private class ScopeBuilder {
    val nodeTypesByLabel = mutable.Map.empty[String, NodeTypeDetails]
    val edgeTypesByLabel = mutable.Map.empty[String, EdgeTypeDetails]

    def build(): Scope =
      new Scope(nodeTypesByLabel.values.toSet, edgeTypesByLabel.values.toSet)
  }

  private class Scope(val nodeTypes: Set[NodeTypeDetails],
                      val edgeTypes: Set[EdgeTypeDetails]) {
    /**
      * checks if property|node|edge name has ambiguities, e.g. instead of
      * ```
        val thing = builder.addProperty("Thing")
        val thing = builder.addNodeType("Thing")
      * ```
      * we need to disambiguate them by amending the generated schemna variable:
      * ```
        val thingProperty = builder.addProperty("Thing")
        val thingNode = builder.addNodeType("Thing")
      * ```
      */
    def nameHasAmbiguities(name: String): Boolean =
      namesWithAmbiguities.contains(name)

    private lazy val namesWithAmbiguities: Set[String] = {
      val counts = mutable.Map.empty[String, Int].withDefaultValue(0)

      val fromNodes = nodeTypes.toSeq.flatMap { node =>
        node.label +: node.propertyByName.keys.toSeq
      }
      val fromEdges = edgeTypes.toSeq.flatMap { edge =>
        edge.label +: edge.propertyByName.keys.toSeq
      }

      (fromNodes ++ fromEdges).foreach { a =>
        val newValue = counts(a) + 1
        counts.update(a, newValue)
      }

      counts.collect {
        case (name, count) if count >= 2 => name
      }.toSet
    }
  }

  /** convert various raw inputs to somewhat standardized scala names, e.g.
    * CamelCase -> camelCase
    * SNAKE_CASE -> snakeCase
    * This is by no means complete and failsafe.
    **/
  private def camelCase(raw: String): String = {
    if (raw.contains('_')) {
      (raw.split("_").map(_.toLowerCase).toList match {
        case head :: tail => head :: tail.map(_.capitalize) // capitalise all but first element
        case Nil => Nil
      }).mkString
    } else if (raw.forall(_.isUpper)) {
      raw.toLowerCase
    } else {
      decapitalize(raw)
    }
  }

  /** inversion of StringOps::capitalize - doesn't the name say it all? :) */
  private def decapitalize(s: String): String =
    if (s == null || s.length == 0 || !s.charAt(0).isUpper) s
    else s.updated(0, s.charAt(0).toLower)
}
