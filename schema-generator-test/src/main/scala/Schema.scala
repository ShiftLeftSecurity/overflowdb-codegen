package org.test.schema

import overflowdb.schema.{Schema, SchemaBuilder}
import overflowdb.schema.EdgeType.Cardinality
import overflowdb.schema.Property.ValueType

object GratefuldeadSchema {
  val builder = new SchemaBuilder(
      domainShortName = "gratefuldead",
      basePackage = "org.test"
  )

  /* <properties start> */
  val nameOnSongNode = builder.addProperty(name = "name", valueType = ValueType.String, comment = "")
val performances = builder.addProperty(name = "performances", valueType = ValueType.Int, comment = "")
val songType = builder.addProperty(name = "songType", valueType = ValueType.String, comment = "")
val nameOnArtistNode = builder.addProperty(name = "name", valueType = ValueType.String, comment = "")
val weight = builder.addProperty(name = "weight", valueType = ValueType.Int, comment = "")
  /* <properties end> */

  /* <nodes start> */
  val song = builder.addNodeType(name = "song", comment = "").addProperties(nameOnSongNode, performances, songType)

val artist = builder.addNodeType(name = "artist", comment = "").addProperties(nameOnArtistNode)
  /* <nodes end> */

  /* <edges start> */
  val followedBy = builder.addEdgeType(name = "followedBy", comment = "").addProperties(weight)

val sungBy = builder.addEdgeType(name = "sungBy", comment = "")

val writtenBy = builder.addEdgeType(name = "writtenBy", comment = "")
  /* <edges end> */

  /* <relationships start> */
  song.addOutEdge(edge = followedBy, inNode = song, cardinalityOut = Cardinality.List, cardinalityIn = Cardinality.List, stepNameOut = "", stepNameIn = "")

song.addOutEdge(edge = sungBy, inNode = artist, cardinalityOut = Cardinality.List, cardinalityIn = Cardinality.List, stepNameOut = "", stepNameIn = "")

song.addOutEdge(edge = writtenBy, inNode = artist, cardinalityOut = Cardinality.List, cardinalityIn = Cardinality.List, stepNameOut = "", stepNameIn = "")
  /* <relationships end> */

  val instance: Schema = builder.build()
}

