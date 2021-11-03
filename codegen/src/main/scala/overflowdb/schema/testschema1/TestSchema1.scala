package overflowdb.schema.testschema1

import java.io.File
import overflowdb.codegen.CodeGen
import overflowdb.schema.Property.ValueType
import overflowdb.schema.{Constant, EdgeType, Property, SchemaBuilder, SchemaInfo}
import overflowdb.storage.ValueTypes

// TODO create integration test from this
object TestSchema1 extends App {
  val builder = new SchemaBuilder("Cpg", "io.shiftleft.codepropertygraph.generated")

  implicit val schemaInfo = SchemaInfo.forClass(getClass)

  // properties
  val name = builder
    .addProperty("NAME", ValueType.String, comment = "Name of represented object, e.g., method name (e.g. \"run\")")
    .mandatory(default = "[empty]")
    .protoId(5)

  val order = builder
    .addProperty("ORDER", ValueType.Int, comment =
        "General ordering property, such that the children of each AST-node are typically numbered from 1, ..., N (this is not enforced). The ordering has no technical meaning, but is used for pretty printing and OUGHT TO reflect order in the source code"
    ).mandatory(default = 0)
    .protoId(4)

  val hash = builder
    .addProperty("HASH", ValueType.String, comment = "Hash value of the artifact that this CPG is built from.")
    .protoId(120)

  val inheritsFromTypeFullName = builder
    .addProperty("INHERITS_FROM_TYPE_FULL_NAME", ValueType.String, comment =
        "The static types a TYPE_DECL inherits from. This property is matched against the FULL_NAME of TYPE nodes and thus it is required to have at least one TYPE node for each TYPE_FULL_NAME"
    ).asList()
     .protoId(53)

  val alias = builder
    .addProperty("ALIAS", ValueType.Boolean,
      comment = "Defines whether a PROPAGATE edge creates an alias"
    ).mandatory(default = false)
    .protoId(1)

  val localName = builder
    .addProperty("LOCAL_NAME", ValueType.String,
      comment = "Local name of referenced CONTAINED node. This key is deprecated."
    ).protoId(6)

  val edgekey1Lst = builder
    .addProperty("EDGEKEY_1_LST", ValueType.Int,
      comment = "test list edge key"
    ).asList()
    .protoId(6999)

  // edge types
  val ast = builder
    .addEdgeType(
      "AST",
      comment = "Syntax tree edge"
    ).protoId(3)

  // node base types
  val astNode = builder
    .addNodeBaseType(
      "AST_NODE",
      comment = "Any node that can exist in an abstract syntax tree"
    ).addProperties(order)

  // node types
  val namespaceBlock = builder
    .addNodeType(
      "NAMESPACE_BLOCK",
      comment = "A reference to a namespace"
    ).protoId(41)
    .addProperties(name, order)
    .extendz(astNode)

  val file = builder
    .addNodeType(
      "FILE",
      comment = "Node representing a source file - the root of the AST"
    )
    .protoId(38)
    .addProperties(name, hash, inheritsFromTypeFullName, order)
    .extendz(astNode)
    .addOutEdge(
      edge = ast,
      inNode = namespaceBlock,
      cardinalityOut = EdgeType.Cardinality.List,
      cardinalityIn = EdgeType.Cardinality.ZeroOrOne
    )
    .addContainedNode(namespaceBlock, "tags", Property.Cardinality.List)

  // constants
  val dispatchTypes = builder.addConstants(
    category = "DispatchTypes",
    Constant(
      "STATIC_DISPATCH",
      value = "STATIC_DISPATCH",
      valueType = ValueTypes.STRING,
      comment = "For statically dispatched calls the call target is known before program execution"
    ).protoId(1),
    Constant(
      "DYNAMIC_DISPATCH",
      value = "DYNAMIC_DISPATCH",
      valueType = ValueTypes.STRING,
      comment = "For dynamically dispatched calls the target is determined during runtime"
    ).protoId(2)
  )

