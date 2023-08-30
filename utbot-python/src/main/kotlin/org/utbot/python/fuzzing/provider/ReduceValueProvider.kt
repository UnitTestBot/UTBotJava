package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.*
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.provider.utils.isCallable
import org.utbot.python.fuzzing.provider.utils.isConcreteType
import org.utbot.python.fuzzing.provider.utils.isMagic
import org.utbot.python.fuzzing.provider.utils.isPrivate
import org.utbot.python.fuzzing.provider.utils.isProperty
import org.utbot.python.fuzzing.provider.utils.isProtected
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.general.UtType

object ReduceValueProvider : ValueProvider<UtType, PythonFuzzedValue, PythonMethodDescription> {
    private val unsupportedTypes = listOf(
        pythonRePatternClassId.canonicalName,
        pythonListClassId.canonicalName,
        pythonSetClassId.canonicalName,
        pythonTupleClassId.canonicalName,
        pythonDictClassId.canonicalName,
        pythonBytesClassId.canonicalName,
        pythonBytearrayClassId.canonicalName,
        pythonComplexClassId.canonicalName,
        pythonIntClassId.canonicalName,
        pythonFloatClassId.canonicalName,
        pythonStrClassId.canonicalName,
        pythonBoolClassId.canonicalName,
    )

    override fun accept(type: UtType): Boolean {
        val hasSupportedType =
            !unsupportedTypes.contains(type.pythonTypeName())
        return hasSupportedType && isConcreteType(type)
    }

    override fun generate(description: PythonMethodDescription, type: UtType) = sequence {
        val fields = findFields(description, type)
        findConstructors(description, type)
            .forEach {
                val modifications = emptyList<Routine.Call<UtType, PythonFuzzedValue>>().toMutableList()
                modifications.addAll(fields.map { field ->
                    Routine.Call(listOf(field.type)) { instance, arguments ->
                        val obj = instance.tree as PythonTree.ReduceNode
                        obj.state[field.meta.name] = arguments.first().tree
                    }
                })
                yieldAll(callConstructors(type, it, modifications.asSequence()))
            }
    }

    private fun findFields(description: PythonMethodDescription, type: UtType): List<PythonDefinition> {
        // TODO: here we need to use same as .getPythonAttributeByName but without name
        // TODO: now we do not have fields from parents
        // TODO: here we should use only attributes from __slots__
        return type.getPythonAttributes().filter { attr ->
            !attr.isMagic() && !attr.isProtected() && !attr.isPrivate() && !attr.isProperty() && !attr.isCallable(
                description.pythonTypeStorage
            )
        }
    }

    private fun findConstructors(description: PythonMethodDescription, type: UtType): List<PythonDefinition> {
        val initMethodName = "__init__"
        val newMethodName = "__new__"
        val typeDescr = type.pythonDescription()
        return if (typeDescr is PythonCompositeTypeDescription) {
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
    }

    private fun constructObject(
        type: UtType,
        constructorFunction: FunctionType,
        modifications: Sequence<Routine.Call<UtType, PythonFuzzedValue>>
    ): Seed.Recursive<UtType, PythonFuzzedValue> {
        val description = constructorFunction.pythonDescription() as PythonCallableTypeDescription
        val positionalArgs = description.argumentKinds.count { it == PythonCallableTypeDescription.ArgKind.ARG_POS }
        val arguments = constructorFunction.arguments.take(positionalArgs)
        val nonSelfArgs = arguments.drop(1)

        return Seed.Recursive(
            construct = Routine.Create(nonSelfArgs) { v ->
                PythonFuzzedValue(
                    PythonTree.ReduceNode(
                        PythonClassId(type.pythonModuleName(), type.pythonName()),
                        PythonClassId(type.pythonModuleName(), type.pythonName()),
                        v.map { it.tree },
                    ),
                    "%var% = ${type.pythonTypeRepresentation()}"
                )
            },
            modify = modifications,
            empty = Routine.Empty { PythonFuzzedValue(PythonTree.FakeNode) }
        )
    }

    private fun callConstructors(
        type: UtType,
        constructor: PythonDefinition,
        modifications: Sequence<Routine.Call<UtType, PythonFuzzedValue>>
    ): Sequence<Seed.Recursive<UtType, PythonFuzzedValue>> = sequence {
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