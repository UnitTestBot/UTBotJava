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
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.getPythonAttributes
import org.utbot.python.newtyping.pythonTypeName
import org.utbot.python.newtyping.pythonTypeRepresentation

class ReduceValueProvider(
    private val idGenerator: IdGenerator<Long>
) : ValueProvider<Type, PythonFuzzedValue, PythonMethodDescription> {
    private val unsupportedTypes = listOf<String>(
        "builtins.list",
        "builtins.set",
        "builtins.tuple",
        "builtins.dict",
        "builtins.bytes",
        "builtins.bytearray",
        "builtins.complex",
    )

    override fun accept(type: Type): Boolean {
        val hasInit =
            type.getPythonAttributes().any { it.name == "__init__" }
        val hasNew =
            type.getPythonAttributes().any { it.name == "__new__" }
        val hasSupportedType =
            !unsupportedTypes.contains(type.pythonTypeName())
        return hasSupportedType && type.meta is PythonConcreteCompositeTypeDescription && (hasInit || hasNew)
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence {
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
                yieldAll(callConstructors(type, it, modifications.asSequence()))
            }
    }

    private fun constructObject(
        type: Type,
        constructorFunction: FunctionType,
        modifications: Sequence<Routine.Call<Type, PythonFuzzedValue>>
    ): Seed.Recursive<Type, PythonFuzzedValue> {
        val arguments = constructorFunction.arguments
        val nonSelfArgs = arguments.drop(1)

        return Seed.Recursive(
            construct = Routine.Create(nonSelfArgs) { v ->
                PythonFuzzedValue(
                    PythonTree.ReduceNode(
                        idGenerator.createId(),
                        PythonClassId(type.pythonTypeName()),
                        PythonClassId(type.pythonTypeName()),
                        v.map { it.tree },
                    ),
                    "%var% = ${type.pythonTypeRepresentation()}"
                )
            },
            modify = modifications,
            empty = Routine.Empty {
                PythonFuzzedValue(
                    PythonTree.fromObject(),
                    "%var% = ${type.pythonTypeRepresentation()}"
                )
            }
        )
    }

    private fun callConstructors(
        type: Type,
        constructor: PythonAttribute,
        modifications: Sequence<Routine.Call<Type, PythonFuzzedValue>>
    ): Sequence<Seed.Recursive<Type, PythonFuzzedValue>> = sequence {
        val constructors = emptyList<FunctionType>().toMutableList()
        if (constructor.type.pythonTypeName() == "Overload") {
            constructor.type.parameters.forEach {
                if (it is FunctionType) {
                    constructors.add(it)
                }
            }
        } else {
            constructors.add(constructor.type as FunctionType)
        }
        constructors.forEach {
            yield(constructObject(type, it, modifications))
        }
    }
}