package overflowdb.schema.cpg

import overflowdb.schema._

object EnhancementsInternal {
  def apply(
      builder: SchemaBuilder,
      base: Base.Schema,
      enhancements: Enhancements.Schema,
      javaSpecific: JavaSpecific.Schema
  ) =
    new Schema(builder, base, enhancements, javaSpecific)

  class Schema(
      builder: SchemaBuilder,
      base: Base.Schema,
      enhancements: Enhancements.Schema,
      javaSpecific: JavaSpecific.Schema
  ) {
    import base._
    import enhancements._
    import javaSpecific._

    // node properties
    val depthFirstOrder = builder
      .addNodeProperty(
        name = "DEPTH_FIRST_ORDER",
        valueType = "Integer",
        cardinality = Cardinality.ZeroOrOne,
        comment = "The depth first ordering number used to detect back edges in reducible CFGs"
      )
      .protoId(17)

    val hasMapping = builder
      .addNodeProperty(
        name = "HAS_MAPPING",
        valueType = "Boolean",
        cardinality = Cardinality.ZeroOrOne,
        comment = "Marks that a method has at least one mapping defined from the policies"
      )
      .protoId(23)

    val internalFlags = builder
      .addNodeProperty(
        name = "INTERNAL_FLAGS",
        valueType = "Integer",
        cardinality = Cardinality.ZeroOrOne,
        comment = "Internal flags"
      )
      .protoId(78)

    // edge types
    val taintRemove = builder
      .addEdgeType(
        name = "TAINT_REMOVE",
        comment =
          "Indicates taint removal. Only present between corresponding METHOD_PARAMETER_IN and METHOD_PARAMETER_OUT nodes"
      )
      .protoId(17)

    val dynamicType = builder
      .addEdgeType(
        name = "DYNAMIC_TYPE",
        comment =
          "Indicates the dynamic type(s) of an entity. This comes initially from the frontend provided DYNAMIC_TYPE_HINT_FULL_NAME property and is extended by our type resolution"
      )
      .protoId(20)

    val dominate = builder
      .addEdgeType(
        name = "DOMINATE",
        comment = "Points to dominated node in DOM tree"
      )
      .protoId(181)

    val postDominate = builder
      .addEdgeType(
        name = "POST_DOMINATE",
        comment = "Points to dominated node in post DOM tree"
      )
      .protoId(182)

    val cdg = builder
      .addEdgeType(
        name = "CDG",
        comment = "Control dependency graph"
      )
      .protoId(183)

    // deprecated, c.f. CONFIG_FILE
    val attachedData = builder
      .addEdgeType(
        name = "ATTACHED_DATA",
        comment = "Link between FRAMEWORK and FRAMEWORK_DATA nodes"
      )
      .protoId(18)

    // node types
    cfgNode.addProperties(internalFlags)

    jumpTarget
      .addProperties(internalFlags)
      .addOutEdge(edge = dominate, inNode = callNode)
      .addOutEdge(edge = dominate, inNode = identifier)
      .addOutEdge(edge = dominate, inNode = fieldIdentifier)
      .addOutEdge(edge = dominate, inNode = literal)
      .addOutEdge(edge = dominate, inNode = ret)
      .addOutEdge(edge = dominate, inNode = methodRef)
      .addOutEdge(edge = dominate, inNode = typeRef)
      .addOutEdge(edge = dominate, inNode = block)
      .addOutEdge(edge = dominate, inNode = jumpTarget)
      .addOutEdge(edge = dominate, inNode = controlStructure)
      .addOutEdge(edge = dominate, inNode = unknown)
      .addOutEdge(edge = postDominate, inNode = callNode)
      .addOutEdge(edge = postDominate, inNode = identifier)
      .addOutEdge(edge = postDominate, inNode = fieldIdentifier)
      .addOutEdge(edge = postDominate, inNode = literal)
      .addOutEdge(edge = postDominate, inNode = ret)
      .addOutEdge(edge = postDominate, inNode = methodRef)
      .addOutEdge(edge = postDominate, inNode = typeRef)
      .addOutEdge(edge = postDominate, inNode = block)
      .addOutEdge(edge = postDominate, inNode = jumpTarget)
      .addOutEdge(edge = postDominate, inNode = controlStructure)
      .addOutEdge(edge = postDominate, inNode = unknown)
      .addOutEdge(edge = cdg, inNode = callNode)
      .addOutEdge(edge = cdg, inNode = identifier)
      .addOutEdge(edge = cdg, inNode = fieldIdentifier)
      .addOutEdge(edge = cdg, inNode = literal)
      .addOutEdge(edge = cdg, inNode = methodRef)
      .addOutEdge(edge = cdg, inNode = typeRef)
      .addOutEdge(edge = cdg, inNode = ret)
      .addOutEdge(edge = cdg, inNode = block)
      .addOutEdge(edge = cdg, inNode = methodReturn)
      .addOutEdge(edge = cdg, inNode = controlStructure)
      .addOutEdge(edge = cdg, inNode = jumpTarget)
      .addOutEdge(edge = cdg, inNode = unknown)

    lazy val tags: NodeType = builder
      .addNodeType(
        name = "TAGS",
        comment = "Multiple tags"
      )
      .protoId(301)
      .addProperties()
      .addContainedNode(tag, "tags", Cardinality.List)

    // deprecated, c.f. CONFIG_FILE
    lazy val framework: NodeType = builder
      .addNodeType(
        name = "FRAMEWORK",
        comment =
          "Indicates the usage of a framework. E.g. java spring. The name key is one of the values from frameworks list"
      )
      .protoId(42)
      .addProperties(name)
      .addOutEdge(edge = attachedData, inNode = frameworkData)

    // deprecated, c.f. CONFIG_FILE
    lazy val frameworkData: NodeType = builder
      .addNodeType(
        name = "FRAMEWORK_DATA",
        comment = "Data used by a framework"
      )
      .protoId(43)
      .addProperties(name, content)

    method
      .addProperties(hasMapping, depthFirstOrder, internalFlags)
      .addOutEdge(edge = taggedBy, inNode = tag)
      .addOutEdge(edge = dominate, inNode = callNode)
      .addOutEdge(edge = dominate, inNode = identifier)
      .addOutEdge(edge = dominate, inNode = fieldIdentifier)
      .addOutEdge(edge = dominate, inNode = literal)
      .addOutEdge(edge = dominate, inNode = methodRef)
      .addOutEdge(edge = dominate, inNode = typeRef)
      .addOutEdge(edge = dominate, inNode = ret)
      .addOutEdge(edge = dominate, inNode = block)
      .addOutEdge(edge = dominate, inNode = methodReturn)
      .addOutEdge(edge = dominate, inNode = unknown)

    methodReturn
      .addProperties(depthFirstOrder, internalFlags)
      .addOutEdge(edge = taggedBy, inNode = tag)
      .addOutEdge(edge = dynamicType, inNode = typeDecl)
      .addOutEdge(edge = dynamicType, inNode = method, cardinalityOut = Cardinality.ZeroOrOne)
      .addOutEdge(edge = postDominate, inNode = callNode)
      .addOutEdge(edge = postDominate, inNode = identifier)
      .addOutEdge(edge = postDominate, inNode = fieldIdentifier)
      .addOutEdge(edge = postDominate, inNode = literal)
      .addOutEdge(edge = postDominate, inNode = methodRef)
      .addOutEdge(edge = postDominate, inNode = typeRef)
      .addOutEdge(edge = postDominate, inNode = ret)
      .addOutEdge(edge = postDominate, inNode = block)
      .addOutEdge(edge = postDominate, inNode = method)
      .addOutEdge(edge = postDominate, inNode = controlStructure)
      .addOutEdge(edge = postDominate, inNode = jumpTarget)
      .addOutEdge(edge = postDominate, inNode = unknown)

    literal
      .addProperties(depthFirstOrder, internalFlags)
      .addOutEdge(edge = taggedBy, inNode = tag)
      .addOutEdge(edge = dynamicType, inNode = typeDecl)
      .addOutEdge(edge = dynamicType, inNode = method)
      .addOutEdge(edge = dominate, inNode = callNode)
      .addOutEdge(edge = dominate, inNode = identifier)
      .addOutEdge(edge = dominate, inNode = fieldIdentifier)
      .addOutEdge(edge = dominate, inNode = literal)
      .addOutEdge(edge = dominate, inNode = methodRef)
      .addOutEdge(edge = dominate, inNode = typeRef)
      .addOutEdge(edge = dominate, inNode = ret)
      .addOutEdge(edge = dominate, inNode = block)
      .addOutEdge(edge = dominate, inNode = methodReturn)
      .addOutEdge(edge = dominate, inNode = controlStructure)
      .addOutEdge(edge = dominate, inNode = jumpTarget)
      .addOutEdge(edge = dominate, inNode = unknown)
      .addOutEdge(edge = postDominate, inNode = callNode)
      .addOutEdge(edge = postDominate, inNode = identifier)
      .addOutEdge(edge = postDominate, inNode = fieldIdentifier)
      .addOutEdge(edge = postDominate, inNode = literal)
      .addOutEdge(edge = postDominate, inNode = methodRef)
      .addOutEdge(edge = postDominate, inNode = typeRef)
      .addOutEdge(edge = postDominate, inNode = ret)
      .addOutEdge(edge = postDominate, inNode = block)
      .addOutEdge(edge = postDominate, inNode = method)
      .addOutEdge(edge = postDominate, inNode = controlStructure)
      .addOutEdge(edge = postDominate, inNode = jumpTarget)
      .addOutEdge(edge = postDominate, inNode = unknown)
      .addOutEdge(edge = cdg, inNode = callNode)
      .addOutEdge(edge = cdg, inNode = identifier)
      .addOutEdge(edge = cdg, inNode = fieldIdentifier)
      .addOutEdge(edge = cdg, inNode = literal)
      .addOutEdge(edge = cdg, inNode = methodRef)
      .addOutEdge(edge = cdg, inNode = typeRef)
      .addOutEdge(edge = cdg, inNode = ret)
      .addOutEdge(edge = cdg, inNode = block)
      .addOutEdge(edge = cdg, inNode = methodReturn)
      .addOutEdge(edge = cdg, inNode = controlStructure)
      .addOutEdge(edge = cdg, inNode = jumpTarget)
      .addOutEdge(edge = cdg, inNode = unknown)

    local
      .addOutEdge(edge = taggedBy, inNode = tag)
      .addOutEdge(edge = dynamicType, inNode = typeDecl)
      .addOutEdge(edge = dynamicType, inNode = method)

    member
      .addOutEdge(edge = dynamicType, inNode = typeDecl)
      .addOutEdge(edge = taggedBy, inNode = tag)

    callNode
      .addProperties(depthFirstOrder, internalFlags)
      .addOutEdge(edge = taggedBy, inNode = tag)
      .addOutEdge(edge = dynamicType, inNode = typeDecl)
      .addOutEdge(edge = dynamicType, inNode = method)
      .addOutEdge(edge = dominate, inNode = callNode)
      .addOutEdge(edge = dominate, inNode = identifier)
      .addOutEdge(edge = dominate, inNode = fieldIdentifier)
      .addOutEdge(edge = dominate, inNode = literal)
      .addOutEdge(edge = dominate, inNode = methodRef)
      .addOutEdge(edge = dominate, inNode = typeRef)
      .addOutEdge(edge = dominate, inNode = ret)
      .addOutEdge(edge = dominate, inNode = block)
      .addOutEdge(edge = dominate, inNode = methodReturn)
      .addOutEdge(edge = dominate, inNode = controlStructure)
      .addOutEdge(edge = dominate, inNode = jumpTarget)
      .addOutEdge(edge = dominate, inNode = unknown)
      .addOutEdge(edge = postDominate, inNode = callNode)
      .addOutEdge(edge = postDominate, inNode = identifier)
      .addOutEdge(edge = postDominate, inNode = fieldIdentifier)
      .addOutEdge(edge = postDominate, inNode = literal)
      .addOutEdge(edge = postDominate, inNode = methodRef)
      .addOutEdge(edge = postDominate, inNode = typeRef)
      .addOutEdge(edge = postDominate, inNode = ret)
      .addOutEdge(edge = postDominate, inNode = block)
      .addOutEdge(edge = postDominate, inNode = method)
      .addOutEdge(edge = postDominate, inNode = controlStructure)
      .addOutEdge(edge = postDominate, inNode = jumpTarget)
      .addOutEdge(edge = postDominate, inNode = unknown)
      .addOutEdge(edge = cdg, inNode = callNode)
      .addOutEdge(edge = cdg, inNode = identifier)
      .addOutEdge(edge = cdg, inNode = fieldIdentifier)
      .addOutEdge(edge = cdg, inNode = literal)
      .addOutEdge(edge = cdg, inNode = methodRef)
      .addOutEdge(edge = cdg, inNode = typeRef)
      .addOutEdge(edge = cdg, inNode = ret)
      .addOutEdge(edge = cdg, inNode = block)
      .addOutEdge(edge = cdg, inNode = methodReturn)
      .addOutEdge(edge = cdg, inNode = controlStructure)
      .addOutEdge(edge = cdg, inNode = jumpTarget)
      .addOutEdge(edge = cdg, inNode = unknown)

    identifier
      .addProperties(depthFirstOrder, internalFlags)
      .addOutEdge(edge = taggedBy, inNode = tag)
      .addOutEdge(edge = dynamicType, inNode = typeDecl)
      .addOutEdge(edge = dynamicType, inNode = method)
      .addOutEdge(edge = dominate, inNode = callNode)
      .addOutEdge(edge = dominate, inNode = identifier)
      .addOutEdge(edge = dominate, inNode = fieldIdentifier)
      .addOutEdge(edge = dominate, inNode = literal)
      .addOutEdge(edge = dominate, inNode = methodRef)
      .addOutEdge(edge = dominate, inNode = typeRef)
      .addOutEdge(edge = dominate, inNode = ret)
      .addOutEdge(edge = dominate, inNode = block)
      .addOutEdge(edge = dominate, inNode = methodReturn)
      .addOutEdge(edge = dominate, inNode = controlStructure)
      .addOutEdge(edge = dominate, inNode = jumpTarget)
      .addOutEdge(edge = dominate, inNode = unknown)
      .addOutEdge(edge = postDominate, inNode = callNode)
      .addOutEdge(edge = postDominate, inNode = identifier)
      .addOutEdge(edge = postDominate, inNode = fieldIdentifier)
      .addOutEdge(edge = postDominate, inNode = literal)
      .addOutEdge(edge = postDominate, inNode = methodRef)
      .addOutEdge(edge = postDominate, inNode = typeRef)
      .addOutEdge(edge = postDominate, inNode = ret)
      .addOutEdge(edge = postDominate, inNode = block)
      .addOutEdge(edge = postDominate, inNode = method)
      .addOutEdge(edge = postDominate, inNode = controlStructure)
      .addOutEdge(edge = postDominate, inNode = jumpTarget)
      .addOutEdge(edge = postDominate, inNode = unknown)
      .addOutEdge(edge = cdg, inNode = callNode)
      .addOutEdge(edge = cdg, inNode = identifier)
      .addOutEdge(edge = cdg, inNode = fieldIdentifier)
      .addOutEdge(edge = cdg, inNode = literal)
      .addOutEdge(edge = cdg, inNode = methodRef)
      .addOutEdge(edge = cdg, inNode = typeRef)
      .addOutEdge(edge = cdg, inNode = ret)
      .addOutEdge(edge = cdg, inNode = block)
      .addOutEdge(edge = cdg, inNode = methodReturn)
      .addOutEdge(edge = cdg, inNode = controlStructure)
      .addOutEdge(edge = cdg, inNode = jumpTarget)
      .addOutEdge(edge = cdg, inNode = unknown)

    fieldIdentifier
      .addProperties(depthFirstOrder, internalFlags)
      .addOutEdge(edge = taggedBy, inNode = tag)
      .addOutEdge(edge = dynamicType, inNode = typeDecl)
      .addOutEdge(edge = dynamicType, inNode = method)
      .addOutEdge(edge = dominate, inNode = callNode)
      .addOutEdge(edge = dominate, inNode = identifier)
      .addOutEdge(edge = dominate, inNode = fieldIdentifier)
      .addOutEdge(edge = dominate, inNode = literal)
      .addOutEdge(edge = dominate, inNode = methodRef)
      .addOutEdge(edge = dominate, inNode = typeRef)
      .addOutEdge(edge = dominate, inNode = ret)
      .addOutEdge(edge = dominate, inNode = block)
      .addOutEdge(edge = dominate, inNode = methodReturn)
      .addOutEdge(edge = dominate, inNode = controlStructure)
      .addOutEdge(edge = dominate, inNode = jumpTarget)
      .addOutEdge(edge = dominate, inNode = unknown)
      .addOutEdge(edge = postDominate, inNode = callNode)
      .addOutEdge(edge = postDominate, inNode = identifier)
      .addOutEdge(edge = postDominate, inNode = fieldIdentifier)
      .addOutEdge(edge = postDominate, inNode = literal)
      .addOutEdge(edge = postDominate, inNode = methodRef)
      .addOutEdge(edge = postDominate, inNode = typeRef)
      .addOutEdge(edge = postDominate, inNode = ret)
      .addOutEdge(edge = postDominate, inNode = block)
      .addOutEdge(edge = postDominate, inNode = method)
      .addOutEdge(edge = postDominate, inNode = controlStructure)
      .addOutEdge(edge = postDominate, inNode = jumpTarget)
      .addOutEdge(edge = postDominate, inNode = unknown)
      .addOutEdge(edge = cdg, inNode = callNode)
      .addOutEdge(edge = cdg, inNode = identifier)
      .addOutEdge(edge = cdg, inNode = fieldIdentifier)
      .addOutEdge(edge = cdg, inNode = literal)
      .addOutEdge(edge = cdg, inNode = methodRef)
      .addOutEdge(edge = cdg, inNode = typeRef)
      .addOutEdge(edge = cdg, inNode = ret)
      .addOutEdge(edge = cdg, inNode = block)
      .addOutEdge(edge = cdg, inNode = methodReturn)
      .addOutEdge(edge = cdg, inNode = controlStructure)
      .addOutEdge(edge = cdg, inNode = jumpTarget)
      .addOutEdge(edge = cdg, inNode = unknown)

    methodParameterIn
      .addOutEdge(edge = taggedBy, inNode = tag)
      .addOutEdge(edge = dynamicType, inNode = typeDecl)
      .addOutEdge(edge = dynamicType, inNode = method)
      .addOutEdge(edge = taintRemove, inNode = methodParameterOut)

    ret
      .addProperties(depthFirstOrder, internalFlags)
      .addOutEdge(edge = dominate, inNode = callNode)
      .addOutEdge(edge = dominate, inNode = identifier)
      .addOutEdge(edge = dominate, inNode = fieldIdentifier)
      .addOutEdge(edge = dominate, inNode = literal)
      .addOutEdge(edge = dominate, inNode = methodRef)
      .addOutEdge(edge = dominate, inNode = typeRef)
      .addOutEdge(edge = dominate, inNode = ret)
      .addOutEdge(edge = dominate, inNode = block)
      .addOutEdge(edge = dominate, inNode = methodReturn)
      .addOutEdge(edge = dominate, inNode = controlStructure)
      .addOutEdge(edge = dominate, inNode = jumpTarget)
      .addOutEdge(edge = dominate, inNode = unknown)
      .addOutEdge(edge = postDominate, inNode = callNode)
      .addOutEdge(edge = postDominate, inNode = identifier)
      .addOutEdge(edge = postDominate, inNode = fieldIdentifier)
      .addOutEdge(edge = postDominate, inNode = literal)
      .addOutEdge(edge = postDominate, inNode = methodRef)
      .addOutEdge(edge = postDominate, inNode = typeRef)
      .addOutEdge(edge = postDominate, inNode = ret)
      .addOutEdge(edge = postDominate, inNode = block)
      .addOutEdge(edge = postDominate, inNode = method)
      .addOutEdge(edge = postDominate, inNode = controlStructure)
      .addOutEdge(edge = postDominate, inNode = jumpTarget)
      .addOutEdge(edge = postDominate, inNode = unknown)

    block
      .addProperties(depthFirstOrder, internalFlags)
      .addOutEdge(edge = dynamicType, inNode = typeDecl)
      .addOutEdge(edge = dynamicType, inNode = method)
      .addOutEdge(edge = dominate, inNode = callNode)
      .addOutEdge(edge = dominate, inNode = identifier)
      .addOutEdge(edge = dominate, inNode = fieldIdentifier)
      .addOutEdge(edge = dominate, inNode = literal)
      .addOutEdge(edge = dominate, inNode = methodRef)
      .addOutEdge(edge = dominate, inNode = typeRef)
      .addOutEdge(edge = dominate, inNode = ret)
      .addOutEdge(edge = dominate, inNode = block)
      .addOutEdge(edge = dominate, inNode = methodReturn)
      .addOutEdge(edge = dominate, inNode = controlStructure)
      .addOutEdge(edge = dominate, inNode = jumpTarget)
      .addOutEdge(edge = dominate, inNode = unknown)
      .addOutEdge(edge = postDominate, inNode = callNode)
      .addOutEdge(edge = postDominate, inNode = identifier)
      .addOutEdge(edge = postDominate, inNode = fieldIdentifier)
      .addOutEdge(edge = postDominate, inNode = literal)
      .addOutEdge(edge = postDominate, inNode = methodRef)
      .addOutEdge(edge = postDominate, inNode = typeRef)
      .addOutEdge(edge = postDominate, inNode = ret)
      .addOutEdge(edge = postDominate, inNode = block)
      .addOutEdge(edge = postDominate, inNode = method)
      .addOutEdge(edge = postDominate, inNode = controlStructure)
      .addOutEdge(edge = postDominate, inNode = jumpTarget)
      .addOutEdge(edge = postDominate, inNode = unknown)
      .addOutEdge(edge = cdg, inNode = callNode)
      .addOutEdge(edge = cdg, inNode = identifier)
      .addOutEdge(edge = cdg, inNode = fieldIdentifier)
      .addOutEdge(edge = cdg, inNode = literal)
      .addOutEdge(edge = cdg, inNode = methodRef)
      .addOutEdge(edge = cdg, inNode = typeRef)
      .addOutEdge(edge = cdg, inNode = ret)
      .addOutEdge(edge = cdg, inNode = block)
      .addOutEdge(edge = cdg, inNode = methodReturn)
      .addOutEdge(edge = cdg, inNode = controlStructure)
      .addOutEdge(edge = cdg, inNode = jumpTarget)
      .addOutEdge(edge = cdg, inNode = unknown)

    unknown
      .addProperties(depthFirstOrder, internalFlags)
      .addOutEdge(edge = taggedBy, inNode = tag)
      .addOutEdge(edge = dynamicType, inNode = typeDecl)
      .addOutEdge(edge = dynamicType, inNode = method)
      .addOutEdge(edge = dominate, inNode = callNode)
      .addOutEdge(edge = dominate, inNode = identifier)
      .addOutEdge(edge = dominate, inNode = fieldIdentifier)
      .addOutEdge(edge = dominate, inNode = literal)
      .addOutEdge(edge = dominate, inNode = methodRef)
      .addOutEdge(edge = dominate, inNode = typeRef)
      .addOutEdge(edge = dominate, inNode = ret)
      .addOutEdge(edge = dominate, inNode = block)
      .addOutEdge(edge = dominate, inNode = methodReturn)
      .addOutEdge(edge = dominate, inNode = controlStructure)
      .addOutEdge(edge = dominate, inNode = jumpTarget)
      .addOutEdge(edge = dominate, inNode = unknown)
      .addOutEdge(edge = postDominate, inNode = callNode)
      .addOutEdge(edge = postDominate, inNode = identifier)
      .addOutEdge(edge = postDominate, inNode = fieldIdentifier)
      .addOutEdge(edge = postDominate, inNode = literal)
      .addOutEdge(edge = postDominate, inNode = methodRef)
      .addOutEdge(edge = postDominate, inNode = typeRef)
      .addOutEdge(edge = postDominate, inNode = ret)
      .addOutEdge(edge = postDominate, inNode = block)
      .addOutEdge(edge = postDominate, inNode = method)
      .addOutEdge(edge = postDominate, inNode = controlStructure)
      .addOutEdge(edge = postDominate, inNode = jumpTarget)
      .addOutEdge(edge = postDominate, inNode = unknown)
      .addOutEdge(edge = cdg, inNode = callNode)
      .addOutEdge(edge = cdg, inNode = identifier)
      .addOutEdge(edge = cdg, inNode = fieldIdentifier)
      .addOutEdge(edge = cdg, inNode = literal)
      .addOutEdge(edge = cdg, inNode = methodRef)
      .addOutEdge(edge = cdg, inNode = typeRef)
      .addOutEdge(edge = cdg, inNode = ret)
      .addOutEdge(edge = cdg, inNode = block)
      .addOutEdge(edge = cdg, inNode = methodReturn)
      .addOutEdge(edge = cdg, inNode = controlStructure)
      .addOutEdge(edge = cdg, inNode = jumpTarget)
      .addOutEdge(edge = cdg, inNode = unknown)

    controlStructure
      .addOutEdge(edge = taggedBy, inNode = tag)
      .addOutEdge(edge = dynamicType, inNode = typeDecl)
      .addOutEdge(edge = dynamicType, inNode = method)
      .addOutEdge(edge = dominate, inNode = callNode)
      .addOutEdge(edge = dominate, inNode = identifier)
      .addOutEdge(edge = dominate, inNode = fieldIdentifier)
      .addOutEdge(edge = dominate, inNode = literal)
      .addOutEdge(edge = dominate, inNode = methodRef)
      .addOutEdge(edge = dominate, inNode = typeRef)
      .addOutEdge(edge = dominate, inNode = ret)
      .addOutEdge(edge = dominate, inNode = block)
      .addOutEdge(edge = dominate, inNode = methodReturn)
      .addOutEdge(edge = dominate, inNode = controlStructure)
      .addOutEdge(edge = dominate, inNode = jumpTarget)
      .addOutEdge(edge = dominate, inNode = unknown)
      .addOutEdge(edge = postDominate, inNode = callNode)
      .addOutEdge(edge = postDominate, inNode = identifier)
      .addOutEdge(edge = postDominate, inNode = fieldIdentifier)
      .addOutEdge(edge = postDominate, inNode = literal)
      .addOutEdge(edge = postDominate, inNode = methodRef)
      .addOutEdge(edge = postDominate, inNode = typeRef)
      .addOutEdge(edge = postDominate, inNode = ret)
      .addOutEdge(edge = postDominate, inNode = block)
      .addOutEdge(edge = postDominate, inNode = method)
      .addOutEdge(edge = postDominate, inNode = controlStructure)
      .addOutEdge(edge = postDominate, inNode = jumpTarget)
      .addOutEdge(edge = postDominate, inNode = unknown)
      .addOutEdge(edge = cdg, inNode = callNode)
      .addOutEdge(edge = cdg, inNode = identifier)
      .addOutEdge(edge = cdg, inNode = fieldIdentifier)
      .addOutEdge(edge = cdg, inNode = literal)
      .addOutEdge(edge = cdg, inNode = methodRef)
      .addOutEdge(edge = cdg, inNode = typeRef)
      .addOutEdge(edge = cdg, inNode = ret)
      .addOutEdge(edge = cdg, inNode = block)
      .addOutEdge(edge = cdg, inNode = methodReturn)
      .addOutEdge(edge = cdg, inNode = controlStructure)
      .addOutEdge(edge = cdg, inNode = jumpTarget)
      .addOutEdge(edge = cdg, inNode = unknown)

    methodRef
      .addProperties(depthFirstOrder, internalFlags)
      .addOutEdge(edge = dynamicType, inNode = typeDecl)
      .addOutEdge(edge = dynamicType, inNode = method)
      .addOutEdge(edge = dominate, inNode = callNode)
      .addOutEdge(edge = dominate, inNode = identifier)
      .addOutEdge(edge = dominate, inNode = fieldIdentifier)
      .addOutEdge(edge = dominate, inNode = literal)
      .addOutEdge(edge = dominate, inNode = methodRef)
      .addOutEdge(edge = dominate, inNode = typeRef)
      .addOutEdge(edge = dominate, inNode = ret)
      .addOutEdge(edge = dominate, inNode = block)
      .addOutEdge(edge = dominate, inNode = methodReturn)
      .addOutEdge(edge = dominate, inNode = controlStructure)
      .addOutEdge(edge = dominate, inNode = jumpTarget)
      .addOutEdge(edge = dominate, inNode = unknown)
      .addOutEdge(edge = postDominate, inNode = callNode)
      .addOutEdge(edge = postDominate, inNode = identifier)
      .addOutEdge(edge = postDominate, inNode = fieldIdentifier)
      .addOutEdge(edge = postDominate, inNode = literal)
      .addOutEdge(edge = postDominate, inNode = methodRef)
      .addOutEdge(edge = postDominate, inNode = typeRef)
      .addOutEdge(edge = postDominate, inNode = ret)
      .addOutEdge(edge = postDominate, inNode = block)
      .addOutEdge(edge = postDominate, inNode = method)
      .addOutEdge(edge = postDominate, inNode = controlStructure)
      .addOutEdge(edge = postDominate, inNode = jumpTarget)
      .addOutEdge(edge = postDominate, inNode = unknown)
      .addOutEdge(edge = cdg, inNode = callNode)
      .addOutEdge(edge = cdg, inNode = identifier)
      .addOutEdge(edge = cdg, inNode = fieldIdentifier)
      .addOutEdge(edge = cdg, inNode = literal)
      .addOutEdge(edge = cdg, inNode = methodRef)
      .addOutEdge(edge = cdg, inNode = typeRef)
      .addOutEdge(edge = cdg, inNode = ret)
      .addOutEdge(edge = cdg, inNode = block)
      .addOutEdge(edge = cdg, inNode = methodReturn)
      .addOutEdge(edge = cdg, inNode = controlStructure)
      .addOutEdge(edge = cdg, inNode = jumpTarget)
      .addOutEdge(edge = cdg, inNode = unknown)

    typeRef
      .addProperties(depthFirstOrder, internalFlags)
      .addOutEdge(edge = dynamicType, inNode = typeDecl)
      .addOutEdge(edge = dynamicType, inNode = method)
      .addOutEdge(edge = dominate, inNode = callNode)
      .addOutEdge(edge = dominate, inNode = identifier)
      .addOutEdge(edge = dominate, inNode = fieldIdentifier)
      .addOutEdge(edge = dominate, inNode = literal)
      .addOutEdge(edge = dominate, inNode = methodRef)
      .addOutEdge(edge = dominate, inNode = typeRef)
      .addOutEdge(edge = dominate, inNode = ret)
      .addOutEdge(edge = dominate, inNode = block)
      .addOutEdge(edge = dominate, inNode = methodReturn)
      .addOutEdge(edge = dominate, inNode = controlStructure)
      .addOutEdge(edge = dominate, inNode = jumpTarget)
      .addOutEdge(edge = dominate, inNode = unknown)
      .addOutEdge(edge = postDominate, inNode = callNode)
      .addOutEdge(edge = postDominate, inNode = identifier)
      .addOutEdge(edge = postDominate, inNode = fieldIdentifier)
      .addOutEdge(edge = postDominate, inNode = literal)
      .addOutEdge(edge = postDominate, inNode = methodRef)
      .addOutEdge(edge = postDominate, inNode = typeRef)
      .addOutEdge(edge = postDominate, inNode = ret)
      .addOutEdge(edge = postDominate, inNode = block)
      .addOutEdge(edge = postDominate, inNode = method)
      .addOutEdge(edge = postDominate, inNode = controlStructure)
      .addOutEdge(edge = postDominate, inNode = jumpTarget)
      .addOutEdge(edge = postDominate, inNode = unknown)
      .addOutEdge(edge = cdg, inNode = callNode)
      .addOutEdge(edge = cdg, inNode = identifier)
      .addOutEdge(edge = cdg, inNode = fieldIdentifier)
      .addOutEdge(edge = cdg, inNode = literal)
      .addOutEdge(edge = cdg, inNode = methodRef)
      .addOutEdge(edge = cdg, inNode = typeRef)
      .addOutEdge(edge = cdg, inNode = ret)
      .addOutEdge(edge = cdg, inNode = block)
      .addOutEdge(edge = cdg, inNode = methodReturn)
      .addOutEdge(edge = cdg, inNode = controlStructure)
      .addOutEdge(edge = cdg, inNode = jumpTarget)
      .addOutEdge(edge = cdg, inNode = unknown)

    lazy val detachedTrackingPoint: NodeType = builder
      .addNodeType(
        name = "DETACHED_TRACKING_POINT",
        comment = ""
      )
      .protoId(1001)
      .addProperties()
      .extendz(trackingPoint)
      .addContainedNode(cfgNode, "cfgNode", Cardinality.One)

    controlStructure
      .addProperties(depthFirstOrder, internalFlags)

    implicitCall
      .addProperties(depthFirstOrder, internalFlags)

    postExecutionCall
      .addProperties(depthFirstOrder, internalFlags)

    annotationLiteral
      .addProperties(depthFirstOrder, internalFlags)

    arrayInitializer
      .addProperties(depthFirstOrder, internalFlags)

    // constants
    // deprecated, c.f. CONFIG_FILE
    val frameworks = builder.addConstants(
      category = "Frameworks",
      Constant(name = "PLAY", value = "PLAY", valueType = "String", comment = "Play framework").protoId(1),
      Constant(name = "GWT", value = "GWT", valueType = "String", comment = "Google web toolkit").protoId(2),
      Constant(name = "SPRING", value = "SPRING", valueType = "String", comment = "Java spring framework").protoId(3),
      Constant(name = "VERTX", value = "VERTX", valueType = "String", comment = "Polyglot event-driven framework")
        .protoId(4),
      Constant(name = "JSF", value = "JSF", valueType = "String", comment = "JavaServer Faces").protoId(5),
      Constant(name = "SERVLET", value = "SERVLET", valueType = "String", comment = "Java Servlet based frameworks")
        .protoId(6),
      Constant(name = "JAXRS", value = "JAXRS", valueType = "String", comment = "JAX-RS").protoId(7),
      Constant(name = "SPARK", value = "SPARK", valueType = "String", comment = "Spark micro web framework").protoId(8),
      Constant(name = "ASP_NET_CORE", value = "ASP_NET_CORE", valueType = "String", comment = "Microsoft ASP.NET Core")
        .protoId(9),
      Constant(
        name = "ASP_NET_WEB_API",
        value = "ASP_NET_WEB_API",
        valueType = "String",
        comment = "Microsoft ASP.NET Web API"
      ).protoId(10),
      Constant(name = "ASP_NET_MVC", value = "ASP_NET_MVC", valueType = "String", comment = "Microsoft ASP.NET MVC")
        .protoId(11),
      Constant(name = "JAXWS", value = "JAXWS", valueType = "String", comment = "JAX-WS").protoId(12),
      Constant(
        name = "ASP_NET_WEB_UI",
        value = "ASP_NET_WEB_UI",
        valueType = "String",
        comment = "Microsoft ASP.NET Web UI"
      ).protoId(13),
      Constant(
        name = "JAVA_INTERNAL",
        value = "JAVA_INTERNAL",
        valueType = "String",
        comment = "Framework facilities directly provided by Java"
      ).protoId(14),
      Constant(name = "DROPWIZARD", value = "DROPWIZARD", valueType = "String", comment = "Dropwizard framework")
        .protoId(15),
      Constant(name = "WCF", value = "WCF", valueType = "String", comment = "WCF HTTP and REST").protoId(16)
    )

  }

}
