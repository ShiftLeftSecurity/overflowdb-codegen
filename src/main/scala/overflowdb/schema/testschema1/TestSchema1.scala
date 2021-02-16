package overflowdb.schema.testschema1

import java.io.File
import overflowdb.codegen.CodeGen
import overflowdb.schema.{Cardinality, Constant, SchemaBuilder}

// TODO create integration test from this
object TestSchema1 extends App {
  val builder = new SchemaBuilder("io.shiftleft.codepropertygraph.generated")

  // node properties
  val name = builder.addNodeProperty("NAME", "String", Cardinality.One,
    "Name of represented object, e.g., method name (e.g. \"run\")")
  val order = builder.addNodeProperty("ORDER", "Integer", Cardinality.One,
    "General ordering property, such that the children of each AST-node are typically numbered from 1, ..., N (this is not enforced). The ordering has no technical meaning, but is used for pretty printing and OUGHT TO reflect order in the source code")
  val hash = builder.addNodeProperty(name = "HASH", valueType = "String", Cardinality.ZeroOrOne,
    "Hash value of the artifact that this CPG is built from.")
  val inheritsFromTypeFullName = builder.addNodeProperty(name = "INHERITS_FROM_TYPE_FULL_NAME", valueType = "String", Cardinality.List,
      comment = "The static types a TYPE_DECL inherits from. This property is matched against the FULL_NAME of TYPE nodes and thus it is required to have at least one TYPE node for each TYPE_FULL_NAME")

  // edge properties
  val alias = builder.addEdgeProperty("ALIAS", "Boolean", Cardinality.One, "Defines whether a PROPAGATE edge creates an alias")
  val localName = builder.addEdgeProperty("LOCAL_NAME", "String", Cardinality.ZeroOrOne, "Local name of referenced CONTAINED node. This key is deprecated.")
  val edgeKey1Lst = builder.addEdgeProperty("EDGEKEY_1_LST", "Integer", Cardinality.List, "test list edge key")

  //  edge types
  val ast = builder.addEdgeType("AST", "Syntax tree edge")

  //  node base types
  val astNode = builder.addNodeBaseType("AST_NODE", extendz = Nil, "Any node that can exist in an abstract syntax tree")
    .addProperties(order)

  // node types
  val namespaceBlock = builder.addNodeType("NAMESPACE_BLOCK", 41, Seq(astNode), "A reference to a namespace")
    .addProperties(name)

  // nodes
  val file = builder.addNodeType("FILE", 38, Seq(astNode), "Node representing a source file - the root of the AST")
    .addProperties(name, hash, inheritsFromTypeFullName)
  //    .addOutEdge(ast, InNode(namespaceBlock, "0-1:n"))

  // constants
  val dispatchTypes = builder.addConstants(category = "DispatchTypes",
    Constant(name = "STATIC_DISPATCH", value = "STATIC_DISPATCH", valueType = "String",
      comment = "For statically dispatched calls the call target is known before program execution"),
    Constant(name = "DYNAMIC_DISPATCH", value = "DYNAMIC_DISPATCH", valueType = "String",
      comment = "For dynamically dispatched calls the target is determined during runtime ")
  )

  val operators = builder.addConstants(category = "Operators",
    Constant(name = "addition", value = "<operator>.addition", valueType = "String"),
    Constant(name = "pointerShift", value = "<operator>.pointerShift", valueType = "String",
      comment = "Shifts a pointer. In terms of CPG, the first argument is the pointer and the second argument is the index. The index selection works the same way as for indirectIndexAccess. This operator is currently only used directly by the LLVM language, but it is also used internally for C. For example, pointerShift(ptr, 7) is equivalent to &(ptr[7]). Handling of this operator is special-cased in the back-end")
  )

  new CodeGen(builder.build).run(new File("target"))
}
