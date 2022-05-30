package overflowdb.schemagenerator

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import overflowdb.BatchedUpdate._

class DiffGraphToSchemaTest extends AnyWordSpec with Matchers {
  val domainName = "sampleDomain"
  val schemaPackage = "odb.sample.schema"
  val targetPackage = "odb.sample"
  val builder = new DiffGraphToSchema(domainName = domainName, schemaPackage = schemaPackage, targetPackage = targetPackage)

  val NodeTypeA = "NODE_TYPE_A"
//  val NodePropertyA = "nodePropertyA"

  "generate schema from diffgraph" in {
    val diffGraph = new DiffGraphBuilder()
      .addNode(NodeTypeA)
//      .addNode(NodeLabel1, NodeProperty1, "prop1Value")
      .build()

    val result = builder.build(diffGraph)
    result should startWith(s"package $schemaPackage")
    result should include("""val nodeTypeA = builder.addNodeType(name = "NODE_TYPE_A")""")

  }

}
