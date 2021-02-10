package overflowdb.schema

import overflowdb.codegen.Helpers

import scala.collection.mutable

/**
 *  TODO future refactorings:
 *  + move lazy val to Helpers, don't import Helpers here
 *  + use valueType: Class[_] ?
 */
class SchemaBuilder(basePackage: String) {
  val nodePropertyKeys = mutable.ListBuffer.empty[Property]
  val edgePropertyKeys = mutable.ListBuffer.empty[Property]
  val nodeBaseTypes = mutable.ListBuffer.empty[NodeBaseTrait]
  val nodeTypes = mutable.ListBuffer.empty[NodeType]
  val edgeTypes = mutable.ListBuffer.empty[EdgeType]
  val constants  = mutable.ListBuffer.empty[Constant]

  def addNodePropertyKey(name: String, valueType: String, cardinality: Cardinality, comment: String = ""): Property =
    addAndReturn(nodePropertyKeys, Property(name, stringToOption(comment), valueType, cardinality))

  def addEdgePropertyKey(name: String, valueType: String, cardinality: Cardinality, comment: String = ""): Property =
    addAndReturn(edgePropertyKeys, Property(name, stringToOption(comment), valueType, cardinality))

  def addNodeBaseType(name: String, properties: Seq[Property], extendz: Seq[NodeBaseTrait] = Nil, comment: String = ""): NodeBaseTrait =
    addAndReturn(nodeBaseTypes, NodeBaseTrait(name, properties, extendz, stringToOption(comment)))

  def addEdgeType(name: String, comment: String = ""): EdgeType =
    addAndReturn(edgeTypes, EdgeType(name, stringToOption(comment)))

  def addNodeType(name: String, id: Int, extendz: Seq[NodeBaseTrait] = Nil, comment: String = ""): NodeType =
    addAndReturn(nodeTypes, NodeType(name, stringToOption(comment), id, extendz, containedNodes = Nil))

  def addConstant(name: String, value: String, valueType: String, cardinality: Cardinality, comment: String = ""): Constant =
    addAndReturn(constants, Constant(name, value, stringToOption(comment), valueType, cardinality))

  def build: Schema = {
    new Schema(basePackage, nodePropertyKeys, edgePropertyKeys, nodeBaseTypes, nodeTypes, edgeTypes, constants)
  }

  private def addAndReturn[A](buffer: mutable.Buffer[A], a: A): A = {
    buffer.append(a)
    a
  }

  private def stringToOption(s: String): Option[String] = s.trim match {
    case "" => None
    case nonEmptyString => Some(nonEmptyString)
  }
}

/**
* @param basePackage: specific for your domain, e.g. `com.example.mydomain`
 */
class Schema(val basePackage: String,
             val nodePropertyKeys: Seq[Property],
             val edgePropertyKeys: Seq[Property],
             val nodeBaseTraits: Seq[NodeBaseTrait],
             val nodeTypes: Seq[NodeType],
             val edgeTypes: Seq[EdgeType],
             val constants: Seq[Constant]) {

  /* schema only specifies `node.outEdges` - this builds a reverse map (essentially `node.inEdges`) along with the outNodes */
  lazy val nodeToInEdgeContexts: Map[NodeType, Seq[InEdgeContext]] = {
    case class NeighborContext(neighborNode: NodeType, edge: EdgeType, outNode: OutNode)
    val tuples: Seq[NeighborContext] =
      for {
        nodeType <- nodeTypes
        outEdge <- nodeType.outEdges
        inNode <- outEdge.inNodes
      } yield NeighborContext(inNode.node, outEdge.edge, OutNode(nodeType.name, inNode.cardinality))

    /* grouping above tuples by `neighborNodeType` and `inEdgeName`
     * we use this from sbt, so unfortunately we can't yet use scala 2.13's `groupMap` :( */
    val grouped: Map[NodeType, Map[EdgeType, Seq[OutNode]]] =
      tuples.groupBy(_.neighborNode).mapValues(_.groupBy(_.edge).mapValues(_.map(_.outNode)).toMap)

    grouped.mapValues { inEdgesWithNeighborNodes =>
      // all nodes can have incoming `CONTAINS_NODE` edges
      val adjustedInEdgesWithNeighborNodes =
        if (inEdgesWithNeighborNodes.contains(DefaultEdgeTypes.ContainsNode)) inEdgesWithNeighborNodes
        else inEdgesWithNeighborNodes + (DefaultEdgeTypes.ContainsNode -> Set.empty)

      adjustedInEdgesWithNeighborNodes.map { case (edge, neighborNodes) =>
        InEdgeContext(edge, neighborNodes.toSet)
      }.toSeq
    }
  }

//  lazy val defaultConstantReads: Reads[Constant] = constantReads("name", "name")

//  def constantReads(nameField: String, valueField: String): Reads[Constant] = (
//    (JsPath \ nameField).read[String] and
//      (JsPath \ valueField).read[String] and
//      (JsPath \ "comment").readNullable[String] and
//      (JsPath \ "valueType").readNullable[String] and
//      (JsPath \ "cardinality").readNullable[String]
//    )(Constant.apply _)
//
//  def constantsFromElement(rootElementName: String)(implicit reads: Reads[Constant] = defaultConstantReads): List[Constant] =
//    (jsonRoot \ rootElementName).get.validate[List[Constant]].get
}

