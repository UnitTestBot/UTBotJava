package org.utbot.python.code

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTree

class KlaxonPythonTreeParser(
    jsonString: String
) {
    private val jsonObject = parseJsonString(jsonString)

    private val rawMemory = jsonObject.obj("memory")!!.map {
        it.key.toLong() to it.value as JsonObject
    }.toMap()

    private val memory = emptyMap<Long, PythonTree.PythonTreeNode>().toMutableMap()

    fun parseJsonToPythonTree(): PythonTree.PythonTreeNode {
        return parseToPythonTree(jsonObject.obj("json")!!)
    }

    private fun parseJsonString(jsonString: String): JsonObject {
        val parser: Parser = Parser.default()
        val stringBuilder: StringBuilder = StringBuilder(jsonString)
        return parser.parse(stringBuilder) as JsonObject
    }

    private fun findInMemory(id: Long): PythonTree.PythonTreeNode {
        return if (memory.containsKey(id))
            memory[id]!!
        else {
            return parseReduce(rawMemory[id]!!)
        }
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
            } else if (repr == "-inf") {
                repr = "float('$repr')"
            }
            PythonTree.PrimitiveNode(PythonClassId(type), repr)
        } else {
            when (type) {
                "builtins.list" -> parsePythonList(json.array("value")!!)
                "builtins.set" -> parsePythonSet(json.array("value")!!)
                "builtins.tuple" -> parsePythonTuple(json.array("value")!!)
                "builtins.dict" -> parsePythonDict(json.array("value")!!)
                else -> findInMemory(json.long("value")!!)
            }
        }
        result.comparable = comparable
        return result
    }

    private fun parseReduce(value: JsonObject): PythonTree.PythonTreeNode {
        val id = value.long("id")!!
        val initObject = PythonTree.ReduceNode(
            id,
            PythonClassId(value.string("type")!!),
            PythonClassId(value.string("constructor")!!),
            parsePythonList(value.array("args")!!).children,
        )
        memory[id] = initObject
        initObject.state = parsePythonDict(value.array("state")!!).items.map {
            (it.key as PythonTree.PrimitiveNode).repr to it.value
        }.toMap()
        initObject.listitems = parsePythonList(value.array("listitems")!!).children
        initObject.dictitems = parsePythonDict(value.array("dictitems")!!).items
        return initObject
    }

    private fun parsePythonList(items: JsonArray<JsonObject>): PythonTree.ListNode {
        return PythonTree.ListNode(items.mapIndexed { index, jsonObject -> index to parseToPythonTree(jsonObject) }.toMap().toMutableMap())
    }

    private fun parsePythonSet(items: JsonArray<JsonObject>): PythonTree.SetNode {
        return PythonTree.SetNode(items.map { parseToPythonTree(it) }.toSet())
    }

    private fun parsePythonTuple(items: JsonArray<JsonObject>): PythonTree.TupleNode {
        return PythonTree.TupleNode(items.mapIndexed { index, jsonObject -> index to parseToPythonTree(jsonObject) }.toMap().toMutableMap())
    }

    private fun parsePythonDict(items: JsonArray<JsonArray<Any>>): PythonTree.DictNode {
        return PythonTree.DictNode(items.associate {
            val key = it[0]
            val value = it[1] as JsonObject
            (
                    if (key is String)
                        PythonTree.PrimitiveNode(
                            PythonClassId("builtins.str"),
                            key
                        )
                    else
                        parseToPythonTree(key as JsonObject)
                    ) to parseToPythonTree(value)
        })
    }
}