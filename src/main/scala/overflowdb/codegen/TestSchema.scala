package overflowdb.codegen

import overflowdb.schema.{Cardinality, InNode, SchemaBuilder}

// TODO move to test, or drop
object TestSchema extends App {
  val schema = new SchemaBuilder("io.shiftleft.codepropertygraph.generated")

  // node properties
  val name = schema.addNodePropertyKey("NAME", "string", Cardinality.One, "Name of represented object, e.g., method name (e.g. \"run\")")
  val order = schema.addNodePropertyKey("ORDER", "int",
    Cardinality.One,
    "General ordering property, such that the children of each AST-node are typically numbered from 1, ..., N (this is not enforced). The ordering has no technical meaning, but is used for pretty printing and OUGHT TO reflect order in the source code")

  // edge properties
  val localName = schema.addEdgePropertyKey("LOCAL_NAME", "string", Cardinality.ZeroOrOne, "Local name of referenced CONTAINED node. This key is deprecated.")

  // edge types
  val ast = schema.addEdgeType("AST", "Syntax tree edge")

  // node base types
  val astNode = schema.addNodeBaseType("AST_NODE", Seq(order), extendz = Nil, "Any node that can exist in an abstract syntax tree")

  // node types
  val namespaceBlock = schema.addNodeType("NAMESPACE_BLOCK", 41, Seq(astNode), "A reference to a namespace")

  val file = schema.addNodeType("FILE", 38, Seq(astNode), "Node representing a source file - the root of the AST")
//    .addProperties(name, order)
//    .addOutEdge(ast, InNode(namespaceBlock, "0-1:n"))

  // TODO add constants

  val outputDir = new java.io.File("target")
  new CodeGen(schema.build).run(outputDir)
}
