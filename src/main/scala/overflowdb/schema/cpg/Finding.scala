package overflowdb.schema.cpg

import overflowdb.schema._
import overflowdb.storage.ValueTypes

object Finding {
  def apply(builder: SchemaBuilder, base: Base.Schema, enhancements: Enhancements.Schema) =
    new Schema(builder, base, enhancements)

  class Schema(builder: SchemaBuilder, base: Base.Schema, enhancements: Enhancements.Schema) {
    import base._
    import enhancements._

// node properties
val key = builder.addNodeProperty(
  name = "KEY",
  valueType = ValueTypes.STRING,
  cardinality = Cardinality.One,
  comment = ""
).protoId(131)

// node types
val finding: NodeType = builder.addNodeType(
  name = "FINDING",
  comment = ""
).protoId(214)

.addProperties()


val keyValuePair: NodeType = builder.addNodeType(
  name = "KEY_VALUE_PAIR",
  comment = ""
).protoId(217)

.addProperties(key, value)


// node relations
finding

.addContainedNode(builder.anyNode, "evidence", Cardinality.List)
.addContainedNode(keyValuePair, "keyValuePairs", Cardinality.List)

// constants

  }

}
