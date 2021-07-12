package overflowdb.schema

import overflowdb.NodeRef
import overflowdb.codegen.Helpers._

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

  /** nodeTypes and nodeBaseTypes combined */
  lazy val allNodeTypes: Seq[AbstractNodeType] =
    nodeTypes ++ nodeBaseTypes

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

abstract class AbstractNodeType(val name: String, val comment: Option[String], val schemaInfo: SchemaInfo)
  extends HasClassName with HasProperties with HasSchemaInfo {
  protected val _extendz: mutable.Set[NodeBaseType] = mutable.Set.empty
  protected val _outEdges: mutable.Set[AdjacentNode] = mutable.Set.empty
  protected val _inEdges: mutable.Set[AdjacentNode] = mutable.Set.empty

  /** all node types that extend this node */
  def subtypes(allNodes: Set[AbstractNodeType]): Set[AbstractNodeType]


  /** properties (including potentially inherited properties) */
  override def properties: Seq[Property] = {
    val entireClassHierarchy = this +: extendzRecursively
    entireClassHierarchy.flatMap(_.propertiesWithoutInheritance).distinct.sortBy(_.name.toLowerCase)
  }

  def propertiesWithoutInheritance: Seq[Property] =
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
  def properties: Seq[Property] =
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
class Property(val name: String,
               val valueType: Property.ValueType,
               val cardinality: Property.Cardinality,
               val comment: Option[String] = None,
               val schemaInfo: SchemaInfo) extends HasClassName with HasOptionalProtoId with HasSchemaInfo


class Property2(val name: String, val valueType: Property.ValueType2) {
  import Property.Cardinality
  protected var _cardinality: Cardinality = Cardinality.ZeroOrOne

//  type Foo = Property.ValueType2#ScalaTpe //TODO compute based on ValueType! or do that on valueType type param?
  type Foo = valueType.ScalaTpe
  // idea: use type member rather than type param?

  /** make this a mandatory property, which allows us to use primitives (better memory footprint, no GC, ...) */
  // TODO use aux type here via params?
  def mandatory(default: valueType.ScalaTpe): Property2 = {
//  def mandatory(default: Foo): Property2 = {
    println(default)
    //    _cardinality = Cardinality.One
    this
  }

  // this works...
  def mandatory2(vt: Property.ValueType2)(default: vt.ScalaTpe): Property2 = {
    ???
  }

  def foo1[T, R](t: T)(implicit f: Property.Foo1.Aux[T, R]): R = {
    println(t)
    println(f)
    f.value
  }
}

object Property {

  trait Foo1[A] {
    type B
    def value: B
  }
  object Foo1 {
    type Aux[A0, B0] = Foo1[A0] { type B = B0  }

    implicit def fi = new Foo1[Int] {
      type B = String
      val value = "Foo"
    }
  }

  abstract class ValueType2(val odbStorageType: overflowdb.storage.ValueTypes) {
    type ScalaTpe
  }
  object ValueType2 {
    import overflowdb.storage.ValueTypes._
    object Boolean extends ValueType2(BOOLEAN) {
      override type ScalaTpe = Boolean
    }
    object String extends ValueType2(STRING) {
      override type ScalaTpe = String
    }
    object Byte extends ValueType2(BYTE) {
      override type ScalaTpe = Byte
    }
    object Short extends ValueType2(SHORT) {
      override type ScalaTpe = Short
    }
    object Int extends ValueType2(INTEGER) {
      override type ScalaTpe = Int
    }
    object Long extends ValueType2(LONG) {
      override type ScalaTpe = Long
    }
    object Float extends ValueType2(FLOAT) {
      override type ScalaTpe = Float
    }
    object Double extends ValueType2(DOUBLE) {
      override type ScalaTpe = Double
    }
    object Char extends ValueType2(CHARACTER) {
      override type ScalaTpe = Char
    }
    object List extends ValueType2(LIST) {
      override type ScalaTpe = Seq[_]
    }
    object NodeRef extends ValueType2(NODE_REF) {
      override type ScalaTpe = NodeRef[_]
    }
    object Unknown extends ValueType2(UNKNOWN) {
      override type ScalaTpe = Any
    }
  }

  abstract class ValueType(val odbStorageType: overflowdb.storage.ValueTypes)
  object ValueType {
    import overflowdb.storage.ValueTypes._
    object Boolean extends ValueType(BOOLEAN)
    object String extends ValueType(STRING)
    object Byte extends ValueType(BYTE)
    object Short extends ValueType(SHORT)
    object Int extends ValueType(INTEGER)
    object Long extends ValueType(LONG)
    object Float extends ValueType(FLOAT)
    object Double extends ValueType(DOUBLE)
    object Char extends ValueType(CHARACTER)
    object List extends ValueType(LIST)
    object NodeRef extends ValueType(NODE_REF)
    object Unknown extends ValueType(UNKNOWN)
  }

  sealed abstract class Cardinality
  object Cardinality {
    case object ZeroOrOne extends Cardinality
    case object List extends Cardinality
    case object ISeq extends Cardinality
    case class One[A <: ValueType](default: Default[A]) extends Cardinality
  }

  case class Default[A : DefaultValueCheckImpl](value: A) {
    def defaultValueCheckImpl(valueName: String, defaultValue: String): String = {
      implicitly[DefaultValueCheckImpl[A]].apply(valueName, defaultValue)
    }
  }

  // move to codegen?
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
  protected val _properties: mutable.Set[Property] = mutable.Set.empty

  def addProperty(additional: Property): this.type = {
    _properties.add(additional)
    this
  }

  def addProperties(additional: Property*): this.type = {
    additional.foreach(addProperty)
    this
  }

  /** properties (including potentially inherited properties) */
  def properties: Seq[Property]
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
