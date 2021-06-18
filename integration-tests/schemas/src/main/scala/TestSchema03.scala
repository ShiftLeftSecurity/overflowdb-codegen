/** complex scenario with multiple layers of base nodes
  * similar to what we use in docker-ext schema extension
  */
class TestSchema03 extends TestSchema {
  val abstractNode1 = builder.addNodeBaseType(name = "ABSTRACT_NODE1")
  val nodeExt1 = builder.addNodeType(name = "NODE_EXT1").extendz(abstractNode1)
  val otherNode = builder.addNodeType(name = "OTHER_NODE")
  val edge1 = builder.addEdgeType(name = "EDGE1")

//  nodeExt1.addOutEdge(edge = edge1, inNode = abstractNode1)
//  otherNode.addOutEdge(edge = edge1, inNode = nodeExt1) //when using `inNode = abstractNode1` the generated code works

  abstractNode1.addOutEdge(edge = edge1, inNode = nodeExt1)
       nodeExt1.addOutEdge(edge = edge1, inNode = otherNode)
}
