package overflowdb.schemagenerator

import overflowdb.formats.graphml.GraphMLImporter

import java.nio.file.Paths

object GraphMLToSchema extends App {

  // TODO use scopt, add tests etc.
  val diffGraph = GraphMLImporter.createDiffGraph(Seq(Paths.get("/home/mp/Projects/shiftleft/overflowdb/src/test/resources/grateful-dead.xml")))
  println(diffGraph.size())

  new DiffGraphToSchema(
    domainName = "gratefuldead", schemaPackage = "org.test.schema", targetPackage = "org.test"
  ).toFile(diffGraph, Paths.get("target/Schema.scala"))

}
