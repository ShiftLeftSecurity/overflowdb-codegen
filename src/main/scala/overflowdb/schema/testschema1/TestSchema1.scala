package overflowdb.schema.testschema1

import java.io.File
import overflowdb.codegen.CodeGen
import overflowdb.schema.{Cardinality, Constant, SchemaBuilder}

// TODO create integration test from this
object TestSchema1 extends App {
  val builder = new SchemaBuilder("io.shiftleft.codepropertygraph.generated")

  // node properties
  val name = builder.addNodeProperty(
    name = "NAME",
    valueType = "String",
    cardinality = Cardinality.One,
    comment = "Name of represented object, e.g., method name (e.g. \"run\")"
  ).protoId(5)

  val order = builder.addNodeProperty(
    name = "ORDER",
    valueType = "Integer",
    cardinality = Cardinality.One,
    comment = "General ordering property, such that the children of each AST-node are typically numbered from 1, ..., N (this is not enforced). The ordering has no technical meaning, but is used for pretty printing and OUGHT TO reflect order in the source code"
  ).protoId(4)

  val hash = builder.addNodeProperty(
    name = "HASH",
    valueType = "String",
    cardinality = Cardinality.ZeroOrOne,
    comment = "Hash value of the artifact that this CPG is built from."
  ).protoId(120)

  val inheritsFromTypeFullName = builder.addNodeProperty(
    name = "INHERITS_FROM_TYPE_FULL_NAME",
    valueType = "String",
    cardinality = Cardinality.List,
    comment = "The static types a TYPE_DECL inherits from. This property is matched against the FULL_NAME of TYPE nodes and thus it is required to have at least one TYPE node for each TYPE_FULL_NAME"
  ).protoId(53)

  // edge properties
  val alias = builder.addEdgeProperty(
    name = "ALIAS",
    valueType = "Boolean",
    cardinality = Cardinality.One,
    comment = "Defines whether a PROPAGATE edge creates an alias"
  ).protoId(1)

  val localName = builder.addEdgeProperty(
    name = "LOCAL_NAME",
    valueType = "String",
    cardinality = Cardinality.ZeroOrOne,
    comment = "Local name of referenced CONTAINED node. This key is deprecated."
  ).protoId(6)

  val edgekey1Lst = builder.addEdgeProperty(
    name = "EDGEKEY_1_LST",
    valueType = "Integer",
    cardinality = Cardinality.List,
    comment = "test list edge key"
  ).protoId(6999)

  // edge types
  val ast = builder.addEdgeType(
    name = "AST",
    comment = "Syntax tree edge"
  ).protoId(3)


  // node base types
  val astNode = builder.addNodeBaseType(
    name = "AST_NODE",
    comment = "Any node that can exist in an abstract syntax tree"
  ).addProperties(order)

  // node types
  val namespaceBlock = builder.addNodeType(
    name = "NAMESPACE_BLOCK",
    comment = "A reference to a namespace"
  ).protoId(41)
    .addProperties(name, order)
    .extendz(astNode)



  val file = builder.addNodeType(
    name = "FILE",
    comment = "Node representing a source file - the root of the AST"
  ).protoId(38)
    .addProperties(name, hash, inheritsFromTypeFullName, order)
    .extendz(astNode)
    .addOutEdge(edge = ast, inNode = namespaceBlock, cardinalityOut = Cardinality.List, cardinalityIn = Cardinality.ZeroOrOne)
    .addContainedNode(namespaceBlock, "tags", Cardinality.List)

  // constants
  val dispatchTypes = builder.addConstants(category = "DispatchTypes",
    Constant(name = "STATIC_DISPATCH", value = "STATIC_DISPATCH", valueType = "String", comment = "For statically dispatched calls the call target is known before program execution"),
    Constant(name = "DYNAMIC_DISPATCH", value = "DYNAMIC_DISPATCH", valueType = "String", comment = "For dynamically dispatched calls the target is determined during runtime"),
  )

