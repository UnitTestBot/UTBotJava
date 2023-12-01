package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonBoolClassId
import org.utbot.python.framework.api.python.util.pythonBytearrayClassId
import org.utbot.python.framework.api.python.util.pythonBytesClassId
import org.utbot.python.framework.api.python.util.pythonComplexClassId
import org.utbot.python.framework.api.python.util.pythonDictClassId
import org.utbot.python.framework.api.python.util.pythonFloatClassId
import org.utbot.python.framework.api.python.util.pythonIntClassId
import org.utbot.python.framework.api.python.util.pythonListClassId
import org.utbot.python.framework.api.python.util.pythonObjectClassId
import org.utbot.python.framework.api.python.util.pythonRePatternClassId
import org.utbot.python.framework.api.python.util.pythonSetClassId
import org.utbot.python.framework.api.python.util.pythonStrClassId
import org.utbot.python.framework.api.python.util.pythonTupleClassId
import org.utbot.python.fuzzing.FuzzedUtType
import org.utbot.python.fuzzing.FuzzedUtType.Companion.activateAny
import org.utbot.python.fuzzing.FuzzedUtType.Companion.toFuzzed
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.PythonValueProvider
import org.utbot.python.fuzzing.provider.utils.isAny
import org.utbot.python.fuzzing.provider.utils.isCallable
import org.utbot.python.fuzzing.provider.utils.isConcreteType
import org.utbot.python.fuzzing.provider.utils.isMagic
import org.utbot.python.fuzzing.provider.utils.isPrivate
import org.utbot.python.fuzzing.provider.utils.isProperty
import org.utbot.python.fuzzing.provider.utils.isProtected
import org.utbot.python.newtyping.PythonCallableTypeDescription
import org.utbot.python.newtyping.PythonCompositeTypeDescription
import org.utbot.python.newtyping.PythonDefinition
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.getPythonAttributeByName
import org.utbot.python.newtyping.getPythonAttributes
import org.utbot.python.newtyping.pythonDescription
import org.utbot.python.newtyping.pythonName
import org.utbot.python.newtyping.pythonNoneType
import org.utbot.python.newtyping.pythonTypeName

