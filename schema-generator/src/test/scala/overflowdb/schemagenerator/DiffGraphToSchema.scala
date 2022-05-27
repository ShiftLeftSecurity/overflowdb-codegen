package overflowdb.schemagenerator

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import overflowdb.BatchedUpdate._

class DiffGraphToSchemaTest extends AnyWordSpec with Matchers {
  val domainName = "sampleDomain"
  val schemaPackage = "odb.sample.schema"
  val targetPackage = "odb.sample"
  val builder = new DiffGraphToSchema(domainName = domainName, schemaPackage = schemaPackage, targetPackage = targetPackage)

  val NodeLabel1 = "NODE_LABEL1"

  "generate schema from diffgraph" in {
    val diffGraph = new DiffGraphBuilder()
      .addNode(NodeLabel1)
      .build()

    val result = builder.build(diffGraph)
    result should startWith(s"package $schemaPackage")
    result should include("""val node_label1 = builder.addNodeType(name = "NODE_LABEL1")""")

  }

  // TODO write to files
  // TODO create sbt integration test

}
