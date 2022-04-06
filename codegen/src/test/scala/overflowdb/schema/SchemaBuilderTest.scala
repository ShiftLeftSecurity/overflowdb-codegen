package overflowdb.schema

import org.scalatest.wordspec.AnyWordSpec
import Property.ValueType

class SchemaBuilderTest extends AnyWordSpec {

  "proto ids must be unique within one category" in {

    val schemaModifications: Seq[(String, SchemaBuilder => Any)] = Seq(
      ("node", _.addNodeType("testNode").protoId(10)),
      ("edge", _.addEdgeType("testEdge").protoId(10)),
      ("category1", _.addConstants("category1", Constant("constant1", "value1", ValueType.String).protoId(10))),
      ("category2", _.addConstants("category2", Constant("constant2", "value2", ValueType.String).protoId(10))),
      ("node property", { schemaBuilder =>
        val property = schemaBuilder.addProperty("prop", ValueType.String).protoId(10)
        schemaBuilder.addNodeType("testNode").addProperty(property)
      }),
      ("edge property", { schemaBuilder =>
        val property = schemaBuilder.addProperty("prop", ValueType.String).protoId(11)
        schemaBuilder.addEdgeType("testEdge").addProperty(property)
      }),
    )

    /* all combinations of any two schema modifications */
    for {
      (case1, modification1) <- schemaModifications
      (case2, modification2) <- schemaModifications
    } {
      val builder = new SchemaBuilder("test", "testpkg")
      modification1(builder)
      modification2(builder)

      if (case1 == case2) {
        /** when using the same protoId within the same category should lead to an AssertionError
         * during `schemaBuilder.build`, since all of these use the same protoId */
        withClue(s"adding two $case1 schema elements with identical protoId:") {
          assertThrows[AssertionError](builder.build)
        }
      } else {
        /** using the same protoId in different categories is fine */
        builder.build
      }
    }
  }

}
