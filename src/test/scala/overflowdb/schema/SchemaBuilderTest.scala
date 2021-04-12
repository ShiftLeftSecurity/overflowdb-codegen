package overflowdb.schema

import org.scalatest.wordspec.AnyWordSpec
import overflowdb.storage.ValueTypes

class SchemaBuilderTest extends AnyWordSpec {

  "proto ids must be unique" in {
    val schemaModifications: Seq[(String, SchemaBuilder => Any)] = Seq(
      ("nodeProperty", _.addNodeProperty(name = "prop", valueType = ValueTypes.STRING, cardinality = Cardinality.One).protoId(10)),
      ("edgeProperty", _.addEdgeProperty(name = "prop2", valueType = ValueTypes.STRING, cardinality = Cardinality.One).protoId(10)),
      ("node", _.addNodeType("testNode").protoId(10)),
      ("edge", _.addEdgeType("testEdge").protoId(10)),
      ("category", _.addConstants("category1", Constant("constant1", "value1", ValueTypes.STRING).protoId(10))),
      ("category", _.addConstants("category2", Constant("constant2", "value2", ValueTypes.STRING).protoId(10))),
    )

    /** combining any two of these schema modifications should lead to an AssertionError during `schemaBuilder.build`,
     * since all of these use the same protoId */
    for {
      (case1, modification1) <- schemaModifications
      (case2, modification2) <- schemaModifications
    } {
      val builder = new SchemaBuilder("test")
      modification1(builder)
      modification2(builder)
      withClue(s"combining $case1 and $case2:") {
        assertThrows[AssertionError](builder.build)
      }
    }
  }

}
