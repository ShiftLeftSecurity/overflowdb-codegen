package overflowdb.schema.cpg

import overflowdb.schema._

object Finding {
  def apply(builder: SchemaBuilder, base: Base.Schema, enhancements: Enhancements.Schema) =
    new Schema(builder, base, enhancements)

  class Schema(builder: SchemaBuilder, base: Base.Schema, enhancements: Enhancements.Schema) {
    import base._
    import enhancements._

    // node properties
    val key = builder.addNodeProperty(
      name = "KEY",
      valueType = "String",
      cardinality = Cardinality.One,
      comment = ""
    ).protoId(131)

    // node types
    lazy val finding: NodeType = builder.addNodeType(
      name = "FINDING",
      comment = ""
    ).protoId(214)

      .addProperties()


      .addContainedNode(cpgNode, "evidence", Cardinality.List)
      .addContainedNode(keyValuePair, "keyValuePairs", Cardinality.List)

    lazy val keyValuePair: NodeType = builder.addNodeType(
      name = "KEY_VALUE_PAIR",
      comment = ""
    ).protoId(217)

      .addProperties(key, value)




  }

}
