package overflowdb.schema.cpg

import overflowdb.codegen.CodeGen
import overflowdb.schema.SchemaBuilder

import java.io.File

/**
 * Schema for the base code property graph
 * Language modules are required to produce graphs adhering to this schema
 */
object CpgSchema extends App{
  val builder = new SchemaBuilder("io.shiftleft.codepropertygraph.generated")
  val base = Base(builder)
//  val javaSpecific = JavaSpecific(builder, base)
  new CodeGen(builder.build).run(new File("target"))
}
