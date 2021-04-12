package overflowdb.schema

import overflowdb.codegen.Helpers._
import overflowdb.storage.ValueTypes
import scala.collection.mutable

/**
* @param basePackage: specific for your domain, e.g. `com.example.mydomain`
 */
class Schema(val basePackage: String,
             val properties: Seq[Property],
             val nodeBaseTypes: Seq[NodeBaseType],
             val nodeTypes: Seq[NodeType],
             val edgeTypes: Seq[EdgeType],
             val constantsByCategory: Map[String, Seq[Constant]],
             val protoOptions: Option[ProtoOptions]) {

  /** properties that are used in node types */
  def nodeProperties: Seq[Property] =
    properties.filter(property =>
      (nodeTypes ++ nodeBaseTypes).exists(_.properties.contains(property))
    )

  /** properties that are used in edge types */
  def edgeProperties: Seq[Property] =
    properties.filter(property =>
      edgeTypes.exists(_.properties.contains(property))
    )
}

abstract class AbstractNodeType(val name: String, val comment: Option[String]) extends HasClassName {
  protected val _extendz: mutable.Set[NodeBaseType] = mutable.Set.empty
  protected val _outEdges: mutable.Set[AdjacentNode] = mutable.Set.empty
  protected val _inEdges: mutable.Set[AdjacentNode] = mutable.Set.empty
  protected val _properties: mutable.Set[Property] = mutable.Set.empty

  def addProperty(additional: Property): this.type = {
    _properties.add(additional)
    this
  }

  def addProperties(additional: Property*): this.type = {
    additional.foreach(addProperty)
    this
  }

  def properties: Seq[Property] = {
    /* only to provide feedback for potential schema optimisation: no need to redefine properties if they are already
     * defined in one of the parents */
    for {
      property <- _properties
      baseType <- _extendz
      if baseType.properties.contains(property)
    } println(s"[info]: $this wouldn't need to have $property added explicitly - $baseType already brings it in")

    (_properties ++ _extendz.flatMap(_.properties)).toSeq.sortBy(_.name.toLowerCase)
  }

  def extendz(additional: NodeBaseType*): this.type = {
    additional.foreach(_extendz.add)
    this
  }

  def extendz: Seq[NodeBaseType] =
    _extendz.toSeq

  /**
   * note: allowing to define one outEdge for ONE inNode only - if you are looking for Union Types, please use NodeBaseTypes
   */
  def addOutEdge(edge: EdgeType,
                 inNode: AbstractNodeType,
                 cardinalityOut: Cardinality = Cardinality.List,
                 cardinalityIn: Cardinality = Cardinality.List): this.type = {
    _outEdges.add(AdjacentNode(edge, inNode, cardinalityOut))
    inNode._inEdges.add(AdjacentNode(edge, this, cardinalityIn))
    this
  }

  def addInEdge(edge: EdgeType,
                outNode: AbstractNodeType,
                cardinalityIn: Cardinality = Cardinality.List,
                cardinalityOut: Cardinality = Cardinality.List): this.type = {
    _inEdges.add(AdjacentNode(edge, outNode, cardinalityIn))
    outNode._outEdges.add(AdjacentNode(edge, this, cardinalityOut))
    this
  }

  def outEdges: Seq[AdjacentNode] =
    (_outEdges ++ _extendz.flatMap(_.outEdges)).toSeq

  def inEdges: Seq[AdjacentNode] =
    (_inEdges ++ _extendz.flatMap(_.inEdges)).toSeq
}

class NodeType(name: String, comment: Option[String]) extends AbstractNodeType(name, comment) with HasOptionalProtoId  {
  protected val _containedNodes: mutable.Set[ContainedNode] = mutable.Set.empty

  lazy val classNameDb = s"${className}Db"

  def containedNodes: Seq[ContainedNode] =
    _containedNodes.toSeq.sortBy(_.localName.toLowerCase)

  def addContainedNode(node: AbstractNodeType, localName: String, cardinality: Cardinality): NodeType = {
    _containedNodes.add(ContainedNode(node, localName, cardinality))
    this
  }

  override def toString = s"NodeType($name)"
}

class NodeBaseType(name: String, comment: Option[String]) extends AbstractNodeType(name, comment) {
  override def toString = s"NodeBaseType($name)"
}

case class AdjacentNode(viaEdge: EdgeType, neighbor: AbstractNodeType, cardinality: Cardinality)

case class ContainedNode(nodeType: AbstractNodeType, localName: String, cardinality: Cardinality)

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

class EdgeType(val name: String, val comment: Option[String]) extends HasClassName with HasOptionalProtoId {
  protected val _properties: mutable.Set[Property] = mutable.Set.empty

  def properties: Seq[Property] = _properties.toSeq.sortBy(_.name)

  def addProperties(additional: Property*): EdgeType = {
    additional.foreach(_properties.add)
    this
  }

  override def toString = s"EdgeType($name)"
}

class Property(val name: String,
               val comment: Option[String],
               val valueType: ValueTypes,
               val cardinality: Cardinality) extends HasClassName with HasOptionalProtoId {
  override def toString = s"Property($name)"
}

class Constant(val name: String,
               val value: String,
               val valueType: ValueTypes,
               val comment: Option[String]) extends HasOptionalProtoId {
  override def toString = s"Constant($name)"
}

object Constant {
  def apply(name: String, value: String, valueType: ValueTypes, comment: String = ""): Constant =
    new Constant(name, value, valueType, stringToOption(comment))
}

case class NeighborNodeInfo(accessorName: String, className: String, cardinality: Cardinality)
case class NeighborInfo(edge: EdgeType, nodeInfos: Seq[NeighborNodeInfo], offsetPosition: Int)

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

case class ProtoOptions(pkg: String,
                        javaOuterClassname: String,
                        javaPackage: String,
                        goPackage: String,
                        csharpNamespace: String,
                        uncommonProtoEnumNameMappings: Map[String, String] = Map.empty)

trait HasClassName {
  def name: String
  lazy val className = camelCaseCaps(name)
}

trait HasOptionalProtoId {
  protected var _protoId: Option[Int] = None

  def protoId(id: Int): this.type = {
    _protoId = Some(id)
    this
  }

  def protoId: Option[Int] = _protoId
}
