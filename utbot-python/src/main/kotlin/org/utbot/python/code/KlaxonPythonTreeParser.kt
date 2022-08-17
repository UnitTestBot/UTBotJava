package org.utbot.python.code

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import org.utbot.framework.plugin.api.PythonClassId
import org.utbot.framework.plugin.api.PythonTree

object KlaxonPythonTreeParser {
    fun parseJsonToPythonTree(jsonString: String): PythonTree.PythonTreeNode {
        val json = parseJsonString(jsonString)
        return parseToPythonTree(json)
    }

    private fun parseJsonString(jsonString: String): JsonObject {
        val parser: Parser = Parser.default()
        val stringBuilder: StringBuilder = StringBuilder(jsonString)
        return parser.parse(stringBuilder) as JsonObject
    }

    private fun parseToPythonTree(json: JsonObject): PythonTree.PythonTreeNode {
        val type = json.string("type")!!
        val strategy = json.string("strategy")!!
        val comparable = json.boolean("comparable")!!

        val result = if (strategy == "repr") {
            var repr = json.string("value")!!
            if (type == "builtins.complex") {
                repr = "complex('$repr')"
            } else if (repr == "nan") {
                repr = "float('$repr')"
            } else if (repr == "inf") {
                repr = "float('$repr')"
            }
            PythonTree.PrimitiveNode(PythonClassId(type), repr)
        } else {
            when (type) {
                "builtins.list" -> parsePythonList(json.array("value")!!)
                "builtins.set" -> parsePythonSet(json.array("value")!!)
                "builtins.tuple" -> parsePythonTuple(json.array("value")!!)
                "builtins.dict" -> parsePythonDict(json.array("value")!!)
                else -> parseReduce(type, json.obj("value")!!)
            }
        }
        result.comparable = comparable
        return result
    }

    private fun parseReduce(type: String, value: JsonObject): PythonTree.PythonTreeNode {
        return PythonTree.ReduceNode(
            PythonClassId(type),
            value.string("constructor")!!,
            parsePythonList(value.array("args")!!).items,
            parsePythonDict(value.array("state")!!).items.map {
                (it.key as PythonTree.PrimitiveNode).repr to it.value
            }.toMap(),
            parsePythonList(value.array("listitems")!!).items,
            parsePythonDict(value.array("dictitems")!!).items,
        )
    }

    private fun parsePythonList(items: JsonArray<JsonObject>): PythonTree.ListNode {
        return PythonTree.ListNode(items.map { parseToPythonTree(it) })
    }

    private fun parsePythonSet(items: JsonArray<JsonObject>): PythonTree.SetNode {
        return PythonTree.SetNode(items.map { parseToPythonTree(it) }.toSet())
    }

    private fun parsePythonTuple(items: JsonArray<JsonObject>): PythonTree.TupleNode {
        return PythonTree.TupleNode(items.map { parseToPythonTree(it) })
    }

    private fun parsePythonDict(items: JsonArray<JsonArray<Any>>): PythonTree.DictNode {
        return PythonTree.DictNode(items.associate {
            val key = it[0]
            val value = it[1] as JsonObject
            (if (key is String) PythonTree.PrimitiveNode(PythonClassId("builtins.str"), key) else parseToPythonTree(key as JsonObject)) to parseToPythonTree(value)
        })
    }
}