package overflowdb.schema

import overflowdb.codegen.Helpers._

import scala.collection.mutable

/**
 *  TODO future refactorings:
 *  + move lazy val to Helpers, don't import Helpers here
 *  + use valueType: Class[_] ?
 */
class SchemaBuilder(basePackage: String) {
  val nodePropertyKeys = mutable.ListBuffer.empty[Property]
  val edgePropertyKeys = mutable.ListBuffer.empty[Property]
  val nodeBaseTypes = mutable.ListBuffer.empty[NodeBaseType]
  val nodeTypes = mutable.ListBuffer.empty[NodeType]
  val edgeTypes = mutable.ListBuffer.empty[EdgeType]
  val constantsByCategory = mutable.Map.empty[String, Seq[Constant]]

  def addNodeProperty(name: String, valueType: String, cardinality: Cardinality, comment: String = ""): Property =
    addAndReturn(nodePropertyKeys, Property(name, stringToOption(comment), valueType, cardinality))

  def addEdgeProperty(name: String, valueType: String, cardinality: Cardinality, comment: String = ""): Property =
    addAndReturn(edgePropertyKeys, Property(name, stringToOption(comment), valueType, cardinality))

  def addNodeBaseType(name: String, comment: String = ""): NodeBaseType =
    addAndReturn(nodeBaseTypes, new NodeBaseType(name, stringToOption(comment)))

  def addEdgeType(name: String, comment: String = ""): EdgeType =
    addAndReturn(edgeTypes, new EdgeType(name, stringToOption(comment)))

  def addNodeType(name: String, id: Int, comment: String = ""): NodeType =
    addAndReturn(nodeTypes, new NodeType(name, stringToOption(comment), id))

  def addConstants(category: String, constants: Constant*): Seq[Constant] = {
    val previousEntries = constantsByCategory.getOrElse(category, Seq.empty)
    constantsByCategory.put(category, previousEntries ++ constants)
    constants
  }

  def build: Schema =
    new Schema(basePackage, nodePropertyKeys, edgePropertyKeys, nodeBaseTypes, nodeTypes, edgeTypes, constantsByCategory.toMap)

  private def addAndReturn[A](buffer: mutable.Buffer[A], a: A): A = {
    buffer.append(a)
    a
  }
}