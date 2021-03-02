package overflowdb.schema.cpg

import overflowdb.codegen.CodeGen
import overflowdb.schema.SchemaBuilder

import java.io.File

object CpgSchema extends App{
  val builder = new SchemaBuilder("io.shiftleft.codepropertygraph.generated")
  val base = Base(builder)
//  val javaSpecific = JavaSpecific(builder, base)
  new CodeGen(builder.build).run(new File("target"))
}
