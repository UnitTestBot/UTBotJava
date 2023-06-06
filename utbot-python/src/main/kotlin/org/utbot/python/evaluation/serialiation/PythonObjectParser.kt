package org.utbot.python.evaluation.serialiation

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTree

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

class TypeInfo(
    val module: String,
    val kind: String,
) {
    val qualname: String = if (module.isEmpty()) kind else "$module.$kind"
}

sealed class MemoryObject(
    val id: String,
    val typeinfo: TypeInfo,
    val comparable: Boolean,
) {
    val qualname: String = typeinfo.qualname
}

class ReprMemoryObject(
    id: String,
    typeinfo: TypeInfo,
    comparable: Boolean,
    val value: String,
) : MemoryObject(id, typeinfo, comparable)

class ListMemoryObject(
    id: String,
    typeinfo: TypeInfo,
    comparable: Boolean,
    val items: List<String>,
) : MemoryObject(id, typeinfo, comparable)

class DictMemoryObject(
    id: String,
    typeinfo: TypeInfo,
    comparable: Boolean,
    val items: Map<String, String>,
) : MemoryObject(id, typeinfo, comparable)

class ReduceMemoryObject(
    id: String,
    typeinfo: TypeInfo,
    comparable: Boolean,
    val constructor: TypeInfo,
    val args: String,
    val state: String,
    val listitems: String,
    val dictitems: String
) : MemoryObject(id, typeinfo, comparable)

fun PythonTree.PythonTreeNode.toMemoryObject(memoryDump: MemoryDump): String {
    val obj = when (this) {
        is PythonTree.PrimitiveNode -> {
            ReprMemoryObject(
                this.id.toString(),
                TypeInfo(this.type.moduleName, this.type.typeName),
                this.comparable,
                this.repr
            )
        }

        is PythonTree.ListNode -> {
            val items = this.items.entries
                .sortedBy { it.key }
                .map { it.value.toMemoryObject(memoryDump) }
            ListMemoryObject(
                this.id.toString(),
                TypeInfo(this.type.moduleName, this.type.typeName),
                this.comparable,
                items
            )
        }

        is PythonTree.TupleNode -> {
            val items = this.items.entries
                .sortedBy { it.key }
                .map { it.value.toMemoryObject(memoryDump) }
            ListMemoryObject(
                this.id.toString(),
                TypeInfo(this.type.moduleName, this.type.typeName),
                this.comparable,
                items
            )
        }

        is PythonTree.SetNode -> {
            val items = this.items.map { it.toMemoryObject(memoryDump) }
            ListMemoryObject(
                this.id.toString(),
                TypeInfo(this.type.moduleName, this.type.typeName),
                this.comparable,
                items
            )
        }

        is PythonTree.DictNode -> {
            val items = this.items.entries
                .associate {
                    it.key.toMemoryObject(memoryDump) to it.value.toMemoryObject(memoryDump)
                }
            DictMemoryObject(
                this.id.toString(),
                TypeInfo(this.type.moduleName, this.type.typeName),
                this.comparable,
                items
            )
        }

        is PythonTree.ReduceNode -> {
            val stateObjId = PythonTree.DictNode(this.state.entries.associate {
                PythonTree.fromString(it.key) to it.value
            }.toMutableMap())
            val argsIds = PythonTree.ListNode(this.args.withIndex().associate { it.index to it.value }.toMutableMap())
            val listItemsIds =
                PythonTree.ListNode(this.listitems.withIndex().associate { it.index to it.value }.toMutableMap())
            val dictItemsIds = PythonTree.DictNode(this.dictitems.toMutableMap())
            ReduceMemoryObject(
                this.id.toString(),
                TypeInfo(
                    this.type.moduleName,
                    this.type.typeName,
                ),
                this.comparable,
                TypeInfo(
                    this.constructor.moduleName,
                    this.constructor.typeName,
                ),
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

fun MemoryObject.toPythonTree(
    memoryDump: MemoryDump,
    visited: MutableMap<String, PythonTree.PythonTreeNode> = mutableMapOf()
): PythonTree.PythonTreeNode {
    val obj = visited.getOrPut(this.id) {
        val id = this.id.toLong()
        val obj = when (this) {
            is ReprMemoryObject -> {
                PythonTree.PrimitiveNode(
                    id,
                    PythonClassId(this.typeinfo.module, this.typeinfo.kind),
                    value
                )
            }

            is DictMemoryObject -> {
                PythonTree.DictNode(
                    id,
                    items.entries.associate {
                        memoryDump.getById(it.key).toPythonTree(memoryDump, visited) to
                                memoryDump.getById(it.value).toPythonTree(memoryDump, visited)
                    }.toMutableMap()
                )
            }

            is ListMemoryObject -> {
                val elementsMap = items.withIndex().associate {
                    it.index to
                            memoryDump.getById(it.value).toPythonTree(memoryDump, visited)
                }.toMutableMap()
                when (this.qualname) {
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
                val prevObj = PythonTree.ReduceNode(
                    id,
                    PythonClassId(this.typeinfo.module, this.typeinfo.kind),
                    PythonClassId(this.constructor.module, this.constructor.kind),
                    arguments.items.map { memoryDump.getById(it).toPythonTree(memoryDump, visited) },
                )
                visited[this.id] = prevObj

                prevObj.state = stateObjs.items.entries.associate {
                    (memoryDump.getById(it.key).toPythonTree(memoryDump, visited) as PythonTree.PrimitiveNode)
                        .repr.drop(1).dropLast(1) to
                            memoryDump.getById(it.value).toPythonTree(memoryDump, visited)
                }.toMutableMap()
                prevObj.listitems = listitemsObjs.items.map { memoryDump.getById(it).toPythonTree(memoryDump, visited) }
                prevObj.dictitems = dictitemsObjs.items.entries.associate {
                    memoryDump.getById(it.key).toPythonTree(memoryDump, visited) to
                            memoryDump.getById(it.value).toPythonTree(memoryDump, visited)
                }
                prevObj
            }
        }
        obj.comparable = this.comparable
        return obj
    }
    return obj
}

fun serializeObjects(objs: List<PythonTree.PythonTreeNode>): Pair<List<String>, String> {
    val memoryDump = MemoryDump(emptyMap<String, MemoryObject>().toMutableMap())
    val ids = objs.map { it.toMemoryObject(memoryDump) }
    return Pair(ids, PythonObjectParser.serializeMemory(memoryDump))
}
