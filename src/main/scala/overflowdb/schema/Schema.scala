package overflowdb.schema

import overflowdb.codegen.Helpers._

import scala.collection.mutable

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

sealed trait Node {
  def className: String
}

class NodeType(val name: String, val comment: Option[String]) extends Node {
  protected var _protoId: Option[Int] = None
  protected val _properties: mutable.Set[Property] = mutable.Set.empty
  protected val _extendz: mutable.Set[NodeBaseType] = mutable.Set.empty
  protected val _outEdges: mutable.Set[AdjacentNode] = mutable.Set.empty
  protected val _inEdges: mutable.Set[AdjacentNode] = mutable.Set.empty
  protected val _containedNodes: mutable.Set[ContainedNode] = mutable.Set.empty

  lazy val className = camelCaseCaps(name)
  lazy val classNameDb = s"${className}Db"

  def protoId: Option[Int] = _protoId

  def protoId(id: Int): NodeType = {
    _protoId = Some(id)
    this
  }

  def properties: Seq[Property] =
    (_properties ++ _extendz.flatMap(_.properties)).toSeq.sortBy(_.name)

  def extendz: Seq[NodeBaseType] =
    _extendz.toSeq

  def outEdges: Seq[AdjacentNode] =
    _outEdges.toSeq

  def inEdges: Seq[AdjacentNode] =
    _inEdges.toSeq

  def containedNodes: Seq[ContainedNode] =
    _containedNodes.toSeq

  def addProperties(additional: Property*): NodeType = {
    additional.foreach(_properties.add)
    this
  }

  def addContainedNode(node: Node, localName: String, cardinality: Cardinality): NodeType = {
    _containedNodes.add(ContainedNode(node, localName, cardinality))
    this
  }

  def extendz(additional: NodeBaseType*): NodeType = {
    additional.foreach(_extendz.add)
    this
  }

  /**
   * note: allowing to define one outEdge for ONE inNode only - if you are looking for Union Types, please use NodeBaseTypes
   */
  def addOutEdge(edge: EdgeType,
                 inNode: NodeType,
                 cardinalityOut: Cardinality = Cardinality.List,
                 cardinalityIn: Cardinality = Cardinality.List): NodeType = {
    addAdjacentNode(edge, inNode, cardinalityOut, _outEdges)
    addAdjacentNode(edge, this, cardinalityIn, inNode._inEdges)
    this
  }

  def addInEdge(edge: EdgeType,
                outNode: NodeType,
                cardinalityIn: Cardinality = Cardinality.List,
                cardinalityOut: Cardinality = Cardinality.List): NodeType = {
    addAdjacentNode(edge, outNode, cardinalityIn, _inEdges)
    addAdjacentNode(edge, this, cardinalityOut, outNode._outEdges)
    this
  }

  private def addAdjacentNode(edge: EdgeType,
                              neighborNode: NodeType,
                              cardinality: Cardinality,
                              set: mutable.Set[AdjacentNode]): Unit = {
    // if edgeType is already present in set, combine the two entries
    set.find(_.viaEdge == edge) match {
      case Some(adjacentNode) =>
        set.remove(adjacentNode)
        set.add(AdjacentNode(edge, None, Cardinality.List))
      case None =>
        set.add(AdjacentNode(edge, Some(neighborNode), cardinality))
    }

  }

  override def toString = s"NodeType($name)"
}

// TODO deduplicate with NodeType - maybe just add a flag `isAbstract` and allow general inheritance?
class NodeBaseType(val name: String, val comment: Option[String]) extends Node {
  protected val _properties: mutable.Set[Property] = mutable.Set.empty
  protected val _extendz: mutable.Set[NodeBaseType] = mutable.Set.empty
  lazy val className = camelCaseCaps(name)

  def properties: Seq[Property] = _properties.toSeq.sortBy(_.name)

  def extendz: Seq[NodeBaseType] =
    _extendz.toSeq

  def extendz(additional: NodeBaseType*): NodeBaseType = {
    additional.foreach(_extendz.add)
    this
  }

  def addProperties(additional: Property*): NodeBaseType = {
    additional.foreach(_properties.add)
    this
  }

  // TODO add ability for outEdge/inEdge etc.

  override def toString = s"NodeBaseType($name)"
}


case class AdjacentNode(viaEdge: EdgeType, neighbor: Option[NodeType], cardinality: Cardinality)

case class ContainedNode(nodeType: Node, localName: String, cardinality: Cardinality)

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
               val comment: Option[String]) {
  protected val _properties: mutable.Set[Property] = mutable.Set.empty
  protected var _protoId: Option[Int] = None

  lazy val className = camelCaseCaps(name)

  def protoId: Option[Int] = _protoId

  def protoId(id: Int): EdgeType = {
    _protoId = Some(id)
    this
  }

  def properties: Seq[Property] = _properties.toSeq.sortBy(_.name)

  def addProperties(additional: Property*): EdgeType = {
    additional.foreach(_properties.add)
    this
  }

  override def toString = s"EdgeType($name)"
}

class Property(val name: String,
               val comment: Option[String],
               val valueType: String,
               val cardinality: Cardinality) {
  protected var _protoId: Option[Int] = None
  lazy val className = camelCaseCaps(name)

  def protoId: Option[Int] = _protoId

  def protoId(id: Int): Property = {
    _protoId = Some(id)
    this
  }
}

class Constant(val name: String,
               val value: String,
               val valueType: String,
               val comment: Option[String]) {
  protected var _protoId: Option[Int] = None

  def protoId: Option[Int] = _protoId

  def protoId(id: Int): Constant = {
    _protoId = Some(id)
    this
  }
}
object Constant {
  def apply(name: String, value: String, valueType: String, comment: String = ""): Constant =
    new Constant(name, value, valueType, stringToOption(comment))
}

case class NeighborNodeInfo(accessorName: String, className: String, cardinality: Cardinality)
case class NeighborInfo(accessorNameForEdge: String, nodeInfo: Option[NeighborNodeInfo], offsetPosition: Int)

object HigherValueType extends Enumeration {
  type HigherValueType = Value
  val None, Option, List, ISeq = Value
}

object Direction extends Enumeration {
  val IN, OUT = Value
  val all = List(IN, OUT)
}

object DefaultEdgeTypes {
  // TODO define this in actual schema, not here
  val ContainsNode = new EdgeType("CONTAINS_NODE", None).protoId(9)
}

case class ProductElement(name: String, accessorSrc: String, index: Int)

