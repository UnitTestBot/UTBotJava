package org.utbot.python.fuzzing.provider

import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.PythonAttribute
import org.utbot.python.newtyping.PythonConcreteCompositeTypeDescription
import org.utbot.python.newtyping.general.FunctionTypeCreator
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.getPythonAttributes

class ReduceValueProvider(
    private val idGenerator: IdGenerator<Long>
) : ValueProvider<Type, PythonFuzzedValue, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        val hasInit =
            type.getPythonAttributes().any { it.name == "__init__" && it.type is FunctionTypeCreator.Original }
        val hasNew =
            type.getPythonAttributes().any { it.name == "__new__" && it.type is FunctionTypeCreator.Original }
        return type.meta is PythonConcreteCompositeTypeDescription && (hasInit || hasNew)
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence {
        val meta = type.meta as PythonConcreteCompositeTypeDescription
        type.getPythonAttributes()
            .filter { it.name == "__init__" || it.name == "__new__" }
            .forEach {
                val fields = type.getPythonAttributes()
                    .filter { attr -> attr.type.getPythonAttributes().all { it.name != "__call__" } }

                val modifications = emptyList<Routine.Call<Type, PythonFuzzedValue>>().toMutableList()
                modifications.addAll(fields.map { field ->
                    Routine.Call(listOf(field.type)) { instance, arguments ->
                        val obj = instance.tree as PythonTree.ReduceNode
                        obj.state[field.name] = arguments.first().tree
                    }
                })
                yield(constructObject(meta, it, modifications.asSequence()))
            }
    }

    private fun constructObject(meta: PythonConcreteCompositeTypeDescription, constructor: PythonAttribute, modifications: Sequence<Routine.Call<Type, PythonFuzzedValue>>): Seed.Recursive<Type, PythonFuzzedValue> {
        return when (constructor.name) {
            "__init__" -> {
                val arguments = (constructor.type as FunctionTypeCreator.Original).arguments
                val nonSelfArgs = arguments.drop(1)

                Seed.Recursive(
                    construct = Routine.Create(nonSelfArgs) { v ->
                        PythonFuzzedValue(
                            PythonTree.ReduceNode(
                                idGenerator.createId(),
                                PythonClassId(meta.name.toString()),
                                PythonClassId(meta.name.toString()),
                                v.map { it.tree },
                            ),
                        )
                    },
                    modify = modifications.asSequence(),
                    empty = Routine.Empty {
                        PythonFuzzedValue(
                            PythonTree.fromObject(),
                            "%var% = ${meta.name}"
                        )
                    }
                )
            }

            "__new__" -> {
                val arguments = (constructor.type as FunctionTypeCreator.Original).arguments
                val nonClsArgs = arguments.drop(1)

                Seed.Recursive(
                    construct = Routine.Create(nonClsArgs) { v ->
                        PythonFuzzedValue(
                            PythonTree.ReduceNode(
                                idGenerator.createId(),
                                PythonClassId(meta.name.toString()),
                                PythonClassId(meta.name.toString()),
                                v.map { it.tree },
                            ),
                        )
                    },
                    empty = Routine.Empty {
                        PythonFuzzedValue(
                            PythonTree.fromObject(),
                            "%var% = ${meta.name}"
                        )
                    }
                )
            }

            else -> {
                throw IllegalArgumentException("Invalid constructor name ${constructor.name}")
            }
        }
    }
}