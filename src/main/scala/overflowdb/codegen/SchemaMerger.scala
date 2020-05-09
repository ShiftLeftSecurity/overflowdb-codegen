package overflowdb.codegen

import java.io.File
import scala.collection.mutable
import ujson._

object SchemaMerger {
  object FieldNames {
    /* we're merging elements based on the name field */
    val Name = "name"

    /* ids must not have duplicates within a given collection */
    val Id = "id"

    val NodeTypes = "nodeTypes"
  }

  def mergeCollections(inputFiles: Seq[File]): File = {
    import better.files.FileExtensions

    val inputJsons = inputFiles.sorted.map { file =>
      val jsonString = file.toScala.lines.filterNot(isComment).mkString("\n")
      Obj(ujson.read(jsonString).obj)
    }
    val mergedJson = mergeCollections(inputJsons)
    better.files.File.newTemporaryFile(getClass.getSimpleName).write(
      write(mergedJson)
    ).toJava
  }

  /* merge the first level elements, assuming they are all named lists */
  def mergeCollections(inputJsons: Seq[Obj]): Obj = {
    val result = mutable.LinkedHashMap.empty[String, Value]
    inputJsons.foreach { json =>
      json.value.foreach { case (name, value) =>
        result.get(name) match {
          case None =>
            // first time we encounter this element, simply add it to the result
            result.put(name, value)
          case Some(oldValue) =>
            // we've seen this element before, merge the old and new ones
            result.put(name, mergeLists(oldValue.arr, value.arr))
        }
      }
    }

    verifyNoDuplicateIds(result.iterator)
    result.get(FieldNames.NodeTypes).map(_.arr).foreach(mergeOutEdgeLists)
    withMissingContainsEdges(Obj(result))
  }

  /* for any node that has `containedNode` entries, automatically add the corresponding `outEdges`
  * n.b. not strictly a `merge` feature, but closely related, and was in mergeSchemas.py before */
  private def withMissingContainsEdges(json: Obj): Obj = {
    val result = ujson.copy(json).obj
    result("nodeTypes").arr.map(_.obj).foreach { nodeType =>
      nodeType.get("outEdges").map(_.arr).foreach { outEdges =>
        val outEdgeNames = outEdges.map(_.obj("edgeName").str).toSet

        if (!outEdgeNames.contains("CONTAINS_NODE")) {
          val containsNodeEntry = read(""" { "edgeName": "CONTAINS_NODE", "inNodes": [{"name": "NODE"}] }""")
          outEdges.append(containsNodeEntry)
        }

        val requiredInNodesForContains = nodeType.get("containedNodes").map(_.arr).getOrElse(Nil).map { containedNode =>
          InNode(name = containedNode.obj("nodeType").str, cardinality = None)
        }

        /* replace entry with `edge["edgeName"] == "CONTAINS_NODE"` if it exists, or add one if it doesn't.
         * to do that, convert outEdges to Map<EdgeName, OutEdge> and back at the end */
        val inNodesByOutEdgeName: Map[String, Seq[InNode]] = outEdges.map { edge =>
          edge.obj("edgeName").str -> edge.obj("inNodes").arr.map(parseInNode)
        }.toMap
        val containsInNodesBefore = inNodesByOutEdgeName.getOrElse("CONTAINS_NODE", Seq.empty)
        val containsInNodes = (containsInNodesBefore ++ requiredInNodesForContains).distinct

        outEdges.clear
        inNodesByOutEdgeName.+("CONTAINS_NODE" -> containsInNodes).foreach { case (edgeName, inNodes) =>
          val inNodesJson = mergeByName(inNodes).map {
            case InNode(name, None) =>
              Obj("name" -> name)
            case InNode(name, Some(cardinality)) =>
              Obj("name" -> name, "cardinality" -> cardinality)
          }
          outEdges.append(Obj(
            "edgeName" -> edgeName,
            "inNodes" -> inNodesJson
          ))
        }
      }

    }
    result
  }

  private def mergeByName(inNodes: Seq[InNode]): Seq[InNode] = {
    inNodes.groupBy(_.name).toList.map {
      case (name, List(inNode)) =>
        // inNode with this name appeared only once, just take it as is
        inNode
      case (name, inNodes) =>
        // multiple inNodes with the same name, merge cardinalities
        InNode(name, cardinality = mergeCardinalities(inNodes.map(_.cardinality).toList, name))
    }
  }

