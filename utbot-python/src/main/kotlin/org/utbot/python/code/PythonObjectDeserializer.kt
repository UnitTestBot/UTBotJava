package org.utbot.python.code

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTree

object PythonObjectDeserializer {
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
}

class MemoryDump(
    private val objects: Map<String, MemoryObject>
) {
    fun getById(id: String): MemoryObject {
        return objects[id]!!
    }

    fun getByValue(value: MemoryObject): String {
        return objects.filter { it.value == value }.keys.first()
    }
}

sealed class MemoryObject(
    val kind: String,
    val comparable: Boolean,
)

class ReprMemoryObject(
    kind: String,
    comparable: Boolean,
    val value: String,
): MemoryObject(kind, comparable)

class ListMemoryObject(
    kind: String,
    comparable: Boolean,
    val items: List<String>,
): MemoryObject(kind, comparable)

class DictMemoryObject(
    kind: String,
    comparable: Boolean,
    val items: Map<String, String>,
): MemoryObject(kind, comparable)

class ReduceMemoryObject(
    kind: String,
    comparable: Boolean,
    val constructor: String,
    val args: String,
    val state: String,
    val listitems: String,
    val dictitems: String
): MemoryObject(kind, comparable)

fun MemoryObject.toPythonTree(memoryDump: MemoryDump): PythonTree.PythonTreeNode {
    return when(this) {
        is ReprMemoryObject -> {
           PythonTree.PrimitiveNode(PythonClassId(this.kind), this.value)
        }
        is DictMemoryObject -> {
            PythonTree.DictNode(
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
                    PythonTree.TupleNode(elementsMap)
                }
                "builtins.set" -> {
                    PythonTree.SetNode(elementsMap.values.toMutableSet())
                }
                else -> {
                    PythonTree.ListNode(elementsMap)
                }
            }
        }
        is ReduceMemoryObject -> {
            val state_objs = memoryDump.getById(state) as DictMemoryObject
            val arguments = memoryDump.getById(args) as ListMemoryObject
            val listitems_objs = memoryDump.getById(listitems) as ListMemoryObject
            val dictitems_objs = memoryDump.getById(dictitems) as DictMemoryObject
            PythonTree.ReduceNode(
                memoryDump.getByValue(this).toLong(),
                PythonClassId(this.kind),
                PythonClassId(this.constructor),
                arguments.items.map { memoryDump.getById(it).toPythonTree(memoryDump) },
                state_objs.items.entries.associate {
                    (memoryDump.getById(it.key).toPythonTree(memoryDump) as PythonTree.PrimitiveNode).repr.drop(1).dropLast(1) to
                    memoryDump.getById(it.value).toPythonTree(memoryDump)
                }.toMutableMap(),
                listitems_objs.items.map { memoryDump.getById(it).toPythonTree(memoryDump) },
                dictitems_objs.items.entries.associate {
                    memoryDump.getById(it.key).toPythonTree(memoryDump) to
                    memoryDump.getById(it.value).toPythonTree(memoryDump)
                }
            )
        }
    }
}

fun main() {
    val dump = """
    {"objects": {"140598295296000": {"strategy": "list", "kind": "builtins.list", "comparable": false, "items": ["140598416769264", "140598416769296", "140598416769328", "140598295298816", "140598427938368", "140598374175968"]}, "140598416769264": {"strategy": "repr", "kind": "builtins.int", "comparable": true, "value": "1"}, "140598416769296": {"strategy": "repr", "kind": "builtins.int", "comparable": true, "value": "2"}, "140598416769328": {"strategy": "repr", "kind": "builtins.int", "comparable": true, "value": "3"}, "140598295298816": {"strategy": "dict", "kind": "builtins.dict", "comparable": true, "items": {"140598416769264": "140598416769264"}}, "140598427938368": {"strategy": "repr", "kind": "types.NoneType", "comparable": true, "value": "None"}, "140598372733504": {"strategy": "list", "kind": "builtins.tuple", "comparable": true, "items": ["94206620040656", "140598427931232", "140598427938368"]}, "94206620040656": {"strategy": "repr", "kind": "builtins.type", "comparable": true, "value": "deep_serialization.example.B"}, "140598427931232": {"strategy": "repr", "kind": "builtins.type", "comparable": true, "value": "builtins.object"}, "140598374175968": {"strategy": "reduce", "kind": "deep_serialization.example.B", "comparable": false, "constructor": "copyreg._reconstructor", "args": "140598372733504", "state": "140598295238656", "listitems": "140598295487936", "dictitems": "140598295486336"}, "140598295238656": {"strategy": "dict", "kind": "builtins.dict", "comparable": true, "items": {"140598386103280": "140598416769264", "140598395465264": "140598416769296", "140598372712880": "140598416769328", "140598415768816": "140598374177584"}}, "140598386103280": {"strategy": "repr", "kind": "builtins.str", "comparable": true, "value": "\'b1\'"}, "140598395465264": {"strategy": "repr", "kind": "builtins.str", "comparable": true, "value": "\'b2\'"}, "140598372712880": {"strategy": "repr", "kind": "builtins.str", "comparable": true, "value": "\'b3\'"}, "140598415768816": {"strategy": "repr", "kind": "builtins.str", "comparable": true, "value": "\'time\'"}, "140598374184560": {"strategy": "list", "kind": "builtins.tuple", "comparable": true, "items": ["140598295162016"]}, "140598295162016": {"strategy": "repr", "kind": "builtins.bytes", "comparable": true, "value": "b\'\\\\x07\\\\xe7\\\\x01\\\\x13\\\\x11\\\\x01\\\\x1c\\\\x0e\\\\x921\'"}, "140598374177584": {"strategy": "reduce", "kind": "datetime.datetime", "comparable": true, "constructor": "datetime.datetime", "args": "140598374184560", "state": "140598295312768", "listitems": "140598295488000", "dictitems": "140598295485760"}, "140598295312768": {"strategy": "dict", "kind": "builtins.dict", "comparable": true, "items": {}}, "140598295488000": {"strategy": "list", "kind": "builtins.list", "comparable": true, "items": []}, "140598295485760": {"strategy": "dict", "kind": "builtins.dict", "comparable": true, "items": {}}, "140598295487936": {"strategy": "list", "kind": "builtins.list", "comparable": true, "items": []}, "140598295486336": {"strategy": "dict", "kind": "builtins.dict", "comparable": true, "items": {}}}}
""".trimIndent()

    val load = PythonObjectDeserializer.parseDumpedObjects(dump)
    load.getById("140598295296000").toPythonTree(load)
}

