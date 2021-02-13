package overflowdb.schema.testschema2

import overflowdb.codegen.CodeGen
import overflowdb.schema.{Cardinality, Constant, SchemaBuilder}

import java.io.File

// TODO create integration test from this
object TestSchema2 extends App {
  val builder = new SchemaBuilder("io.shiftleft.codepropertygraph.generated")
  val base = Base(builder)
  base.name
//  val javaSpecific = new JavaSpecific(builder, base)
//  new CodeGen(builder.build).run(new File("target"))
}

object Base{
  def apply(builder: SchemaBuilder) = new Schema(builder)
  private class Schema(builder: SchemaBuilder) {
    val name = builder.addNodeProperty("NAME", "string", Cardinality.One, "Name of represented object, e.g., method name (e.g. \"run\")")
  }
}

//class JavaSpecific(builder: SchemaBuilder, base: BaseSchema) {
//  // TODO: add stuff, can now reference dsl elements from base statically
//}