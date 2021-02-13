package overflowdb.schema.testschema2

import overflowdb.codegen.CodeGen
import overflowdb.schema.{Cardinality, Constant, SchemaBuilder}

import java.io.File

// TODO create integration test from this
object TestSchema2 extends App {
  val builder = new SchemaBuilder("io.shiftleft.codepropertygraph.generated")
  val base = new BaseSchema(builder)
  val javaSpecific = new JavaSpecific(builder, base)
  new CodeGen(builder.build).run(new File("target"))
}

class BaseSchema(builder: SchemaBuilder) {
  val name = builder.addNodeProperty("NAME", "string", Cardinality.One, "Name of represented object, e.g., method name (e.g. \"run\")")

  val order = builder.addNodeProperty("ORDER", "int",
    Cardinality.One,
    "General ordering property, such that the children of each AST-node are typically numbered from 1, ..., N (this is not enforced). The ordering has no technical meaning, but is used for pretty printing and OUGHT TO reflect order in the source code")

  val localName = builder.addEdgeProperty("LOCAL_NAME", "string", Cardinality.ZeroOrOne, "Local name of referenced CONTAINED node. This key is deprecated.")

  val ast = builder.addEdgeType("AST", "Syntax tree edge")

  val astNode = builder.addNodeBaseType("AST_NODE", Seq(order), extendz = Nil, "Any node that can exist in an abstract syntax tree")

  val namespaceBlock = builder.addNodeType("NAMESPACE_BLOCK", 41, Seq(astNode), "A reference to a namespace")

  val file = builder.addNodeType("FILE", 38, Seq(astNode), "Node representing a source file - the root of the AST")
  //    .addProperties(name, order)
  //    .addOutEdge(ast, InNode(namespaceBlock, "0-1:n"))

  builder.addConstants(category = "DispatchTypes",
    Constant(name = "STATIC_DISPATCH", value = "STATIC_DISPATCH", valueType = "String",
      comment = "For statically dispatched calls the call target is known before program execution"),
    Constant(name = "DYNAMIC_DISPATCH", value = "DYNAMIC_DISPATCH", valueType = "String",
      comment = "For dynamically dispatched calls the target is determined during runtime ")
  )

  builder.addConstants(category = "Operators",
    Constant(name = "addition", value = "<operator>.addition", valueType = "String"),
    Constant(name = "pointerShift", value = "<operator>.pointerShift", valueType = "String",
      comment = "Shifts a pointer. In terms of CPG, the first argument is the pointer and the second argument is the index. The index selection works the same way as for indirectIndexAccess. This operator is currently only used directly by the LLVM language, but it is also used internally for C. For example, pointerShift(ptr, 7) is equivalent to &(ptr[7]). Handling of this operator is special-cased in the back-end")
  )
}

class JavaSpecific(builder: SchemaBuilder, base: BaseSchema) {
  // TODO: add stuff, can now reference dsl elements from base statically
}