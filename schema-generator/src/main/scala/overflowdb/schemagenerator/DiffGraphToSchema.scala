package overflowdb.schemagenerator

import overflowdb.BatchedUpdate._
import overflowdb.DetachedNodeGeneric

import java.lang.System.lineSeparator
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

  def build(diffGraph: DiffGraph): String = {
    val context = new Context // contains some mutable collections which will be filled based on what we find

    diffGraph.iterator().forEachRemaining {
      case node: DetachedNodeGeneric => handleNode(node, context)
      case edge: CreateEdge => handleEdge(edge, context)
    }

    val properties = context.nodeTypes.values.flatMap(_.propertyNames).toSeq.distinct.map { name =>
      val schemaPropertyName = camelCase(name)
      val PropertyDetails(valueType, isList) = context.propertyValueTypeByName.getOrElse(name, throw new AssertionError(s"no ValueType determined for property with name=$name"))
      val asListAppendixMaybe = if (isList) ".asList()" else ""
      s"""val $schemaPropertyName = builder.addProperty(name = "$name", valueType = $valueType)$asListAppendixMaybe"""
    }.mkString(s"$lineSeparator$lineSeparator")

    // TODO disambiguate between everything: nodes, properties, edges
    //   val ambiguousNames: Set[String] = {
    //     val alreadySeen = mutable.Set.empty[String]
    //     TODO use Set.newBuilder?
    //     TODO walk nodes and edges ...
    //  }

    val nodes = context.nodeTypes.map { case (label,  nodeTypeDetails) =>
      val schemaNodeName = camelCase(label)
      val maybeAddProperties = nodeTypeDetails.propertyNames.toSeq.sorted match {
        case seq if seq.isEmpty => ""
        case seq =>
          val properties = seq.mkString(", ")
          s".addProperties($properties)"
      }
      s"""val $schemaNodeName = builder.addNodeType(name = "$label")$maybeAddProperties
         |""".stripMargin
    }.mkString(s"$lineSeparator$lineSeparator")

    val edges = "TODO"
    val relationships = "TODO"

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

  private def handleNode(node: DetachedNodeGeneric, context: DiffGraphToSchema.Context): Unit = {
    val nodeDetails = context.nodeTypes.getOrElseUpdate(node.label, new NodeTypeDetails)
    node.keyvalues.sliding(2, 2).collect {
      case Array(key: String, value) if !nodeDetails.propertyNames.contains(key) =>
        nodeDetails.propertyNames.addOne(key)
        if (!context.propertyValueTypeByName.contains(key)) {
          if (isList(value.getClass)) {
            iterableForList(value).headOption.map { value =>
              context.propertyValueTypeByName.update(key, PropertyDetails(valueTypeByRuntimeClass(value.getClass), isList = true))
            }
          } else {
            context.propertyValueTypeByName.update(key, PropertyDetails(valueTypeByRuntimeClass(value.getClass),  isList = false))
          }
        }
    }
//    context.nodeTypes.addOne(node.label, nodeDetails.copy(propertyNames = nodeDetails.propertyNames ++ additionalProperties))
  }

  private def handleEdge(edge: CreateEdge, context: DiffGraphToSchema.Context): Unit = {
    val edgeDetails = context.edgeTypes.getOrElseUpdate(edge.label, new EdgeTypeDetails)
    val srcDstCombination = (edge.src.label, edge.dst.label)
    edgeDetails.srcDstNodes.addOne(srcDstCombination)
    // TODO handle properties: edge.propertiesAndKeys, just like above for nodes
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
    } else {
      decapitalize(raw)
    }
  }

  /** inversion of StringOps::capitalize - doesn't the name say it all? :) */
  private def decapitalize(s: String): String =
    if (s == null || s.length == 0 || !s.charAt(0).isUpper) s
    else s.updated(0, s.charAt(0).toLower)

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
  private class Context {
    val nodeTypes = mutable.Map.empty[String, NodeTypeDetails]
    val edgeTypes = mutable.Map.empty[String, EdgeTypeDetails]
    val propertyValueTypeByName = mutable.Map.empty[String, PropertyDetails]
  }
  private class NodeTypeDetails(val propertyNames: mutable.Set[String] = mutable.Set.empty)

  private class EdgeTypeDetails(val srcDstNodes: mutable.Set[(String, String)] = mutable.Set.empty,
                                val propertyNames: mutable.Set[String] = mutable.Set.empty)

  private case class PropertyDetails(valueType: String, isList: Boolean)
}