  val frameworks = builder.addConstants(
    category = "Frameworks",
    Constant("SPRING", value = "SPRING", valueType = ValueTypes.STRING, comment = "Java spring framework").protoId(3),
    Constant("ASP_NET_MVC", value = "ASP_NET_MVC", valueType = ValueTypes.STRING, comment = "Microsoft ASP.NET MVC")
      .protoId(11),
    Constant("JAXWS", value = "JAXWS", valueType = ValueTypes.STRING, comment = "JAX-WS").protoId(12),
    Constant(
      "JAVA_INTERNAL",
      value = "JAVA_INTERNAL",
      valueType = ValueTypes.STRING,
      comment = "Framework facilities directly provided by Java"
    ).protoId(14),
    Constant(
      "ASP_NET_WEB_UI",
      value = "ASP_NET_WEB_UI",
      valueType = ValueTypes.STRING,
      comment = "Microsoft ASP.NET Web UI"
    ).protoId(13),
    Constant("JAXRS", value = "JAXRS", valueType = ValueTypes.STRING, comment = "JAX-RS").protoId(7),
    Constant("DROPWIZARD", value = "DROPWIZARD", valueType = ValueTypes.STRING, comment = "Dropwizard framework")
      .protoId(15),
    Constant("PLAY", value = "PLAY", valueType = ValueTypes.STRING, comment = "Play framework").protoId(1),
    Constant("SPARK", value = "SPARK", valueType = ValueTypes.STRING, comment = "Spark micro web framework").protoId(8),
    Constant("VERTX", value = "VERTX", valueType = ValueTypes.STRING, comment = "Polyglot event-driven framework")
      .protoId(4),
    Constant("JSF", value = "JSF", valueType = ValueTypes.STRING, comment = "JavaServer Faces").protoId(5),
    Constant(
      "ASP_NET_WEB_API",
      value = "ASP_NET_WEB_API",
      valueType = ValueTypes.STRING,
      comment = "Microsoft ASP.NET Web API"
    ).protoId(10),
    Constant("WCF", value = "WCF", valueType = ValueTypes.STRING, comment = "WCF HTTP and REST").protoId(16),
    Constant("GWT", value = "GWT", valueType = ValueTypes.STRING, comment = "Google web toolkit").protoId(2),
    Constant("SERVLET", value = "SERVLET", valueType = ValueTypes.STRING, comment = "Java Servlet based frameworks")
      .protoId(6),
    Constant("ASP_NET_CORE", value = "ASP_NET_CORE", valueType = ValueTypes.STRING, comment = "Microsoft ASP.NET Core")
      .protoId(9)
  )

  val languages = builder.addConstants(
    category = "Languages",
    Constant("JAVA", value = "JAVA", valueType = ValueTypes.STRING, comment = "").protoId(1),
    Constant("JAVASCRIPT", value = "JAVASCRIPT", valueType = ValueTypes.STRING, comment = "").protoId(2),
    Constant("GOLANG", value = "GOLANG", valueType = ValueTypes.STRING, comment = "").protoId(3),
    Constant("CSHARP", value = "CSHARP", valueType = ValueTypes.STRING, comment = "").protoId(4),
    Constant("C", value = "C", valueType = ValueTypes.STRING, comment = "").protoId(5),
    Constant("PYTHON", value = "PYTHON", valueType = ValueTypes.STRING, comment = "").protoId(6),
    Constant("LLVM", value = "LLVM", valueType = ValueTypes.STRING, comment = "").protoId(7),
    Constant("PHP", value = "PHP", valueType = ValueTypes.STRING, comment = "").protoId(8)
  )

  val modifierTypes = builder.addConstants(
    category = "ModifierTypes",
    Constant("STATIC", value = "STATIC", valueType = ValueTypes.STRING, comment = "The static modifier").protoId(1),
    Constant("PUBLIC", value = "PUBLIC", valueType = ValueTypes.STRING, comment = "The public modifier").protoId(2),
    Constant("PROTECTED", value = "PROTECTED", valueType = ValueTypes.STRING, comment = "The protected modifier")
      .protoId(3),
    Constant("PRIVATE", value = "PRIVATE", valueType = ValueTypes.STRING, comment = "The private modifier").protoId(4),
    Constant("ABSTRACT", value = "ABSTRACT", valueType = ValueTypes.STRING, comment = "The abstract modifier").protoId(5),
    Constant("NATIVE", value = "NATIVE", valueType = ValueTypes.STRING, comment = "The native modifier").protoId(6),
    Constant("CONSTRUCTOR", value = "CONSTRUCTOR", valueType = ValueTypes.STRING, comment = "The constructor modifier")
      .protoId(7),
    Constant("VIRTUAL", value = "VIRTUAL", valueType = ValueTypes.STRING, comment = "The virtual modifier").protoId(8)
  )

  val evaluationStrategies = builder.addConstants(
    category = "EvaluationStrategies",
    Constant(
      "BY_REFERENCE",
      value = "BY_REFERENCE",
      valueType = ValueTypes.STRING,
      comment =
        "A parameter or return of a function is passed by reference which means an address is used behind the scenes"
    ).protoId(1),
    Constant(
      "BY_SHARING",
      value = "BY_SHARING",
      valueType = ValueTypes.STRING,
      comment =
        "Only applicable to object parameter or return values. The pointer to the object is passed by value but the object itself is not copied and changes to it are thus propagated out of the method context"
    ).protoId(2),
    Constant(
      "BY_VALUE",
      value = "BY_VALUE",
      valueType = ValueTypes.STRING,
      comment = "A parameter or return of a function passed by value which means a flat copy is used"
    ).protoId(3)
  )

  val operators = builder.addConstants(
    category = "Operators",
    Constant("addition", value = "<operator>.addition", valueType = ValueTypes.STRING, comment = ""),
    Constant(
      "pointerShift",
      value = "<operator>.pointerShift",
      valueType = ValueTypes.STRING,
      comment =
        "Shifts a pointer. In terms of CPG, the first argument is the pointer and the second argument is the index. The index selection works the same way as for indirectIndexAccess. This operator is currently only used directly by the LLVM language, but it is also used internally for C. For example, pointerShift(ptr, 7) is equivalent to &(ptr[7]). Handling of this operator is special-cased in the back-end"
    )
  )

  builder.dontWarnForDuplicateProperty(file, order)
  builder.dontWarnForDuplicateProperty(namespaceBlock, order)

  new CodeGen(builder.build)
    .run(new File("target"))
}
