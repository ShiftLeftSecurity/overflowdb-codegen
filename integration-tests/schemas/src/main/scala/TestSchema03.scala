/** complex scenario with multiple layers of base nodes
  * similar to what we use in docker-ext schema extension
  */
class TestSchema03 extends TestSchema {
  val expression = builder.addNodeBaseType(name = "EXPRESSION")
  val block = builder.addNodeType(name = "BLOCK").extendz(expression)
  val typeDecl = builder.addNodeType(name = "TYPE_DECL").extendz(expression)
  val file = builder.addNodeType(name = "FILE")

  val instruction = builder.addNodeBaseType(name = "INSTRUCTION").extendz(expression)
  val otherInstruction = builder.addNodeType(name = "OTHER_INSTRUCTION").extendz(instruction)

  val ast = builder.addEdgeType(name = "AST")

  block.addOutEdge(edge = ast, inNode = typeDecl)
  block.addOutEdge(edge = ast, inNode = expression)
  file.addOutEdge(edge = ast, inNode = otherInstruction)
}
