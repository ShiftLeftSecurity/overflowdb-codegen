package overflowdb.schema

import overflowdb.codegen.Helpers._

import scala.collection.mutable
import scala.collection.mutable.Buffer

/**
* @param basePackage: specific for your domain, e.g. `com.example.mydomain`
 */
class Schema(val basePackage: String,
             val nodeProperties: Seq[Property],
             val edgeProperties: Seq[Property],
             val nodeBaseTypes: Seq[NodeBaseType],
             val nodeTypes: Seq[NodeType],
             val edgeTypes: Seq[EdgeType],
             val constantsByCategory: Map[String, Seq[Constant]])

class NodeType(val name: String,
               val comment: Option[String],
               val id: Int,
               val extendz: Buffer[NodeBaseType],
               properties: mutable.Set[Property] = mutable.Set.empty,
               protected val _outEdges: mutable.Set[OutEdge] = mutable.Set.empty,
               protected val _inEdges: mutable.Set[InEdge] = mutable.Set.empty,
               val containedNodes: Buffer[ContainedNode]) {
  lazy val className = camelCaseCaps(name)
  lazy val classNameDb = s"${className}Db"

  def properties: Seq[Property] =
    (properties ++ extendz.flatMap(_.properties)).toSeq

  def outEdges: Seq[OutEdge] =
    _outEdges.toSeq

  def inEdges: Seq[InEdge] =
    _inEdges.toSeq

  def addProperties(additional: Property*): NodeType = {
    additional.foreach(properties.add)
    this
  }

  /**
   * note: allowing to define one outEdge for ONE inNode only - if you are looking for Union Types, please use NodeBaseTypes
   */
  def addOutEdge(edge: EdgeType,
                 inNode: NodeType,
                 cardinalityOut: Cardinality = Cardinality.List,
                 cardinalityIn: Cardinality = Cardinality.List): NodeType = {
    _outEdges.add(OutEdge(edge, inNode, cardinalityOut))
    inNode._inEdges.add(InEdge(edge, this, cardinalityIn))
    this
  }

  def addInEdge(edge: EdgeType,
                outNode: NodeType,
                cardinalityIn: Cardinality = Cardinality.List,
                cardinalityOut: Cardinality = Cardinality.List): NodeType = {
    _inEdges.add(InEdge(edge, outNode, cardinalityIn))
    outNode._outEdges.add(OutEdge(edge, this, cardinalityOut))
    this
  }
}

case class OutEdge(edge: EdgeType, inNode: NodeType, cardinality: Cardinality)
case class InEdge(edge: EdgeType, outNode: NodeType, cardinality: Cardinality)

// TODO make this a generic edge rather than a specialised feature?
case class ContainedNode(nodeType: String, localName: String, cardinality: Cardinality) {
  lazy val nodeTypeClassName = camelCaseCaps(nodeType)
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

class EdgeType(val name: String,
               val comment: Option[String],
               val properties: Buffer[Property] = Buffer.empty) {
  lazy val className = camelCaseCaps(name)

  def addProperties(additional: Property*): EdgeType = {
    properties.appendAll(additional)
    this
  }
}

class Property(val name: String,
               val comment: Option[String],
               val valueType: String,
               val cardinality: Cardinality)

class NodeBaseType(val name: String,
                   val extendz: Buffer[NodeBaseType],
                   val comment: Option[String]) {
  val _properties: Buffer[Property] = Buffer.empty
  lazy val className = camelCaseCaps(name)

  def properties: Seq[Property] = _properties

  def addProperties(additional: Property*): NodeBaseType = {
    _properties.appendAll(additional)
    this
  }

  // TODO add ability for outEdge/inEdge etc. - maybe via trait mixin?
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
  val ContainsNode = new EdgeType("CONTAINS_NODE", None)
}

case class ProductElement(name: String, accessorSrc: String, index: Int)

case class Constant(name: String, value: String, valueType: String, comment: Option[String])
object Constant {
  def apply(name: String, value: String, valueType: String, comment: String = ""): Constant =
    Constant(name, value, valueType, stringToOption(comment))
}
