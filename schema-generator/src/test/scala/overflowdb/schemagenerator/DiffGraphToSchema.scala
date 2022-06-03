package overflowdb.schemagenerator

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import overflowdb.BatchedUpdate._

import scala.jdk.CollectionConverters.IterableHasAsJava

class DiffGraphToSchemaTest extends AnyWordSpec with Matchers {
  val domainName = "SampleDomain"
  val schemaPackage = "odb.sample.schema"
  val targetPackage = "odb.sample"
  val builder = new DiffGraphToSchema(domainName = domainName, schemaPackage = schemaPackage, targetPackage = targetPackage)

  "simple schema" in {
    val diffGraph = new DiffGraphBuilder()
    val artist = diffGraph.addAndReturnNode("Artist", "fullName", "Bob Dylan")
    val song   = diffGraph.addAndReturnNode("Song", "name", "The times they are a changin'")
    diffGraph.addEdge(artist, song, "sung", "edgeProperty", "someValue")
    diffGraph.addNode("Artist") // some other artist - should not affect any of the below...

    val result = builder.asSourceString(diffGraph.build())
    result should startWith(s"package $schemaPackage")
    result should include(s"""val fullName = builder.addProperty(name = "fullName", valueType = ValueType.String, comment = "")""")
    result should include(s"""val name = builder.addProperty(name = "name", valueType = ValueType.String, comment = "")""")
    result should include(s"""val edgeProperty = builder.addProperty(name = "edgeProperty", valueType = ValueType.String, comment = "")""")
    result should include("""val artist = builder.addNodeType(name = "Artist", comment = "").addProperties(fullName)""")
    result should include("""val song = builder.addNodeType(name = "Song", comment = "").addProperties(name)""")
    result should include("""val sung = builder.addEdgeType(name = "sung", comment = "").addProperties(edgeProperty)""")
    result should include("""artist.addOutEdge(edge = sung, inNode = song, cardinalityOut = Cardinality.List, cardinalityIn = Cardinality.List, stepNameOut = "", stepNameIn = "")""")
  }

  "camel case for given label/property names" in {
    val diffGraph = new DiffGraphBuilder()

    val artist = diffGraph.addAndReturnNode("SINGER_SONGWRITER", "FULL_NAME", "Bob Dylan")
    val song   = diffGraph.addAndReturnNode("SONG", "name", "The times they are a changin'")
    diffGraph.addEdge(song, artist, "SUNG_BY", "EDGE_PROPERTY", "someValue")

    val result = builder.asSourceString(diffGraph.build())
    result should include(s"""val name = builder.addProperty(name = "name", valueType = ValueType.String, comment = "")""")
    result should include(s"""val fullName = builder.addProperty(name = "FULL_NAME", valueType = ValueType.String, comment = "")""")
    result should include(s"""val edgeProperty = builder.addProperty(name = "EDGE_PROPERTY", valueType = ValueType.String, comment = "")""")
    result should include("""val singerSongwriter = builder.addNodeType(name = "SINGER_SONGWRITER", comment = "").addProperties(fullName)""")
    result should include("""val song = builder.addNodeType(name = "SONG", comment = "").addProperties(name)""")
    result should include("""val sungBy = builder.addEdgeType(name = "SUNG_BY", comment = "").addProperties(edgeProperty)""")
    result should include("""song.addOutEdge(edge = sungBy, inNode = singerSongwriter, cardinalityOut = Cardinality.List, cardinalityIn = Cardinality.List, stepNameOut = "", stepNameIn = "")""")
  }

