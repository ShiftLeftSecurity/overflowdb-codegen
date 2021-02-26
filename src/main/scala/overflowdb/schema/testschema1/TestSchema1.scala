package overflowdb.schema.testschema1

import java.io.File
import overflowdb.codegen.CodeGen
import overflowdb.schema.{Cardinality, Constant, SchemaBuilder}

// TODO create integration test from this
object TestSchema1 extends App {
  val builder = new SchemaBuilder("io.shiftleft.codepropertygraph.generated")

  // node properties
  val name = builder.addNodeProperty(
    name = "NAME",
    valueType = "String",
    cardinality = Cardinality.One,
    protoId = 5,
    comment = "Name of represented object, e.g., method name (e.g. \"run\")")

  val order = builder.addNodeProperty(
    name = "ORDER",
    valueType = "Integer",
    cardinality = Cardinality.One,
    protoId = 4,
    comment = "General ordering property, such that the children of each AST-node are typically numbered from 1, ..., N (this is not enforced). The ordering has no technical meaning, but is used for pretty printing and OUGHT TO reflect order in the source code")

  val hash = builder.addNodeProperty(
    name = "HASH",
    valueType = "String",
    cardinality = Cardinality.ZeroOrOne,
    protoId = 120,
    comment = "Hash value of the artifact that this CPG is built from.")

  val inheritsFromTypeFullName = builder.addNodeProperty(
    name = "INHERITS_FROM_TYPE_FULL_NAME",
    valueType = "String",
    cardinality = Cardinality.List,
    protoId = 53,
    comment = "The static types a TYPE_DECL inherits from. This property is matched against the FULL_NAME of TYPE nodes and thus it is required to have at least one TYPE node for each TYPE_FULL_NAME")

  // edge properties
  val alias = builder.addEdgeProperty(
    name = "ALIAS",
    valueType = "Boolean",
    cardinality = Cardinality.One,
    protoId = 1,
    comment = "Defines whether a PROPAGATE edge creates an alias")

  val localName = builder.addEdgeProperty(
    name = "LOCAL_NAME",
    valueType = "String",
    cardinality = Cardinality.ZeroOrOne,
    protoId = 6,
    comment = "Local name of referenced CONTAINED node. This key is deprecated.")

  val edgekey1Lst = builder.addEdgeProperty(
    name = "EDGEKEY_1_LST",
    valueType = "Integer",
    cardinality = Cardinality.List,
    protoId = 6999,
    comment = "test list edge key")

  //  edge types
  val ast = builder.addEdgeType("AST", "Syntax tree edge")

  //  node base types
  val astNode = builder.addNodeBaseType("AST_NODE", "Any node that can exist in an abstract syntax tree")
    .addProperties(order)

  // node types
  val namespaceBlock = builder.addNodeType("NAMESPACE_BLOCK", 41, "A reference to a namespace")
    .extendz(astNode)
    .addProperties(name)
  val file = builder.addNodeType("FILE", 38, "Node representing a source file - the root of the AST")
    .extendz(astNode)
    .addProperties(name, hash, inheritsFromTypeFullName)
    .addOutEdge(edge = ast, inNode = namespaceBlock, cardinalityOut = Cardinality.List, cardinalityIn = Cardinality.ZeroOrOne)

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
