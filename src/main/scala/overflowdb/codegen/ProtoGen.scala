package overflowdb.codegen

import better.files._
import overflowdb.schema._
import overflowdb.storage.ValueTypes

/** Generates proto definitions for a given domain-specific schema. */
class ProtoGen(schema: Schema) {
  import Helpers._
  val basePackage = schema.basePackage
  val edgesPackage = s"$basePackage.nodes"

  def run(outputDir: java.io.File): java.io.File = {
    val protoOpts = schema.protoOptions.getOrElse(
      throw new AssertionError("schema doesn't have any proto options configured"))

    // TODO move outer loop into protoDef string interpolation?
    val enumsFromConstants: String = schema.constantsByCategory.map { case (categoryName, entries) =>
      val categoryNameSingular = singularize(categoryName)
      val unknownEntry = snakeCase(categoryNameSingular).toUpperCase

      s"""enum $categoryName {
         |  UNKNOWN_$unknownEntry = 0;
         |
         |  ${protoDefs(entries.map(enumEntryMaybe))}
         |}
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
         |
         |${protoDefs(schema.nodeProperties.map(enumEntryMaybe))}
         |}
         |
         |enum EdgePropertyName {
         |  UNKNOWN_EDGE_PROPERTY = 0;
         |
         |${protoDefs(schema.edgeProperties.map(enumEntryMaybe))}
         |}
         |
         |$enumsFromConstants
         |
         |""".stripMargin

    val _outputDir = outputDir.toScala
    _outputDir.createDirectories()
    val outputFile = _outputDir.createChild(s"${protoOpts.pkg}.proto").write(protoDef)
    outputFile.toJava
  }

  private def protoDefs(enumCases: Seq[EnumEntryMaybe]): String = {
    enumCases.filter(_.protoId.isDefined).sortBy(_.protoId.get).map { enumCase =>
      val comment = enumCase.comment.map(comment => s"// $comment").getOrElse("")
      s"""  $comment
         |  ${enumCase.name} = ${enumCase.protoId.get}
         |""".stripMargin
    }.mkString("\n")
  }

  private def enumEntryMaybe(constant: Constant): EnumEntryMaybe =
    EnumEntryMaybe(constant.protoId, constant.name, constant.comment)

  private def enumEntryMaybe(property: Property): EnumEntryMaybe =
    EnumEntryMaybe(property.protoId, property.name, property.comment)

  case class EnumEntryMaybe(protoId: Option[Int], name: String, comment: Option[String])
}


