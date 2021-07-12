package overflowdb.schema

import overflowdb.codegen.Helpers._
import scala.collection.mutable

/**
 * @param basePackage: specific for your domain, e.g. `com.example.mydomain`
 */
class Schema(val basePackage: String,
             val properties: Seq[Property[_]],
             val nodeBaseTypes: Seq[NodeBaseType],
             val nodeTypes: Seq[NodeType],
             val edgeTypes: Seq[EdgeType],
             val constantsByCategory: Map[String, Seq[Constant]],
             val protoOptions: Option[ProtoOptions]) {

  /** nodeTypes and nodeBaseTypes combined */
  lazy val allNodeTypes: Seq[AbstractNodeType] =
    nodeTypes ++ nodeBaseTypes

  /** properties that are used in node types */
  def nodeProperties: Seq[Property[_]] =
    properties.filter(property =>
      (nodeTypes ++ nodeBaseTypes).exists(_.properties.contains(property))
    )

  /** properties that are used in edge types */
  def edgeProperties: Seq[Property[_]] =
    properties.filter(property =>
      edgeTypes.exists(_.properties.contains(property))
    )
}

abstract class AbstractNodeType(val name: String, val comment: Option[String], val schemaInfo: SchemaInfo)
  extends HasClassName with HasProperties with HasSchemaInfo {
  protected val _extendz: mutable.Set[NodeBaseType] = mutable.Set.empty
  protected val _outEdges: mutable.Set[AdjacentNode] = mutable.Set.empty
  protected val _inEdges: mutable.Set[AdjacentNode] = mutable.Set.empty

  /** all node types that extend this node */
  def subtypes(allNodes: Set[AbstractNodeType]): Set[AbstractNodeType]


  /** properties (including potentially inherited properties) */
  override def properties: Seq[Property[_]] = {
    val entireClassHierarchy = this +: extendzRecursively
    entireClassHierarchy.flatMap(_.propertiesWithoutInheritance).distinct.sortBy(_.name.toLowerCase)
  }

  def propertiesWithoutInheritance: Seq[Property[_]] =
    _properties.toSeq.sortBy(_.name.toLowerCase)

  def extendz(additional: NodeBaseType*): this.type = {
    additional.foreach(_extendz.add)
    this
  }

  def extendz: Seq[NodeBaseType] =
    _extendz.toSeq

  def extendzRecursively: Seq[NodeBaseType] = {
    val extendsLevel1 = extendz
    (extendsLevel1 ++ extendsLevel1.flatMap(_.extendzRecursively)).distinct
  }

  /**
   * note: allowing to define one outEdge for ONE inNode only - if you are looking for Union Types, please use NodeBaseTypes
   */
  def addOutEdge(edge: EdgeType,
                 inNode: AbstractNodeType,
                 cardinalityOut: EdgeType.Cardinality = EdgeType.Cardinality.List,
                 cardinalityIn: EdgeType.Cardinality = EdgeType.Cardinality.List): this.type = {
    _outEdges.add(AdjacentNode(edge, inNode, cardinalityOut))
    inNode._inEdges.add(AdjacentNode(edge, this, cardinalityIn))
    this
  }

  def addInEdge(edge: EdgeType,
                outNode: AbstractNodeType,
                cardinalityIn: EdgeType.Cardinality = EdgeType.Cardinality.List,
                cardinalityOut: EdgeType.Cardinality = EdgeType.Cardinality.List): this.type = {
    _inEdges.add(AdjacentNode(edge, outNode, cardinalityIn))
    outNode._outEdges.add(AdjacentNode(edge, this, cardinalityOut))
    this
  }

  def outEdges: Seq[AdjacentNode] =
    _outEdges.toSeq

  def inEdges: Seq[AdjacentNode] =
    _inEdges.toSeq

  def edges(direction: Direction.Value): Seq[AdjacentNode] =
    direction match {
      case Direction.IN => inEdges
      case Direction.OUT => outEdges
    }
}

class NodeType(name: String, comment: Option[String], schemaInfo: SchemaInfo)
  extends AbstractNodeType(name, comment, schemaInfo) with HasOptionalProtoId  {
  protected val _containedNodes: mutable.Set[ContainedNode] = mutable.Set.empty

  lazy val classNameDb = s"${className}Db"

  /** all node types that extend this node */
  override def subtypes(allNodes: Set[AbstractNodeType]) = Set.empty

  def containedNodes: Seq[ContainedNode] =
    _containedNodes.toSeq.sortBy(_.localName.toLowerCase)

  def addContainedNode(node: AbstractNodeType, localName: String, cardinality: Property.Cardinality): NodeType = {
    _containedNodes.add(ContainedNode(node, localName, cardinality))
    this
  }

  override def toString = s"NodeType($name)"
}

