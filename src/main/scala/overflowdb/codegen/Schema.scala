package overflowdb.codegen

import java.io.FileInputStream
import play.api.libs.json._
import play.api.libs.functional.syntax._

class Schema(schemaFile: String) {
  implicit private val nodeBaseTraitRead = Json.reads[NodeBaseTrait]
  implicit private val inNodeRead = Json.reads[InNode]
  implicit private val outEdgeEntryRead = Json.reads[OutEdgeEntry]
  implicit private val containedNodeRead = Json.reads[ContainedNode]
  implicit private val nodeTypesRead = Json.reads[NodeType]
  implicit private val propertyRead = Json.reads[Property]
  implicit private val edgeTypeRead = Json.reads[EdgeType]

  lazy val jsonRoot = Json.parse(new FileInputStream(schemaFile))
  lazy val nodeBaseTraits = (jsonRoot \ "nodeBaseTraits").as[List[NodeBaseTrait]]
  lazy val nodeTypes = (jsonRoot \ "nodeTypes").as[List[NodeType]]
  lazy val edgeTypes = (jsonRoot \ "edgeTypes").as[List[EdgeType]]
  lazy val nodeKeys = (jsonRoot \ "nodeKeys").as[List[Property]]
  lazy val edgeKeys = (jsonRoot \ "edgeKeys").as[List[Property]]

  lazy val nodeTypeByName: Map[String, NodeType] =
    nodeTypes.map(node => node.name -> node).toMap

  lazy val nodeBaseTypeByName: Map[String, NodeBaseTrait] =
    nodeBaseTraits.map(node => node.name -> node).toMap

  lazy val nodePropertyByName: Map[String, Property] =
    nodeKeys.map(property => property.name -> property).toMap

  lazy val edgePropertyByName: Map[String, Property] =
    edgeKeys.map(property => property.name -> property).toMap

  /* schema only specifies `node.outEdges` - this builds a reverse map (essentially `node.inEdges`) along with the outNodes */
  lazy val nodeToInEdgeContexts: Map[String, Seq[InEdgeContext]] = {
    case class NeighborContext(neighborNode: String, edgeLabel: String, outNode: OutNode)
    val tuples: Seq[NeighborContext] =
      for {
        nodeType <- nodeTypes
        outEdge <- nodeType.outEdges.getOrElse(Nil)
        inNode <- outEdge.inNodes
      } yield NeighborContext(inNode.name, outEdge.edgeName, OutNode(nodeType.name, inNode.cardinality))

    /* grouping above tuples by `neighborNodeType` and `inEdgeName`
     * we use this from sbt, so unfortunately we can't yet use scala 2.13's `groupMap` :( */
    type NeighborNodeLabel = String
    type EdgeLabel = String
    val grouped: Map[NeighborNodeLabel, Map[EdgeLabel, Seq[OutNode]]] =
      tuples.groupBy(_.neighborNode).mapValues(_.groupBy(_.edgeLabel).mapValues(_.map(_.outNode)).toMap)

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

  lazy val defaultConstantReads: Reads[Constant] = constantReads("name", "name")

  def constantReads(nameField: String, valueField: String): Reads[Constant] = (
    (JsPath \ nameField).read[String] and
      (JsPath \ valueField).read[String] and
      (JsPath \ "comment").readNullable[String] and
      (JsPath \ "valueType").readNullable[String] and
      (JsPath \ "cardinality").readNullable[String] and
      (JsPath \ "id").readNullable[Int]
    )(Constant.apply _)

  def constantsFromElement(rootElementName: String)(implicit reads: Reads[Constant] = defaultConstantReads): List[Constant] =
    (jsonRoot \ rootElementName).toOption
      .map(_.validate[List[Constant]].get)
      .getOrElse(Nil)
}

case class NodeType(
    name: String,
    comment: Option[String],
    keys: List[String],
    outEdges: Option[List[OutEdgeEntry]],
    is: Option[List[String]],
    containedNodes: Option[List[ContainedNode]],
    id: Int) {
  lazy val className = Helpers.camelCaseCaps(name)
  lazy val classNameDb = s"${className}Db"
  lazy val containedNodesList = containedNodes.getOrElse(Nil)
}

case class OutEdgeEntry(edgeName: String, inNodes: List[InNode])

case class InNode(name: String, cardinality: Option[String])
case class OutNode(name: String, cardinality: Option[String])

case class ContainedNode(nodeType: String, localName: String, cardinality: String) {
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
      .getOrElse(throw new AssertionError(s"cardinality must be one of `zeroOrOne`, `one`, `list`, `iseq` but was $name"))
}

case class EdgeType(id: Int, name: String, keys: List[String], comment: Option[String]) {
  lazy val className = Helpers.camelCaseCaps(name)
}

case class Property(id: Int, name: String, comment: Option[String], valueType: String, cardinality: String)

case class NodeBaseTrait(name: String, hasKeys: List[String], `extends`: Option[List[String]], comment: String) {
  lazy val extendz = `extends` //it's mapped from the key in json :(
  lazy val className = Helpers.camelCaseCaps(name)
}

case class InEdgeContext(edgeName: String, neighborNodes: Set[OutNode])

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

object DefaultNodeTypes {
  /** root type for all nodes */
  val Node = "CPG_NODE"
  val NodeClassname = "CpgNode"
}

object DefaultEdgeTypes {
  val ContainsNode = "CONTAINS_NODE"
}

case class ProductElement(name: String, accessorSrc: String, index: Int)

case class Constant(name: String, value: String, comment: Option[String], valueType: Option[String] = None, cardinality: Option[String] = None,
                    id: Option[Int] = None)
object Constant {
  def fromProperty(property: Property) = Constant(property.name, property.name, property.comment)
  def fromNodeType(property: Property) = Constant(property.name, property.name, property.comment)
  def fromNodeType(tpe: NodeType) = Constant(tpe.name, tpe.name, tpe.comment)
  def fromEdgeType(tpe: EdgeType) = Constant(tpe.name, tpe.name, tpe.comment)
}
