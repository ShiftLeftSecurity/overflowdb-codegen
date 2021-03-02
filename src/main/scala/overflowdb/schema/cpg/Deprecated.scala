package overflowdb.schema.cpg

import overflowdb.schema._

object Deprecated {
  def apply(builder: SchemaBuilder, base: Base.Schema) = new Schema(builder, base)

  class Schema(builder: SchemaBuilder, base: Base.Schema) {
    import base._

    // node types
    callNode
      .addProperties(methodInstFullName, typeFullName)

  }

}