class NodeBaseType(name: String, comment: Option[String], schemaInfo: SchemaInfo)
  extends AbstractNodeType(name, comment, schemaInfo) {

  /** all node types that extend this node */
  override def subtypes(allNodes: Set[AbstractNodeType]) =
    allNodes.filter { candidate =>
      candidate.extendzRecursively.contains(this)
    }

  override def toString = s"NodeBaseType($name)"
}

case class AdjacentNode(viaEdge: EdgeType, neighbor: AbstractNodeType, cardinality: EdgeType.Cardinality)

case class ContainedNode(nodeType: AbstractNodeType, localName: String, cardinality: Property.Cardinality)

class EdgeType(val name: String, val comment: Option[String], val schemaInfo: SchemaInfo)
  extends HasClassName with HasProperties with HasOptionalProtoId with HasSchemaInfo {
  override def toString = s"EdgeType($name)"

  /** properties (including potentially inherited properties) */
  def properties: Seq[Property[_]] =
    _properties.toSeq.sortBy(_.name.toLowerCase)
}

object EdgeType {
  sealed abstract class Cardinality
  object Cardinality {
    case object One extends Cardinality
    case object ZeroOrOne extends Cardinality
    case object List extends Cardinality
  }
}

// TODO rename, move to separate file
import Property._
class Property[A : ToOdbStorageType](val name: String,
                                     val cardinality: Cardinality = Cardinality.ZeroOrOne,
                                     val comment: Option[String] = None,
                                     val schemaInfo: SchemaInfo)
  extends HasClassName with HasOptionalProtoId with HasSchemaInfo {
  lazy val odbStorageType: overflowdb.storage.ValueTypes =
    implicitly[ToOdbStorageType[A]].apply()
}

object Property {
  sealed trait ValueType {
    def odbStorageType: overflowdb.storage.ValueTypes
  }
  object ValueType {
    object Boolean extends ValueType {
      override def odbStorageType = overflowdb.storage.ValueTypes
    }
  }


  trait ToOdbStorageType[A] {
    def apply(): overflowdb.storage.ValueTypes
  }
  object ToOdbStorageType {
    implicit lazy val boolean: ToOdbStorageType[Boolean] = () => overflowdb.storage.ValueTypes.BOOLEAN
    implicit lazy val string: ToOdbStorageType[String] = () => overflowdb.storage.ValueTypes.STRING
    implicit lazy val byte: ToOdbStorageType[Byte] = () => overflowdb.storage.ValueTypes.BYTE
    implicit lazy val short: ToOdbStorageType[Short] = () => overflowdb.storage.ValueTypes.SHORT
    implicit lazy val int: ToOdbStorageType[Int] = () => overflowdb.storage.ValueTypes.INTEGER
    implicit lazy val long: ToOdbStorageType[Long] = () => overflowdb.storage.ValueTypes.LONG
    implicit lazy val float: ToOdbStorageType[Float] = () => overflowdb.storage.ValueTypes.FLOAT
    implicit lazy val double: ToOdbStorageType[Double] = () => overflowdb.storage.ValueTypes.DOUBLE
    implicit lazy val char: ToOdbStorageType[Char] = () => overflowdb.storage.ValueTypes.CHARACTER
  }

  sealed abstract class Cardinality
  object Cardinality {
    case object ZeroOrOne extends Cardinality
    case object List extends Cardinality
    case object ISeq extends Cardinality
    case class One[A](default: Default[A]) extends Cardinality
  }

  case class Default[A: DefaultValueCheckImpl](value: A) {
    def defaultValueCheckImpl(valueName: String, defaultValue: String): String = {
      implicitly[DefaultValueCheckImpl[A]].apply(valueName, defaultValue)
    }
  }

