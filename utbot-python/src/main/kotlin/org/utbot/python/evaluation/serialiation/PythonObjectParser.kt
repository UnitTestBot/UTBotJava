package org.utbot.python.evaluation.serialiation

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonStrClassId

object PythonObjectParser {
    private val moshi = Moshi.Builder()
        .add(
            PolymorphicJsonAdapterFactory.of(MemoryObject::class.java, "strategy")
                .withSubtype(ReprMemoryObject::class.java, "repr")
                .withSubtype(ListMemoryObject::class.java, "list")
                .withSubtype(DictMemoryObject::class.java, "dict")
                .withSubtype(ReduceMemoryObject::class.java, "reduce")
        )
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val jsonAdapter = moshi.adapter(MemoryDump::class.java)

    fun parseDumpedObjects(jsonWithDump: String): MemoryDump {
        return jsonAdapter.fromJson(jsonWithDump) ?: error("Couldn't parse json dump")
    }

    fun serializeMemory(memory: MemoryDump): String {
        return jsonAdapter.toJson(memory) ?: error("Couldn't serialize dump to json")
    }
}

class MemoryDump(
    private val objects: MutableMap<String, MemoryObject>
) {
    fun getById(id: String): MemoryObject {
        return objects[id]!!
    }

    fun addObject(value: MemoryObject) {
        objects[value.id] = value
    }
}

sealed class MemoryObject(
    val id: String,
    val kind: String,
    val module: String,
    val comparable: Boolean,
)

class ReprMemoryObject(
    id: String,
    kind: String,
    module: String,
    comparable: Boolean,
    val value: String,
): MemoryObject(id, kind, module, comparable)

class ListMemoryObject(
    id: String,
    kind: String,
    module: String,
    comparable: Boolean,
    val items: List<String>,
): MemoryObject(id, kind, module, comparable)

class DictMemoryObject(
    id: String,
    kind: String,
    module: String,
    comparable: Boolean,
    val items: Map<String, String>,
): MemoryObject(id, kind, module, comparable)

class ReduceMemoryObject(
    id: String,
    kind: String,
    module: String,
    comparable: Boolean,
    val constructor: String,
    val args: String,
    val state: String,
    val listitems: String,
    val dictitems: String
): MemoryObject(id, kind, module, comparable)

fun PythonTree.PythonTreeNode.toMemoryObject(memoryDump: MemoryDump): String {
    val obj = when(this) {
        is PythonTree.PrimitiveNode -> {
            ReprMemoryObject(this.id.toString(), this.type.name, this.type.moduleName, this.comparable, this.repr)
        }
        is PythonTree.ListNode -> {
            val items = this.items.entries
                .sortedBy { it.key }
                .map { it.value.toMemoryObject(memoryDump) }
            ListMemoryObject(this.id.toString(), this.type.name, this.type.moduleName, this.comparable, items)
        }
        is PythonTree.TupleNode -> {
            val items = this.items.entries
                .sortedBy { it.key }
                .map { it.value.toMemoryObject(memoryDump) }
            ListMemoryObject(this.id.toString(), this.type.name, this.type.moduleName, this.comparable, items)
        }
        is PythonTree.SetNode -> {
            val items = this.items.map { it.toMemoryObject(memoryDump) }
            ListMemoryObject(this.id.toString(), this.type.name, this.type.moduleName, this.comparable, items)
        }
        is PythonTree.DictNode -> {
            val items = this.items.entries
                .associate {
                    it.key.toMemoryObject(memoryDump) to it.value.toMemoryObject(memoryDump)
                }
            DictMemoryObject(this.id.toString(), this.type.name, this.type.moduleName, this.comparable, items)
        }
        is PythonTree.ReduceNode -> {
            val stateObjId = PythonTree.DictNode(this.state.entries.associate { PythonTree.PrimitiveNode(pythonStrClassId, it.key) to it.value }.toMutableMap())
            val argsIds = PythonTree.ListNode(this.args.withIndex().associate { it.index to it.value }.toMutableMap())
            val listItemsIds = PythonTree.ListNode(this.listitems.withIndex().associate { it.index to it.value }.toMutableMap())
            val dictItemsIds = PythonTree.DictNode(this.dictitems.toMutableMap())
            ReduceMemoryObject(
                this.id.toString(),
                this.type.name,
                this.type.moduleName,
                this.comparable,
                this.constructor.name,
                argsIds.toMemoryObject(memoryDump),
                stateObjId.toMemoryObject(memoryDump),
                listItemsIds.toMemoryObject(memoryDump),
                dictItemsIds.toMemoryObject(memoryDump),
            )
        }
        else -> {
            error("Invalid PythonTree.PythonTreeNode $this")
        }
    }
    memoryDump.addObject(obj)
    return obj.id
}

