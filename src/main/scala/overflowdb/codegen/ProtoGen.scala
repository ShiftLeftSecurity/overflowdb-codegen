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
         |
         |""".stripMargin

    val _outputDir = outputDir.toScala
    _outputDir.createDirectories()
    val outputFile = _outputDir.createChild(s"${protoOpts.pkg}.proto").write(protoDef)
    outputFile.toJava
  }

}