  trait DefaultValueCheckImpl[A] {
    def apply(valueName: String, defaultValue: String): String
  }
  object DefaultValueCheckImpl {
    implicit lazy val boolean: DefaultValueCheckImpl[Boolean] = (valueName, defaultValue) => s"$defaultValue == $valueName"
    implicit lazy val string: DefaultValueCheckImpl[String] = (valueName, defaultValue) => s"$defaultValue.equals($valueName)"
    implicit lazy val byte: DefaultValueCheckImpl[Byte] = (valueName, defaultValue) => s"$defaultValue == $valueName"
    implicit lazy val short: DefaultValueCheckImpl[Short] = (valueName, defaultValue) => s"$defaultValue == $valueName"
    implicit lazy val int: DefaultValueCheckImpl[Int] = (valueName, defaultValue) => s"$defaultValue == $valueName"
    implicit lazy val long: DefaultValueCheckImpl[Long] = (valueName, defaultValue) => s"$defaultValue == $valueName"
    implicit lazy val float: DefaultValueCheckImpl[Float] = (valueName, defaultValue) => s"$defaultValue == $valueName"
    implicit lazy val double: DefaultValueCheckImpl[Double] = (valueName, defaultValue) => s"$defaultValue == $valueName"
    implicit lazy val char: DefaultValueCheckImpl[Char] = (valueName, defaultValue) => s"$defaultValue == $valueName"

  }
}

class Constant(val name: String,
               val value: String,
               val valueType: overflowdb.storage.ValueTypes,
               val comment: Option[String],
               val schemaInfo: SchemaInfo) extends HasOptionalProtoId with HasSchemaInfo {
  override def toString = s"Constant($name)"
}

object Constant {
  def apply(name: String, value: String, valueType: overflowdb.storage.ValueTypes, comment: String = "")(
    implicit schemaInfo: SchemaInfo = SchemaInfo.Unknown): Constant =
    new Constant(name, value, valueType, stringToOption(comment), schemaInfo)
}

case class NeighborInfoForEdge(edge: EdgeType, nodeInfos: Seq[NeighborInfoForNode], offsetPosition: Int) {
  lazy val deriveNeighborNodeType: String =
    deriveCommonRootType(nodeInfos.map(_.neighborNode).toSet)
}

case class NeighborInfoForNode(
  neighborNode: AbstractNodeType,
  edge: EdgeType,
  direction: Direction.Value,
  cardinality: EdgeType.Cardinality,
  isInherited: Boolean) {

  lazy val accessorName = s"_${camelCase(neighborNode.name)}Via${edge.className}${camelCaseCaps(direction.toString)}"

  /** handling some accidental complexity within the schema: if a relationship is defined on a base node and
   * separately on a concrete node, with different cardinalities, we need to use the highest cardinality  */
  lazy val consolidatedCardinality: EdgeType.Cardinality = {
    val inheritedCardinalities = neighborNode.extendzRecursively.flatMap(_.inEdges).collect {
      case AdjacentNode(viaEdge, neighbor, cardinality)
        if viaEdge == edge && neighbor == neighborNode => cardinality
    }
    val allCardinalities = cardinality +: inheritedCardinalities
    allCardinalities.distinct.sortBy {
      case EdgeType.Cardinality.List => 0
      case EdgeType.Cardinality.ZeroOrOne => 1
      case EdgeType.Cardinality.One => 2
    }.head
  }

  lazy val returnType: String =
    fullScalaType(neighborNode, consolidatedCardinality)

}

object Direction extends Enumeration {
  val IN, OUT = Value
  val all = List(IN, OUT)
}

object DefaultEdgeTypes {
  // TODO define this in actual schema, not here
  val ContainsNode = new EdgeType("CONTAINS_NODE", None, SchemaInfo.forClass(getClass)).protoId(9)
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

trait HasProperties {
  protected val _properties: mutable.Set[Property[_]] = mutable.Set.empty

  def addProperty(additional: Property[_]): this.type = {
    _properties.add(additional)
    this
  }

  def addProperties(additional: Property[_]*): this.type = {
    additional.foreach(addProperty)
    this
  }

  /** properties (including potentially inherited properties) */
  def properties: Seq[Property[_]]
}

trait HasOptionalProtoId {
  protected var _protoId: Option[Int] = None

  def protoId(id: Int): this.type = {
    _protoId = Some(id)
    this
  }

  def protoId: Option[Int] = _protoId
}

trait HasSchemaInfo {
  def schemaInfo: SchemaInfo
}

/** carry extra information on where a schema element is being defined, e.g. when we want to be able to
 * refer back that `node XYZ` was defined in `BaseSchema`, e.g. for documentation */
case class SchemaInfo(definedIn: Option[Class[_]])
object SchemaInfo {
  val Unknown = SchemaInfo(None)

  def forClass(schemaClass: Class[_]): SchemaInfo =
    SchemaInfo(Option(schemaClass))
}
