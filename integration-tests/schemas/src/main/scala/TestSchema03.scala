/** complex scenario with multiple layers of base nodes
  * similar to what we use in docker-ext schema extension
  */
class TestSchema03a extends TestSchema {
  val abstractNode1 = builder.addNodeBaseType(name = "ABSTRACT_NODE1")
  val nodeExt1 = builder.addNodeType(name = "NODE_EXT1").extendz(abstractNode1)
  val otherNode1 = builder.addNodeType(name = "OTHER_NODE1")
  val edge1 = builder.addEdgeType(name = "EDGE1")

  abstractNode1.addOutEdge(edge = edge1, inNode = nodeExt1)
  nodeExt1.addOutEdge(edge = edge1, inNode = otherNode1)
}

class TestSchema03b extends TestSchema {
  val abstractNode2 = builder.addNodeBaseType(name = "ABSTRACT_NODE2")
  val nodeExt2 = builder.addNodeType(name = "NODE_EXT2").extendz(abstractNode2)
  val otherNode2 = builder.addNodeType(name = "OTHER_NODE2")
  val edge2 = builder.addEdgeType(name = "EDGE2")

  nodeExt2.addOutEdge(edge = edge2, inNode = abstractNode2)
  otherNode2.addOutEdge(edge = edge2, inNode = nodeExt2)
}

class TestSchema03c extends TestSchema {
  val ast = builder.addEdgeType(name = "AST")
  val astNode = builder.addNodeBaseType(name = "AST_NODE")
  val expression = builder.addNodeBaseType(name = "EXPRESSION").extendz(astNode)
  val instruction = builder.addNodeBaseType(name = "INSTRUCTION").extendz(expression)

  val typeRef = builder.addNodeType(name = "TYPE_REF")
    .extendz(expression)
    .addOutEdge(edge = ast, inNode = expression)

  val otherInstruction = builder.addNodeType(name = "OTHER_INSTRUCTION")
    .extendz(instruction)
    .addOutEdge(edge = ast, inNode = typeRef)
}
