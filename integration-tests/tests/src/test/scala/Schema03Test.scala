import overflowdb.traversal._

class Schema03aTest {
  import testschema03a._
  import testschema03a.edges._
  import testschema03a.nodes._
  import testschema03a.traversal._

  // just verifying that the following code compiles
  val abstractNode1: AbstractNode1 = ???
  def edge1Out: Traversal[StoredNode] = abstractNode1.edge1Out
  def _abstractNode1ViaEdge1Out: Traversal[AbstractNode1] = abstractNode1._abstractNode1ViaEdge1Out
  def _nodeExt1ViaEdge1Out: Traversal[NodeExt1] = abstractNode1._nodeExt1ViaEdge1Out
}

class Schema03bTest {
  import testschema03b._
  import testschema03b.edges._
  import testschema03b.nodes._
  import testschema03b.traversal._

  // just verifying that the following code compiles
  val abstractNode2: AbstractNode2 = ???
  def edge2In: Traversal[StoredNode] = abstractNode2.edge2In
  def _abstractNode2ViaEdge2In: Traversal[AbstractNode2] = abstractNode2._abstractNode2ViaEdge2In
  def _nodeExt2ViaEdge2In: Traversal[NodeExt2] = abstractNode2._nodeExt2ViaEdge2In
}

class Schema03cTest {
  // TODO
//  import testschema03c._
//  import testschema03c.edges._
//  import testschema03c.nodes._
//  import testschema03c.traversal._
//
//  // just verifying that the following code compiles
//  val abstractNode3: AbstractNode3 = ???
//  def edge3In: Traversal[StoredNode] = abstractNode3.edge3In
//  def _abstractNode3ViaEdge3In: Traversal[AbstractNode3] = abstractNode3._abstractNode3ViaEdge3In
//  def _nodeExt3ViaEdge3In: Traversal[NodeExt3] = abstractNode3._nodeExt3ViaEdge3In

}