  "testing all property value types" in {
    val diffGraph = new DiffGraphBuilder()
      .addNode("Thing",
        "stringProp", "stringValue",
        "boolProp1", true,
        "boolProp2", true: java.lang.Boolean,
        "byteProp1", 1: Byte,
        "byteProp2", java.lang.Byte.valueOf(2: Byte),
        "shortProp1", 3: Short,
        "shortProp2", java.lang.Short.valueOf(4: Short),
        "intProp1", 5: Int,
        "intProp2", 6: Integer,
        "longProp1", 7: Long,
        "longProp2", 8: java.lang.Long,
        "floatProp1", 9.1f: Float,
        "floatProp2", 10.1f: java.lang.Float,
        "doubleProp1", 11.1f: Double,
        "doubleProp2", 12.1f: java.lang.Double,
        "charProp1", 'a': Char,
        "charProp2", 'b': Character,
        "listProp1", Array("string1", "string2"),
        "listProp2", Seq("string1", "string2"),
        "listProp3", Seq("string1", "string2").asJava,
      )

    val result = builder.asSourceString(diffGraph.build())
    result should include(s"""val stringProp = builder.addProperty(name = "stringProp", valueType = ValueType.String, comment = "")""")
    result should include(s"""val boolProp1 = builder.addProperty(name = "boolProp1", valueType = ValueType.Boolean, comment = "")""")
    result should include(s"""val boolProp2 = builder.addProperty(name = "boolProp2", valueType = ValueType.Boolean, comment = "")""")
    result should include(s"""val byteProp1 = builder.addProperty(name = "byteProp1", valueType = ValueType.Byte, comment = "")""")
    result should include(s"""val byteProp2 = builder.addProperty(name = "byteProp2", valueType = ValueType.Byte, comment = "")""")
    result should include(s"""val shortProp1 = builder.addProperty(name = "shortProp1", valueType = ValueType.Short, comment = "")""")
    result should include(s"""val shortProp2 = builder.addProperty(name = "shortProp2", valueType = ValueType.Short, comment = "")""")
    result should include(s"""val intProp1 = builder.addProperty(name = "intProp1", valueType = ValueType.Int, comment = "")""")
    result should include(s"""val intProp2 = builder.addProperty(name = "intProp2", valueType = ValueType.Int, comment = "")""")
    result should include(s"""val longProp1 = builder.addProperty(name = "longProp1", valueType = ValueType.Long, comment = "")""")
    result should include(s"""val longProp2 = builder.addProperty(name = "longProp2", valueType = ValueType.Long, comment = "")""")
    result should include(s"""val floatProp1 = builder.addProperty(name = "floatProp1", valueType = ValueType.Float, comment = "")""")
    result should include(s"""val floatProp2 = builder.addProperty(name = "floatProp2", valueType = ValueType.Float, comment = "")""")
    result should include(s"""val doubleProp1 = builder.addProperty(name = "doubleProp1", valueType = ValueType.Double, comment = "")""")
    result should include(s"""val doubleProp2 = builder.addProperty(name = "doubleProp2", valueType = ValueType.Double, comment = "")""")
    result should include(s"""val charProp1 = builder.addProperty(name = "charProp1", valueType = ValueType.Char, comment = "")""")
    result should include(s"""val charProp2 = builder.addProperty(name = "charProp2", valueType = ValueType.Char, comment = "")""")
    result should include(s"""val listProp1 = builder.addProperty(name = "listProp1", valueType = ValueType.String, comment = "").asList()""")
    result should include(s"""val listProp2 = builder.addProperty(name = "listProp2", valueType = ValueType.String, comment = "").asList()""")
    result should include(s"""val listProp3 = builder.addProperty(name = "listProp3", valueType = ValueType.String, comment = "").asList()""")
    result should include("""val thing = builder.addNodeType(name = "Thing", comment = "").addProperties(boolProp1, boolProp2, """ +
      """byteProp1, byteProp2, charProp1, charProp2, doubleProp1, doubleProp2, floatProp1, floatProp2, intProp1, intProp2, """ +
      """listProp1, listProp2, listProp3, longProp1, longProp2, shortProp1, shortProp2, stringProp)""")
  }

