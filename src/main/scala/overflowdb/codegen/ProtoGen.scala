package overflowdb.codegen

import better.files._
import overflowdb.schema._
import overflowdb.storage.ValueTypes

/** Generates proto definitions for a given domain-specific schema. */
class ProtoGen(schema: Schema) {
  val basePackage = schema.basePackage
  val edgesPackage = s"$basePackage.nodes"

  def run(outputDir: java.io.File): java.io.File = {
    val protoOpts = schema.protoOptions.getOrElse(
      throw new AssertionError("schema doesn't have any proto options configured"))

    def toProtoDef(props: Seq[Property]): String =
      props
        .filter(_.protoId.isDefined)
        .sortBy(_.protoId.get)
        .map { prop =>
        val comment = prop.comment.map(comment => s"// $comment").getOrElse("")
        s"""  $comment
           |  ${prop.name} = ${prop.protoId.get}
           |""".stripMargin
      }.mkString("\n")

    val protoDef =
      s"""syntax = "proto3";
         |
         |package ${protoOpts.pkg};
         |
         |option go_package = "${protoOpts.goPackage}";
         |option java_package = "${protoOpts.javaPackage}";
         |option java_outer_classname = "${protoOpts.javaOuterClassname}";
         |option csharp_namespace = "${protoOpts.csharpNamespace}";
         |
         |enum NodePropertyName {
         |  UNKNOWN_NODE_PROPERTY = 0;
         |${toProtoDef(schema.nodeProperties)}
         |}
         |
         |enum EdgePropertyName {
         |  UNKNOWN_EDGE_PROPERTY = 0;
         |${toProtoDef(schema.edgeProperties)}
         |}
         |
         |""".stripMargin

    val _outputDir = outputDir.toScala
    _outputDir.createDirectories()
    val outputFile = _outputDir.createChild(s"${protoOpts.pkg}.proto").write(protoDef)
    outputFile.toJava
  }

}


