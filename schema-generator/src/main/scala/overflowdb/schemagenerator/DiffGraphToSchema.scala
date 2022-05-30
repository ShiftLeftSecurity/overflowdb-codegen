package overflowdb.schemagenerator

import overflowdb.BatchedUpdate._
import overflowdb.DetachedNodeGeneric

import java.lang.System.lineSeparator
import scala.collection.mutable

/**
  * Create a base schema definition from a DiffGraph, as e.g. generated by overflowdb.formats.GraphMLImporter
  * This will give you a baseline for creating an overflowdb schema, so you can generate domain types etc.
  *
  * You will need to fill in some gaps later, e.g.
  * - properties: cardinalities, potentially reuse across different nodes/edges, comments
  * - relationships: cardinalities, comments
  * - refactor for readability: order, split into files etc.
  * - many more
  *
  * Note: this isn't optimised for performance and not tested on large input diffgraphs.
  */
class DiffGraphToSchema(domainName: String, schemaPackage: String, targetPackage: String) {

  def build(diffGraph: DiffGraph): String = {
    val nodeTypes = mutable.Set.empty[String]

    diffGraph.iterator().forEachRemaining {
      case node: DetachedNodeGeneric =>
        nodeTypes.addOne(node.label)
        // TODO add node properties, their types etc.
    }

    val nodes = nodeTypes.map { nodeType =>
      // TODO use scala names, i.e. lowercase etc. -> reuse codegen utils?
      val schemaNodeName = nodeType.toLowerCase
      s"""val $schemaNodeName = builder.addNodeType(name = "$nodeType")
         |""".stripMargin
    }.mkString(s"$lineSeparator$lineSeparator")

    s"""package $schemaPackage
       |
       |import overflowdb.schema.{Schema, SchemaBuilder}
       |
       |object ${domainName}Schema {
       |  val builder = new SchemaBuilder(
       |      domainShortName = "$domainName",
       |      basePackage = "$targetPackage"
       |  )
       |
       |  $nodes
       |
       |  val instance: Schema = builder.build()
       |}
       |
       |""".stripMargin
  }

}