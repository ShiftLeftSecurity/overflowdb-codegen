package overflowdb.schema.cpg

import overflowdb.schema._

object Splitting {
  def apply(builder: SchemaBuilder, enhancements: Enhancements.Schema) = new Schema(builder, enhancements)

  class Schema(builder: SchemaBuilder, enhancements: Enhancements.Schema) {
    import enhancements._
    // node types
    lazy val packagePrefix: NodeType = builder
      .addNodeType(
        name = "PACKAGE_PREFIX",
        comment = "This node records what package prefix is most common to all analysed classes in the CPG"
      )
      .protoId(36)
      .addProperties(value)

  }

}
