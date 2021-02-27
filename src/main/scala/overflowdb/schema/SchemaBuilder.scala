package overflowdb.schema

import overflowdb.codegen.Helpers._
import overflowdb.schema.SchemaBuilder.nextProtoId

import java.util.concurrent.atomic.AtomicInteger
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

  def addNodeProperty(name: String, valueType: String, cardinality: Cardinality, comment: String = "", protoId: Int = nextProtoId): Property =
    addAndReturn(nodePropertyKeys, Property(name, stringToOption(comment), valueType, cardinality, protoId))

  def addEdgeProperty(name: String, valueType: String, cardinality: Cardinality, comment: String = "", protoId: Int = nextProtoId): Property =
    addAndReturn(edgePropertyKeys, Property(name, stringToOption(comment), valueType, cardinality, protoId))

  def addNodeBaseType(name: String, comment: String = ""): NodeBaseType =
    addAndReturn(nodeBaseTypes, new NodeBaseType(name, stringToOption(comment)))

  def addEdgeType(name: String, comment: String = "", protoId: Int = nextProtoId): EdgeType =
    addAndReturn(edgeTypes, new EdgeType(name, stringToOption(comment), protoId))

  def addNodeType(name: String, comment: String = "", protoId: Int = nextProtoId): NodeType =
    addAndReturn(nodeTypes, new NodeType(name, stringToOption(comment), protoId))

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

object SchemaBuilder {
  private val _nextProtoId = new AtomicInteger(1)
  def nextProtoId = _nextProtoId.getAndIncrement()
}