case class NodeType(name: String,
                    comment: Option[String],
                    id: Int,
                    extendz: Seq[NodeBaseTrait],
                    properties: Seq[Property] = Nil,
                    outEdges: Seq[OutEdgeEntry] = Nil,
                    containedNodes: Seq[ContainedNode]) {
  lazy val className = Helpers.camelCaseCaps(name)
  lazy val classNameDb = s"${className}Db"

  def addProperties(additional: Property*): NodeType =
    copy(properties = properties ++ additional)

  def addOutEdge(outEdge: EdgeType, inNodes: InNode*) =
    ???
}

case class OutEdgeEntry(edge: EdgeType, inNodes: Seq[InNode]) {
  lazy val className = Helpers.camelCaseCaps(edge.name)
}

case class InNode(node: NodeType, cardinality: String = "n:n") // TODO express in proper types
case class OutNode(name: String, cardinality: String = "n:n") { // TODO express in proper types
  lazy val className = Helpers.camelCaseCaps(name)
}

case class ContainedNode(nodeType: String, localName: String, cardinality: Cardinality) {
  lazy val nodeTypeClassName = Helpers.camelCaseCaps(nodeType)
}

sealed abstract class Cardinality(val name: String)
object Cardinality {
  case object ZeroOrOne extends Cardinality("zeroOrOne")
  case object One extends Cardinality("one")
  case object List extends Cardinality("list")
  case object ISeq extends Cardinality("array")

  def fromName(name: String): Cardinality =
    Seq(ZeroOrOne, One, List, ISeq)
      .find(_.name == name)
      .getOrElse(throw new AssertionError(s"cardinality must be one of `zeroOrOne`, `one`, `list`, `iseq`, but was $name"))
}

case class EdgeType(name: String, comment: Option[String], properties: Seq[Property] = Nil) {
  lazy val className = Helpers.camelCaseCaps(name)

  def addProperties(additionalProperties: Property*): EdgeType =
    copy(properties = properties ++ additionalProperties)
}

case class Property(name: String, comment: Option[String], valueType: String, cardinality: Cardinality)

case class NodeBaseTrait(name: String, properties: Seq[Property], extendz: Seq[NodeBaseTrait], comment: Option[String]) {
  lazy val className = Helpers.camelCaseCaps(name)
}

case class InEdgeContext(edge: EdgeType, neighborNodes: Set[OutNode])

case class NeighborNodeInfo(accessorName: String, className: String, cardinality: Cardinality)
case class NeighborInfo(accessorNameForEdge: String, nodeInfos: Set[NeighborNodeInfo], offsetPosition: Int)

object HigherValueType extends Enumeration {
  type HigherValueType = Value
  val None, Option, List, ISeq = Value
}

object Direction extends Enumeration {
  val IN, OUT = Value
  val all = List(IN, OUT)
}

object DefaultEdgeTypes {
  val ContainsNode = EdgeType("CONTAINS_NODE", None)
}

case class ProductElement(name: String, accessorSrc: String, index: Int)

case class Constant(name: String, value: String, comment: Option[String], valueType: String, cardinality: Cardinality)
object Constant {
  def fromProperty(property: Property) = Constant(property.name, property.name, property.comment, property.valueType, property.cardinality)
  def fromNodeType(tpe: NodeType) = Constant(tpe.name, tpe.name, tpe.comment, tpe.className, Cardinality.One) //TODO really cardinality one?
  def fromEdgeType(tpe: EdgeType) = Constant(tpe.name, tpe.name, tpe.comment, tpe.className, Cardinality.One) //TODO really cardinality one?
}
