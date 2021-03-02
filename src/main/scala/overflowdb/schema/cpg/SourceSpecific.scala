package overflowdb.schema.cpg

import overflowdb.schema._

object SourceSpecific {
  def apply(builder: SchemaBuilder, base: Base.Schema) = new Schema(builder, base)

  class Schema(builder: SchemaBuilder, base: Base.Schema) {
    import base._

    // node types
    lazy val comment: NodeType = builder.addNodeType(
      name = "COMMENT",
      comment = "A comment"
    ).protoId(511)

      .addProperties(lineNumber, code, filename)

      .addOutEdge(edge = sourceFile, inNode = comment)


    file
      .addOutEdge(edge = ast, inNode = comment)



  }

}