  private def mergeCardinalities(cardinalities: List[Option[String]], inNodeName: String): Option[String] =
    cardinalities.distinct match {
      case Nil => None
      case List(cardinality) => cardinality
      case cardinalities if cardinalities.forall(c => c == None || c == Some("n:n")) =>
        // None and Some("n:n") are equivalent
        Some("n:n")
      case cardinalities =>
        throw new NotImplementedError(s"different cardinalities defined for inNodeName=$inNodeName: $cardinalities. That's not (yet) supported.")
    }

  private def mergeLists(oldValues: mutable.ArrayBuffer[Value], newValues: mutable.ArrayBuffer[Value]): Arr = {
    val combined = (oldValues ++ newValues).map(_.obj)
    val byName = combined.groupBy(_(FieldNames.Name))
    val combinedElements = byName.map { case (elementName, keyValues) =>
      val combinedElement = mutable.LinkedHashMap.empty[String, Value]
      keyValues.foreach(_.foreach { case (name, value) =>
        val combinedValue = combine(elementName.str, name, combinedElement.get(name), value)
        combinedElement.put(name, combinedValue)
      })
      combinedElement
    }
    Arr.from(combinedElements.map(Obj.from))
  }

  private def combine(elementName: String, name: String, oldValue: Option[Value], newValue: Value): Value =
    oldValue match {
      case None =>
        // field wasn't set before, take the new value
        newValue
      case Some(oldValue) if oldValue == newValue =>
        // field was set before, but values are identical
        newValue
      case Some(Arr(oldValues)) =>
        // field was set before, but since it's a list we can just join the new list
        oldValues ++ newValue.arr
      case Some(oldValue) =>
        throw new AssertionError(s"$elementName cannot be merged, because it defines the property " +
          s"$name multiple times with different values: $oldValue, $newValue")
    }

  private def verifyNoDuplicateIds(collections: Iterator[(String, Value)]) =
    collections.foreach { case (collectionName, entries) =>
      val idValues = entries.arr.map(_.obj.get(FieldNames.Id)).flatten
      if (idValues.size != idValues.distinct.size) {
        // there are duplicate ids!
        val duplicateIds = idValues.groupBy(id => id).collect {
          case (id, ids) if ids.size > 1 => id
        }
        throw new AssertionError(s"duplicate ids for collection `$collectionName`: ${duplicateIds.mkString(",")}")
      }
    }

  /** outEdges may contain duplicate entries - merge them by the `edgeName`. e.g.
   * [{ "edgeName": "AST","inNodes": ["ANNOTATION"] },
   *  { "edgeName": "ALIAS_OF","inNodes": ["TYPE"] },
   *  { "edgeName": "AST","inNodes": ["TYPE_DECL","METHOD"] }]
   *  will be combined to
   * [{ "edgeName": "AST","inNodes": ["ANNOTATION","TYPE_DECL","METHOD"] },
   *  { "edgeName": "ALIAS_OF","inNodes": ["TYPE"] }]
   * */
  private def mergeOutEdgeLists(nodeTypes: mutable.ArrayBuffer[Value]): Unit =
    nodeTypes.foreach { nodeType =>
      val outEdgesByName = mutable.Map.empty[String, Obj]
      nodeType.obj.get("outEdges").map { outEdges =>
        outEdges.arr.map(_.obj).foreach { outEdge =>
          val edgeName = outEdge("edgeName").str
          outEdgesByName.get(edgeName) match {
            case None =>
              // first time we encounter this outEdge, simply add it to the result
              outEdgesByName.put(edgeName, outEdge)
            case Some(oldValue) =>
              // we've seen this outEdge before, merge the old and new ones
              outEdgesByName.put(edgeName, mergeOutEdge(oldValue.value, outEdge))
          }
        }
      }
      if (outEdgesByName.values.nonEmpty) {
        nodeType.obj.put("outEdges", outEdgesByName.values)
      }
    }

  private def mergeOutEdge(oldValue: mutable.LinkedHashMap[String, Value], newValue: mutable.LinkedHashMap[String, Value]): Obj = {
    newValue.foreach { case (name, value) =>
      val combinedValue = combine("outEdges", name, oldValue.get(name), value)
      oldValue.put(name, combinedValue)
    }
    oldValue
  }

  private def parseInNode(inNodeJson: Value) = {
    val inNodeObj = inNodeJson.obj
    InNode(inNodeObj("name").str, inNodeObj.get("cardinality").map(_.str))
  }

  private def isComment(line: String): Boolean =
    line.trim.startsWith("//")
}