object ReduceValueProvider : PythonValueProvider {
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
        pythonObjectClassId.canonicalName,
    )

    override fun accept(type: FuzzedUtType): Boolean {
        val hasSupportedType =
            !unsupportedTypes.contains(type.pythonTypeName())
        return hasSupportedType && isConcreteType(type.utType) && !type.isAny()
    }

    override fun generate(description: PythonMethodDescription, type: FuzzedUtType) = sequence {
        val fields = findFields(description, type)
        findConstructors(description, type)
            .forEach {
                val modifications = emptyList<Routine.Call<FuzzedUtType, PythonFuzzedValue>>().toMutableList()
                modifications.addAll(fields.map { field ->
                    Routine.Call(listOf(field.type).toFuzzed().activateAny()) { instance, arguments ->
                        val obj = instance.tree as PythonTree.ReduceNode
                        obj.state[field.meta.name] = arguments.first().tree
                    }
                })
                yieldAll(callConstructors(type, it, modifications.asSequence(), description))
            }
    }

    private fun findFields(description: PythonMethodDescription, type: FuzzedUtType): List<PythonDefinition> {
        // TODO: here we need to use same as .getPythonAttributeByName but without name
        // TODO: now we do not have fields from parents
        // TODO: here we should use only attributes from __slots__
        return type.utType.getPythonAttributes().filter { attr ->
            !attr.isMagic() && !attr.isProtected() && !attr.isPrivate() && !attr.isProperty() && !attr.isCallable(
                description.pythonTypeStorage
            )
        }
    }

    private fun findConstructors(description: PythonMethodDescription, type: FuzzedUtType): List<PythonDefinition> {
        val initMethodName = "__init__"
        val newMethodName = "__new__"
        val typeDescr = type.utType.pythonDescription()
        return if (typeDescr is PythonCompositeTypeDescription) {
            val mro = typeDescr.mro(description.pythonTypeStorage, type.utType)
            val initParent = mro.indexOfFirst { p -> p.getPythonAttributes().any { it.meta.name == initMethodName } }
            val newParent = mro.indexOfFirst { p -> p.getPythonAttributes().any { it.meta.name == newMethodName } }
            val initMethods = type.utType.getPythonAttributeByName(description.pythonTypeStorage, initMethodName)
            val newMethods = type.utType.getPythonAttributeByName(description.pythonTypeStorage, newMethodName)
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
        type: FuzzedUtType,
        constructorFunction: FunctionType,
        modifications: Sequence<Routine.Call<FuzzedUtType, PythonFuzzedValue>>,
        description: PythonMethodDescription,
    ): Seed.Recursive<FuzzedUtType, PythonFuzzedValue> {
        return Seed.Recursive(
            construct = buildConstructor(type, constructorFunction, description),
            modify = modifications,
            empty = Routine.Empty { PythonFuzzedValue(buildEmptyValue(type, description)) }
        )
    }

    private fun buildEmptyValue(
        type: FuzzedUtType,
        description: PythonMethodDescription,
    ): PythonTree.PythonTreeNode {
        val newMethodName = "__new__"
        val newMethod = type.utType.getPythonAttributeByName(description.pythonTypeStorage, newMethodName)
        return if (newMethod?.type?.parameters?.size == 1) {
            val classId = PythonClassId(type.pythonModuleName(), type.pythonName())
            PythonTree.ReduceNode(
                classId,
                PythonClassId(type.pythonModuleName(), "${type.pythonName()}.__new__"),
                listOf(PythonTree.PrimitiveNode(classId, classId.name)),
            )
        } else {
            PythonTree.FakeNode
        }
    }

    private fun buildConstructor(
        type: FuzzedUtType,
        constructorFunction: FunctionType,
        description: PythonMethodDescription,
    ): Routine.Create<FuzzedUtType, PythonFuzzedValue> {
        if (constructorFunction.pythonName().endsWith("__new__") && constructorFunction.arguments.any { it.isAny() }) {
            val newMethodName = "__new__"
            val newMethod = type.utType.getPythonAttributeByName(description.pythonTypeStorage, newMethodName)
            val newMethodArgs = (newMethod?.type as FunctionType?)?.arguments
            return if (newMethodArgs != null && newMethodArgs.size == 1) {
                val classId = PythonClassId(type.pythonModuleName(), type.pythonName())
                Routine.Create(newMethodArgs.toFuzzed().activateAny()) { v ->
                    PythonFuzzedValue(
                        PythonTree.ReduceNode(
                            classId,
                            PythonClassId(type.pythonModuleName(), "${type.pythonName()}.__new__"),
                            listOf(PythonTree.PrimitiveNode(classId, classId.name)),
                        ),
                        "%var% = ${type.pythonTypeRepresentation()}"
                    )
                }
            } else {
                val classId = PythonClassId(type.pythonModuleName(), type.pythonName())
                Routine.Create(listOf(pythonNoneType).toFuzzed()) { v ->
                    PythonFuzzedValue(
                        PythonTree.ReduceNode(
                            classId,
                            PythonClassId(pythonObjectClassId.name, "${type.pythonName()}.__new__"),
                            listOf(PythonTree.PrimitiveNode(classId, classId.name)),
                        ),
                        "%var% = ${type.pythonTypeRepresentation()}"
                    )
                }
            }
        } else {
            val typeDescription = constructorFunction.pythonDescription() as PythonCallableTypeDescription
            val positionalArgs = typeDescription.argumentKinds.count { it == PythonCallableTypeDescription.ArgKind.ARG_POS }
            val arguments = constructorFunction.arguments.take(positionalArgs)
            val nonSelfArgs = arguments.drop(1)
            return Routine.Create(nonSelfArgs.toFuzzed()) { v ->
                PythonFuzzedValue(
                    PythonTree.ReduceNode(
                        PythonClassId(type.pythonModuleName(), type.pythonName()),
                        PythonClassId(type.pythonModuleName(), type.pythonName()),
                        v.map { it.tree },
                    ),
                    "%var% = ${type.pythonTypeRepresentation()}"
                )
            }
        }
    }



    private fun callConstructors(
        type: FuzzedUtType,
        constructor: PythonDefinition,
        modifications: Sequence<Routine.Call<FuzzedUtType, PythonFuzzedValue>>,
        description: PythonMethodDescription,
    ): Sequence<Seed.Recursive<FuzzedUtType, PythonFuzzedValue>> = sequence {
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
            yield(constructObject(type, it, modifications, description))
        }
    }
}