fun MemoryObject.toPythonTree(memoryDump: MemoryDump): PythonTree.PythonTreeNode {
    val obj = when(this) {
        is ReprMemoryObject -> {
            PythonTree.PrimitiveNode(
                this.id.toLong(),
                PythonClassId(this.module, this.kind),
                this.value
            )
        }
        is DictMemoryObject -> {
            PythonTree.DictNode(
                this.id.toLong(),
                items.entries.associate {
                    memoryDump.getById(it.key).toPythonTree(memoryDump) to memoryDump.getById(it.value).toPythonTree(memoryDump)
                }.toMutableMap()
            )
        }
        is ListMemoryObject -> {
            val elementsMap = items.withIndex().associate {
                    it.index to memoryDump.getById(it.value).toPythonTree(memoryDump)
                }.toMutableMap()
            when (this.kind) {
                "builtins.tuple" -> {
                    PythonTree.TupleNode(this.id.toLong(), elementsMap)
                }
                "builtins.set" -> {
                    PythonTree.SetNode(this.id.toLong(), elementsMap.values.toMutableSet())
                }
                else -> {
                    PythonTree.ListNode(this.id.toLong(), elementsMap)
                }
            }
        }
        is ReduceMemoryObject -> {
            val stateObjs = memoryDump.getById(state) as DictMemoryObject
            val arguments = memoryDump.getById(args) as ListMemoryObject
            val listitemsObjs = memoryDump.getById(listitems) as ListMemoryObject
            val dictitemsObjs = memoryDump.getById(dictitems) as DictMemoryObject
            PythonTree.ReduceNode(
                this.id.toLong(),
                PythonClassId(this.kind),
                PythonClassId(this.constructor),
                arguments.items.map { memoryDump.getById(it).toPythonTree(memoryDump) },
                stateObjs.items.entries.associate {
                    (memoryDump.getById(it.key).toPythonTree(memoryDump) as PythonTree.PrimitiveNode).repr.drop(1).dropLast(1) to
                    memoryDump.getById(it.value).toPythonTree(memoryDump)
                }.toMutableMap(),
                listitemsObjs.items.map { memoryDump.getById(it).toPythonTree(memoryDump) },
                dictitemsObjs.items.entries.associate {
                    memoryDump.getById(it.key).toPythonTree(memoryDump) to
                    memoryDump.getById(it.value).toPythonTree(memoryDump)
                },
            )
        }
    }
    obj.comparable = this.comparable
    return obj
}

fun serializeObjects(objs: List<PythonTree.PythonTreeNode>): Pair<List<String>, String> {
    val memoryDump = MemoryDump(emptyMap<String, MemoryObject>().toMutableMap())
    val ids = objs.map { it.toMemoryObject(memoryDump) }
    return Pair(ids, PythonObjectParser.serializeMemory(memoryDump))
}

