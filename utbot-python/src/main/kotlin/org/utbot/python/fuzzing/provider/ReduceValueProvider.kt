package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.general.Type

object ReduceValueProvider : ValueProvider<Type, PythonFuzzedValue, PythonMethodDescription> {
    private val unsupportedTypes = listOf(
        "builtins.list",
        "builtins.set",
        "builtins.tuple",
        "builtins.dict",
        "builtins.bytes",
        "builtins.bytearray",
        "builtins.complex",
        "builtins.int",
        "builtins.float",
        "builtins.str",
        "builtins.bool",
    )

    override fun accept(type: Type): Boolean {
        val hasSupportedType =
            !unsupportedTypes.contains(type.pythonTypeName())
        return hasSupportedType && type.meta is PythonConcreteCompositeTypeDescription // && (hasInit || hasNew)
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence {
        val initMethodName = "__init__"
        val newMethodName = "__new__"
        val typeDescr = type.pythonDescription()
        val constructors =
            if (typeDescr is PythonCompositeTypeDescription) {
                val mro = typeDescr.mro(description.pythonTypeStorage, type)
                val initParent = mro.indexOfFirst { p -> p.getPythonAttributes().any { it.meta.name == initMethodName } }
                val newParent = mro.indexOfFirst { p -> p.getPythonAttributes().any { it.meta.name == newMethodName } }
                val initMethods = type.getPythonAttributeByName(description.pythonTypeStorage, initMethodName)
                val newMethods = type.getPythonAttributeByName(description.pythonTypeStorage, newMethodName)
                if (initParent <= newParent && initMethods != null) {
                    listOf(initMethods)
                } else if (newMethods != null) {
                    listOf(newMethods)
                } else {
                    emptyList()  // probably not reachable (because of class object)
                }
            } else {
                emptyList()
            }
        constructors
            .forEach {
                // TODO: here we need to use same as .getPythonAttributeByName but without name
                // TODO: now we do not have fields from parents
                val fields = type.getPythonAttributes()
                    .filter { attr ->
                        !(attr.meta.name.startsWith("__") && attr.meta.name.endsWith("__") && attr.meta.name.length >= 4) &&
                                (attr.meta as? PythonVariableDescription)?.isProperty != true && attr.type.getPythonAttributeByName(
                            description.pythonTypeStorage,
                            "__call__"
                        ) == null
                    }

                val modifications = emptyList<Routine.Call<Type, PythonFuzzedValue>>().toMutableList()
                modifications.addAll(fields.map { field ->
                    Routine.Call(listOf(field.type)) { instance, arguments ->
                        val obj = instance.tree as PythonTree.ReduceNode
                        obj.state[field.meta.name] = arguments.first().tree
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
        val description = constructorFunction.pythonDescription() as PythonCallableTypeDescription
        val positionalArgs = description.argumentKinds.count { it == PythonCallableTypeDescription.ArgKind.ARG_POS }
        val arguments = constructorFunction.arguments.take(positionalArgs)
        val nonSelfArgs = arguments.drop(1)

        return Seed.Recursive(
            construct = Routine.Create(nonSelfArgs) { v ->
                PythonFuzzedValue(
                    PythonTree.ReduceNode(
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
        constructor: PythonDefinition,
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