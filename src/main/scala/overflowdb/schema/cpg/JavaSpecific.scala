package overflowdb.schema.cpg

import overflowdb.schema._

/**
 * This is only intended for Java.
 */
object JavaSpecific {
  def apply(builder: SchemaBuilder, base: Base.Schema, enhancements: Enhancements.Schema) =
    new Schema(builder, base, enhancements)

  class Schema(builder: SchemaBuilder, base: Base.Schema, enhancements: Enhancements.Schema) {
    import base._
    import enhancements._

    // node properties
    val binarySignature = builder.addNodeProperty(
      name = "BINARY_SIGNATURE",
      valueType = "String",
      cardinality = Cardinality.ZeroOrOne,
      comment = "Binary type signature"
    ).protoId(14)

    val content = builder.addNodeProperty(
      name = "CONTENT",
      valueType = "String",
      cardinality = Cardinality.One,
      comment = "Content of CONFIG_FILE node"
    ).protoId(20)

    // node types
    lazy val annotation: NodeType = builder.addNodeType(
      name = "ANNOTATION",
      comment = "A method annotation"
    ).protoId(5)

      .addProperties(code, name, fullName, order)
      .extendz(astNode)
      .addOutEdge(edge = ast, inNode = annotationParameterAssign)


    lazy val annotationParameterAssign: NodeType = builder.addNodeType(
      name = "ANNOTATION_PARAMETER_ASSIGN",
      comment = "Assignment of annotation argument to annotation parameter"
    ).protoId(6)

      .addProperties(code, order)
      .extendz(astNode)
      .addOutEdge(edge = ast, inNode = annotationParameter)
      .addOutEdge(edge = ast, inNode = arrayInitializer)
      .addOutEdge(edge = ast, inNode = annotationLiteral)
      .addOutEdge(edge = ast, inNode = annotation)


    lazy val annotationParameter: NodeType = builder.addNodeType(
      name = "ANNOTATION_PARAMETER",
      comment = "Formal annotation parameter"
    ).protoId(7)

      .addProperties(code, order)
      .extendz(astNode)



    lazy val annotationLiteral: NodeType = builder.addNodeType(
      name = "ANNOTATION_LITERAL",
      comment = "A literal value assigned to an ANNOTATION_PARAMETER"
    ).protoId(49)

      .addProperties(code, name, order, argumentIndex, columnNumber, lineNumber)
      .extendz(expression)



    arrayInitializer
      .addProperties(code, order, argumentIndex, columnNumber, lineNumber)
      .extendz(expression)
      .addOutEdge(edge = ast, inNode = literal)
      .addOutEdge(edge = evalType, inNode = tpe)


    method
      .addProperties(binarySignature)

      .addOutEdge(edge = ast, inNode = annotation)


    methodParameterIn
      .addProperties()

      .addOutEdge(edge = ast, inNode = annotation)


    typeDecl
      .addProperties()

      .addOutEdge(edge = ast, inNode = annotation)


    member
      .addProperties()

      .addOutEdge(edge = ast, inNode = annotation)


    lazy val configFile: NodeType = builder.addNodeType(
      name = "CONFIG_FILE",
      comment = "Configuration file contents. Might be in use by a framework"
    ).protoId(50)

      .addProperties(name, content)
      .extendz(trackingPoint)



  }

}