  val frameworks = builder.addConstants(category = "Frameworks",
    Constant(name = "SPRING", value = "SPRING", valueType = "String", comment = "Java spring framework"),
    Constant(name = "ASP_NET_MVC", value = "ASP_NET_MVC", valueType = "String", comment = "Microsoft ASP.NET MVC"),
    Constant(name = "JAXWS", value = "JAXWS", valueType = "String", comment = "JAX-WS"),
    Constant(name = "JAVA_INTERNAL", value = "JAVA_INTERNAL", valueType = "String", comment = "Framework facilities directly provided by Java"),
    Constant(name = "ASP_NET_WEB_UI", value = "ASP_NET_WEB_UI", valueType = "String", comment = "Microsoft ASP.NET Web UI"),
    Constant(name = "JAXRS", value = "JAXRS", valueType = "String", comment = "JAX-RS"),
    Constant(name = "DROPWIZARD", value = "DROPWIZARD", valueType = "String", comment = "Dropwizard framework"),
    Constant(name = "PLAY", value = "PLAY", valueType = "String", comment = "Play framework"),
    Constant(name = "SPARK", value = "SPARK", valueType = "String", comment = "Spark micro web framework"),
    Constant(name = "VERTX", value = "VERTX", valueType = "String", comment = "Polyglot event-driven framework"),
    Constant(name = "JSF", value = "JSF", valueType = "String", comment = "JavaServer Faces"),
    Constant(name = "ASP_NET_WEB_API", value = "ASP_NET_WEB_API", valueType = "String", comment = "Microsoft ASP.NET Web API"),
    Constant(name = "WCF", value = "WCF", valueType = "String", comment = "WCF HTTP and REST"),
    Constant(name = "GWT", value = "GWT", valueType = "String", comment = "Google web toolkit"),
    Constant(name = "SERVLET", value = "SERVLET", valueType = "String", comment = "Java Servlet based frameworks"),
    Constant(name = "ASP_NET_CORE", value = "ASP_NET_CORE", valueType = "String", comment = "Microsoft ASP.NET Core"),
  )

  val languages = builder.addConstants(category = "Languages",
    Constant(name = "JAVA", value = "JAVA", valueType = "String", comment = ""),
    Constant(name = "JAVASCRIPT", value = "JAVASCRIPT", valueType = "String", comment = ""),
    Constant(name = "GOLANG", value = "GOLANG", valueType = "String", comment = ""),
    Constant(name = "CSHARP", value = "CSHARP", valueType = "String", comment = ""),
    Constant(name = "C", value = "C", valueType = "String", comment = ""),
    Constant(name = "PYTHON", value = "PYTHON", valueType = "String", comment = ""),
    Constant(name = "LLVM", value = "LLVM", valueType = "String", comment = ""),
    Constant(name = "PHP", value = "PHP", valueType = "String", comment = ""),
  )

  val modifierTypes = builder.addConstants(category = "ModifierTypes",
    Constant(name = "STATIC", value = "STATIC", valueType = "String", comment = "The static modifier"),
    Constant(name = "PUBLIC", value = "PUBLIC", valueType = "String", comment = "The public modifier"),
    Constant(name = "PROTECTED", value = "PROTECTED", valueType = "String", comment = "The protected modifier"),
    Constant(name = "PRIVATE", value = "PRIVATE", valueType = "String", comment = "The private modifier"),
    Constant(name = "ABSTRACT", value = "ABSTRACT", valueType = "String", comment = "The abstract modifier"),
    Constant(name = "NATIVE", value = "NATIVE", valueType = "String", comment = "The native modifier"),
    Constant(name = "CONSTRUCTOR", value = "CONSTRUCTOR", valueType = "String", comment = "The constructor modifier"),
    Constant(name = "VIRTUAL", value = "VIRTUAL", valueType = "String", comment = "The virtual modifier"),
  )

  val evaluationStrategies = builder.addConstants(category = "EvaluationStrategies",
    Constant(name = "BY_REFERENCE", value = "BY_REFERENCE", valueType = "String", comment = "A parameter or return of a function is passed by reference which means an address is used behind the scenes"),
    Constant(name = "BY_SHARING", value = "BY_SHARING", valueType = "String", comment = "Only applicable to object parameter or return values. The pointer to the object is passed by value but the object itself is not copied and changes to it are thus propagated out of the method context"),
    Constant(name = "BY_VALUE", value = "BY_VALUE", valueType = "String", comment = "A parameter or return of a function passed by value which means a flat copy is used"),
  )

//  val List(Constant(addition,<operator>.addition,None,None,None), Constant(pointerShift,<operator>.pointerShift,Some(Shifts a pointer. In terms of CPG, the first argument is the pointer and the second argument is the index. The index selection works the same way as for indirectIndexAccess. This operator is currently only used directly by the LLVM language, but it is also used internally for C. For example, pointerShift(ptr, 7) is equivalent to &(ptr[7]). Handling of this operator is special-cased in the back-end),None,None)) = builder.addConstants(category = "Operators",
//    Constant(name = "addition", value = "<operator>.addition", valueType = "String", comment = ""),
//    Constant(name = "pointerShift", value = "<operator>.pointerShift", valueType = "String", comment = "Shifts a pointer. In terms of CPG, the first argument is the pointer and the second argument is the index. The index selection works the same way as for indirectIndexAccess. This operator is currently only used directly by the LLVM language, but it is also used internally for C. For example, pointerShift(ptr, 7) is equivalent to &(ptr[7]). Handling of this operator is special-cased in the back-end"),
//  )

  new CodeGen(builder.build).run(new File("target"))
}
