package overflowdb.schemagenerator

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import overflowdb.BatchedUpdate._

class DiffGraphToSchemaTest extends AnyWordSpec with Matchers {
  val domainName = "sampleDomain"
  val schemaPackage = "odb.sample.schema"
  val targetPackage = "odb.sample"
  val builder = new DiffGraphToSchema(domainName = domainName, schemaPackage = schemaPackage, targetPackage = targetPackage)

  // handle both CamelCase and SNAKE_CASE
  val NodeA = "NodeA"
  val NodeB = "NODE_B"
//  val PropertyY = "PropertyY"
//  val PropertyX = "PROPERTY_X"

  "generate schema from diffgraph" in {
    val diffGraph = new DiffGraphBuilder()
      .addNode(NodeA)
      .addNode(NodeB)
//      .addNode(NodeA, PropertyX, "propertyXValue1")
      .build()

    val result = builder.build(diffGraph)
    result should startWith(s"package $schemaPackage")
//    result should include("""val nodeAPropertyA = builder.addProperty(name = "P")""")
    result should include("""val nodeA = builder.addNodeType(name = "NodeA")""")
    result should include("""val nodeB = builder.addNodeType(name = "NODE_B")""")
  }

}