  "schema with ambiguities in property names" in {
    val diffGraph = new DiffGraphBuilder()
    // using property with same name on different node and edge types
    val artist = diffGraph.addAndReturnNode("Artist", "name", "Bob Dylan", "property1", "value1")
    val song   = diffGraph.addAndReturnNode("Song", "property1", 2f)
    diffGraph.addEdge(artist, song, "sung", "property1", true)

    val result = builder.asSourceString(diffGraph.build())
    result should include(s"""val artistNodeProperty1 = builder.addProperty(name = "property1", valueType = ValueType.String, comment = "")""")
    result should include(s"""val songNodeProperty1 = builder.addProperty(name = "property1", valueType = ValueType.Float, comment = "")""")
    result should include(s"""val sungEdgeProperty1 = builder.addProperty(name = "property1", valueType = ValueType.Boolean, comment = "")""")
    result should include("""val artist = builder.addNodeType(name = "Artist", comment = "").addProperties(name, artistNodeProperty1)""")
    result should include("""val song = builder.addNodeType(name = "Song", comment = "").addProperties(songNodeProperty1)""")
    result should include("""val sung = builder.addEdgeType(name = "sung", comment = "").addProperties(sungEdgeProperty1)""")
  }

  "schema with ambiguities in element names" in {
    val diffGraph = new DiffGraphBuilder()
    val node1 = diffGraph.addAndReturnNode("Thing")
    val node2 = diffGraph.addAndReturnNode("DuplicateThing")
    diffGraph.addEdge(node1, node2, "DuplicateThing")

    val result = builder.asSourceString(diffGraph.build())
    result should include("""val thing = builder.addNodeType(name = "Thing", comment = "")""")
    result should include("""val duplicateThingNode = builder.addNodeType(name = "DuplicateThing", comment = "")""")
    result should include("""val duplicateThingEdge = builder.addEdgeType(name = "DuplicateThing", comment = "")""")
    result should include("""thing.addOutEdge(edge = duplicateThingEdge, inNode = duplicateThingNode, cardinalityOut = Cardinality.List, cardinalityIn = Cardinality.List, stepNameOut = "", stepNameIn = "")""")
  }

  "schema with ambiguities in node and property names" in {
    val diffGraph = new DiffGraphBuilder()
    val node1 = diffGraph.addAndReturnNode("Thing", "DuplicateThing", "someValue")
    val node2 = diffGraph.addAndReturnNode("DuplicateThing")
    diffGraph.addEdge(node1, node2, "Relation")

    val result = builder.asSourceString(diffGraph.build())
    result should include("""val duplicateThingProperty = builder.addProperty(name = "DuplicateThing", valueType = ValueType.String, comment = "")""")
    result should include("""val Thing = builder.addNodeType(name = "Thing", comment = "").addProperties(duplicateThingProperty)""")
    result should include("""val duplicateThingNode = builder.addNodeType(name = "DuplicateThing", comment = "")""")
    result should include("""val relation = builder.addEdgeType(name = "Relation", comment = "")""")
    result should include("""thing.addOutEdge(edge = relation, inNode = duplicateThingNode, cardinalityOut = Cardinality.List, cardinalityIn = Cardinality.List, stepNameOut = "", stepNameIn = "")""")
  }

  "schema with ambiguities in edge and property names" in {
    val diffGraph = new DiffGraphBuilder()
    val node1 = diffGraph.addAndReturnNode("Thing1")
    val node2 = diffGraph.addAndReturnNode("Thing2")
    diffGraph.addEdge(node1, node2, "DuplicateThing", "DuplicateThing", "someValue")

    val result = builder.asSourceString(diffGraph.build())
    result should include("""val duplicateThingProperty = builder.addProperty(name = "DuplicateThing", valueType = ValueType.String, comment = "")""")
    result should include("""val thing1 = builder.addNodeType(name = "Thing1", comment = "")""")
    result should include("""val thing2 = builder.addNodeType(name = "Thing2", comment = "")""")
    result should include("""val duplicateThingEdge = builder.addEdgeType(name = "DuplicateThing", comment = "")""")
    result should include("""thing1.addOutEdge(edge = duplicateThingEdge, inNode = thing2, cardinalityOut = Cardinality.List, cardinalityIn = Cardinality.List, stepNameOut = "", stepNameIn = "")""")
  }

}