fun main() {
    val dump = """
    {
    "objects": {
     "140239390887040": {
      "strategy": "list",
      "id": "140239390887040",
      "kind": "builtins.list",
      "comparable": false,
      "items": [
       "140239394832624",
       "140239394832656",
       "140239392627184",
       "140239394012784",
       "140239392795520",
       "140239406001728",
       "140239392839840",
       "140239390894848"
      ]
     },
     "140239394832624": {
      "strategy": "repr",
      "id": "140239394832624",
      "kind": "builtins.int",
      "comparable": true,
      "value": "1"
     },
     "140239394832656": {
      "strategy": "repr",
      "id": "140239394832656",
      "kind": "builtins.int",
      "comparable": true,
      "value": "2"
     },
     "140239392627184": {
      "strategy": "repr",
      "id": "140239392627184",
      "kind": "builtins.float",
      "comparable": true,
      "value": "float('inf')"
     },
     "140239394012784": {
      "strategy": "repr",
      "id": "140239394012784",
      "kind": "builtins.str",
      "comparable": true,
      "value": "'abc'"
     },
     "140239392795520": {
      "strategy": "dict",
      "id": "140239392795520",
      "kind": "builtins.dict",
      "comparable": true,
      "items": {
       "140239394832624": "140239394832624"
      }
     },
     "140239406001728": {
      "strategy": "repr",
      "id": "140239406001728",
      "kind": "types.NoneType",
      "comparable": true,
      "value": "None"
     },
     "140239391427840": {
      "strategy": "list",
      "id": "140239391427840",
      "kind": "builtins.tuple",
      "comparable": true,
      "items": [
       "94246249326576",
       "140239405994592",
       "140239406001728"
      ]
     },
     "94246249326576": {
      "strategy": "repr",
      "id": "94246249326576",
      "kind": "builtins.type",
      "comparable": true,
      "value": "deep_serialization.example.B"
     },
     "140239405994592": {
      "strategy": "repr",
      "id": "140239405994592",
      "kind": "builtins.type",
      "comparable": true,
      "value": "builtins.object"
     },
     "140239392839840": {
      "strategy": "reduce",
      "id": "140239392839840",
      "kind": "deep_serialization.example.B",
      "comparable": false,
      "constructor": "copyreg._reconstructor",
      "args": "140239391427840",
      "state": "140239392795712",
      "listitems": "140239391672832",
      "dictitems": "140239391673280"
     },
     "140239392795712": {
      "strategy": "dict",
      "id": "140239392795712",
      "kind": "builtins.dict",
      "comparable": true,
      "items": {
       "140239392797168": "140239394832624",
       "140239392797232": "140239394832656",
       "140239392797296": "140239394832688",
       "140239393831920": "140239392849760"
      }
     },
     "140239392797168": {
      "strategy": "repr",
      "id": "140239392797168",
      "kind": "builtins.str",
      "comparable": true,
      "value": "'b1'"
     },
     "140239392797232": {
      "strategy": "repr",
      "id": "140239392797232",
      "kind": "builtins.str",
      "comparable": true,
      "value": "'b2'"
     },
     "140239392797296": {
      "strategy": "repr",
      "id": "140239392797296",
      "kind": "builtins.str",
      "comparable": true,
      "value": "'b3'"
     },
     "140239394832688": {
      "strategy": "repr",
      "id": "140239394832688",
      "kind": "builtins.int",
      "comparable": true,
      "value": "3"
     },
     "140239393831920": {
      "strategy": "repr",
      "id": "140239393831920",
      "kind": "builtins.str",
      "comparable": true,
      "value": "'time'"
     },
     "140239394159488": {
      "strategy": "list",
      "id": "140239394159488",
      "kind": "builtins.tuple",
      "comparable": true,
      "items": [
       "140239391514208"
      ]
     },
     "140239391514208": {
      "strategy": "repr",
      "id": "140239391514208",
      "kind": "builtins.bytes",
      "comparable": true,
      "value": "b'\\x07\\xe7\\x02\\r\\x0c(3\\x06\\x1eA'"
     },
     "140239392849760": {
      "strategy": "reduce",
      "id": "140239392849760",
      "kind": "datetime.datetime",
      "comparable": true,
      "constructor": "datetime.datetime",
      "args": "140239394159488",
      "state": "140239391671232",
      "listitems": "140239391671872",
      "dictitems": "140239391672448"
     },
     "140239391671232": {
      "strategy": "dict",
      "id": "140239391671232",
      "kind": "builtins.dict",
      "comparable": true,
      "items": {}
     },
     "140239391671872": {
      "strategy": "list",
      "id": "140239391671872",
      "kind": "builtins.list",
      "comparable": true,
      "items": []
     },
     "140239391672448": {
      "strategy": "dict",
      "id": "140239391672448",
      "kind": "builtins.dict",
      "comparable": true,
      "items": {}
     },
     "140239391672832": {
      "strategy": "list",
      "id": "140239391672832",
      "kind": "builtins.list",
      "comparable": true,
      "items": []
     },
     "140239391673280": {
      "strategy": "dict",
      "id": "140239391673280",
      "kind": "builtins.dict",
      "comparable": true,
      "items": {}
     },
     "140239390894848": {
      "strategy": "list",
      "id": "140239390894848",
      "kind": "builtins.list",
      "comparable": true,
      "items": [
       "140239392797552"
      ]
     },
     "140239392797552": {
      "strategy": "repr",
      "id": "140239392797552",
      "kind": "builtins.str",
      "comparable": true,
      "value": "'Alex'"
     }
    }
    }
""".trimIndent()

    val load = PythonObjectParser.parseDumpedObjects(dump)
    val obj = load.getById("140239390887040").toPythonTree(load)
    val newMemory = MemoryDump(emptyMap<String, MemoryObject>().toMutableMap())
    val newId = obj.toMemoryObject(newMemory)
    val newDump = PythonObjectParser.serializeMemory(newMemory)
    val newLoad = PythonObjectParser.parseDumpedObjects(newDump)
    val newObj = newLoad.getById(newId).toPythonTree(newLoad)
}

