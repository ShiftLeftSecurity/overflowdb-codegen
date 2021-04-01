package overflowdb.schema

import overflowdb.codegen.Helpers
import overflowdb.codegen.Helpers._
import overflowdb.storage.ValueTypes

import scala.collection.mutable

/**
  *  TODO future refactorings:
  *  + move lazy val to Helpers, don't import Helpers here
  */
class SchemaBuilder(basePackage: String) {
  val nodePropertyKeys = mutable.ListBuffer.empty[Property]
  val edgePropertyKeys = mutable.ListBuffer.empty[Property]
  val nodeBaseTypes = mutable.ListBuffer.empty[NodeBaseType]
  val nodeTypes = mutable.ListBuffer.empty[NodeType]
  val edgeTypes = mutable.ListBuffer.empty[EdgeType]
  val constantsByCategory = mutable.Map.empty[String, Seq[Constant]]
  var protoOptions: Option[ProtoOptions] = None

  /** root node trait for all nodes - use if you want to be explicitly unspecific
   * TODO handle differently - it's a cpg-specific special type at the moment, which isn't nice.
   * it's not even part of the regular base types, but instead defined in the RootTypes.scala
   * */
  lazy val anyNode: NodeBaseType =
    new NodeBaseType("CPG_NODE", Some("generic node base trait - use if you want to be explicitly unspecific"))

  def addNodeProperty(name: String, valueType: ValueTypes, cardinality: Cardinality, comment: String = ""): Property =
    addAndReturn(nodePropertyKeys, new Property(name, stringToOption(comment), valueType, cardinality))

  def addEdgeProperty(name: String, valueType: ValueTypes, cardinality: Cardinality, comment: String = ""): Property =
    addAndReturn(edgePropertyKeys, new Property(name, stringToOption(comment), valueType, cardinality))

  def addNodeBaseType(name: String, comment: String = ""): NodeBaseType =
    addAndReturn(nodeBaseTypes, new NodeBaseType(name, stringToOption(comment)))

  def addEdgeType(name: String, comment: String = ""): EdgeType =
    addAndReturn(edgeTypes, new EdgeType(name, stringToOption(comment)))

  def addNodeType(name: String, comment: String = ""): NodeType =
    addAndReturn(nodeTypes, new NodeType(name, stringToOption(comment)))

  def addConstants(category: String, constants: Constant*): Seq[Constant] = {
    val previousEntries = constantsByCategory.getOrElse(category, Seq.empty)
    constantsByCategory.put(category, previousEntries ++ constants)
    constants
  }

  def protoOptions(value: ProtoOptions): SchemaBuilder = {
    this.protoOptions = Option(value)
    this
  }

  def build: Schema =
    new Schema(
      basePackage,
      nodePropertyKeys.sortBy(_.name).toSeq,
      edgePropertyKeys.sortBy(_.name).toSeq,
      nodeBaseTypes.sortBy(_.name).toSeq,
      nodeTypes.sortBy(_.name).toSeq,
      edgeTypes.sortBy(_.name).toSeq,
      constantsByCategory.toMap,
      protoOptions,
    )

  private def addAndReturn[A](buffer: mutable.Buffer[A], a: A): A = {
    buffer.append(a)
    a
  }
}
