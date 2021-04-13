package overflowdb.schema

import overflowdb.codegen.Helpers._
import overflowdb.storage.ValueTypes

import scala.collection.mutable

class SchemaBuilder(basePackage: String) {
  val propertyKeys = mutable.ListBuffer.empty[Property]
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

  def addProperty(name: String, valueType: ValueTypes, cardinality: Cardinality, comment: String = ""): Property =
    addAndReturn(propertyKeys, new Property(name, stringToOption(comment), valueType, cardinality))

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

  def build: Schema = {
    verifyProtoIdsUnique
    new Schema(
      basePackage,
      propertyKeys.sortBy(_.name.toLowerCase).toSeq,
      nodeBaseTypes.sortBy(_.name.toLowerCase).toSeq,
      nodeTypes.sortBy(_.name.toLowerCase).toSeq,
      edgeTypes.sortBy(_.name.toLowerCase).toSeq,
      constantsByCategory.toMap,
      protoOptions,
    )
  }

  /** proto ids must be unique (if used) */
  def verifyProtoIdsUnique: Unit = {
    val elementsWithProtoId: Seq[HasOptionalProtoId] =
      (propertyKeys ++ nodeTypes ++ edgeTypes ++ constantsByCategory.values.flatten)
        .toSeq
        .filter(_.protoId.isDefined)

    val duplicates = elementsWithProtoId.groupBy(_.protoId.get).filter(_._2.size > 1)
    if (duplicates.nonEmpty) {
      throw new AssertionError(
        s"proto ids must be unique across all schema elements, however we found " +
          s"the following duplicates: protoId -> Seq[SchemaElement]: \n" +
          duplicates.mkString("\n")
      )
    }

  }

  private def addAndReturn[A](buffer: mutable.Buffer[A], a: A): A = {
    buffer.append(a)
    a
  }
}
