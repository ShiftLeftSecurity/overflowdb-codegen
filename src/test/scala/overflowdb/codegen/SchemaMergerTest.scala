package overflowdb.codegen

import org.scalatest._
import ujson._

class SchemaMergerTest extends WordSpec with Matchers {

  "adds independent elements" in {
    val jsonA = """{"nodeTypes": [{"name":"foo", "field1":"value1"}]}"""
    val jsonB = """{"edgeTypes": [{"name":"bar", "field2":"value2"}]}"""

    val result = merge(jsonA, jsonB)
    result shouldBe read(
      """{
      "nodeTypes": [{"name":"foo", "field1":"value1"}],
      "edgeTypes": [{"name":"bar", "field2":"value2"}]
      }""")
  }

  "joins separate elements from same collection" in {
    val jsonA = """{"nodeTypes": [{"name":"foo", "field1":"value1"}]}"""
    val jsonB = """{"nodeTypes": [{"name":"bar", "field2":"value2"}]}"""

    val mergedNodeTypes = merge(jsonA, jsonB)("nodeTypes").arr
    mergedNodeTypes should contain(read(""" {"name":"foo", "field1":"value1"} """))
    mergedNodeTypes should contain(read(""" {"name":"bar", "field2":"value2"} """))
  }

  "combines elements with same `name`" in {
    val jsonA = """{"nodeTypes": [{"name":"foo", "field1": "value1", "listField": ["one", "two"] }]}"""
    val jsonB = """{"nodeTypes": [{"name":"foo", "field2": "value2", "listField": ["three"] }]}"""

    val result = merge(jsonA, jsonB)
    result shouldBe read(
      """{
      "nodeTypes": [{"name":"foo", "field1": "value1", "field2": "value2", "listField": ["one", "two", "three"]}]
      }""")
  }

  "combines and deduplicates outEdges" when {
    "not using cardinalities" in {
      val jsonA =
        """{"nodeTypes": [ { "name": "TYPE_DECL", "outEdges": [
            { "edgeName": "AST","inNodes": [{"name": "ANNOTATION"}] },
            { "edgeName": "ALIAS_OF","inNodes": [{"name": "TYPE"}] } ]
       }]}"""
      val jsonB =
        """{"nodeTypes": [ { "name": "TYPE_DECL", "outEdges": [
            { "edgeName": "AST", "inNodes": [
              {"name": "TYPE_DECL"},
              {"name": "METHOD"}
            ]}
           ]}]}"""

      val result = merge(jsonA, jsonB)
      result shouldBe read(
        """{"nodeTypes": [ { "name": "TYPE_DECL", "outEdges": [
            { "edgeName": "AST","inNodes": [
              {"name": "ANNOTATION"},
              {"name": "TYPE_DECL"},
              {"name": "METHOD"}
            ] },
            { "edgeName": "ALIAS_OF","inNodes": [{"name": "TYPE"}] } ,
            { "edgeName": "CONTAINS_NODE","inNodes":[{"name": "NODE"}]}
           ]}]}""")
    }
  }

  "errors if same element has property defined multiple times with different values" in {
    val jsonA = """{"nodeKeys": [{"name":"foo", "field1": "value1"}]}"""
    val jsonB = """{"nodeKeys": [{"name":"foo", "field1": "value2"}]}"""

    intercept[AssertionError] {
      merge(jsonA, jsonB)
    }
  }

  "errors if collection contains element with duplicate IDs" in {
    val jsonA = """{"nodeKeys": [{"name":"foo", "id":1}]}"""
    val jsonB = """{"nodeKeys": [{"name":"bar", "id":1}]}"""

    intercept[AssertionError] {
      merge(jsonA, jsonB)
    }
  }

  "for any node, automatically add a generic `CONTAINS_NODE` outEdge" in {
    val jsonA =
      """{ "nodeTypes": [
           {
             "name":"CALL_SITE",
             "outEdges": [
               { "edgeName": "SOME_EDGE", "inNodes": [{"name": "SOME_OTHER_NODE"} ] }
             ]
           }
         ]}"""
    val jsonB = "{ }"

    merge(jsonA, jsonB) shouldBe read(
      """{ "nodeTypes": [
           {
             "name":"CALL_SITE",
             "outEdges": [
               { "edgeName": "SOME_EDGE", "inNodes": [ {"name": "SOME_OTHER_NODE"} ] },
               { "edgeName": "CONTAINS_NODE", "inNodes": [{ "name": "NODE" }] }
             ]
           }
         ]}""")
  }


  "for any node that has `containedNode` entries, automatically add the corresponding `outEdges`" in {
    val jsonA =
      """{ "nodeTypes": [
           {
             "name":"CALL_SITE",
             "containedNodes": [{ "nodeType": "METHOD" }, { "nodeType": "CALL" } ],
             "outEdges": [
               { "edgeName": "SOME_EDGE", "inNodes": [ {"name": "SOME_OTHER_NODE"} ] }
             ]
           }
         ]}"""
    val jsonB = "{}"

    merge(jsonA, jsonB) shouldBe read(
      """{ "nodeTypes": [
           {
             "name":"CALL_SITE",
             "containedNodes": [{ "nodeType": "METHOD" }, { "nodeType": "CALL" } ],
             "outEdges": [
               { "edgeName": "SOME_EDGE", "inNodes": [ {"name": "SOME_OTHER_NODE" }] },
               { "edgeName": "CONTAINS_NODE", "inNodes": [
                 {"name": "NODE"},
                 {"name": "METHOD"},
                 {"name": "CALL"}
               ] }
             ]
           }
         ]}""")
  }

  def merge(jsonA: String, jsonB: String) =
    SchemaMerger.mergeCollections(
      Seq(jsonA, jsonB).map(json => Obj(read(json).obj))
    )
}
