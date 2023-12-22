package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.fuzzing.FuzzedUtType
import org.utbot.python.fuzzing.FuzzedUtType.Companion.activateAnyIf
import org.utbot.python.fuzzing.FuzzedUtType.Companion.toFuzzed
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.PythonValueProvider
import org.utbot.python.fuzzing.provider.utils.isConcreteType
import org.utbot.python.newtyping.PythonConcreteCompositeTypeDescription
import org.utbot.python.newtyping.PythonProtocolDescription
import org.utbot.python.newtyping.PythonSubtypeChecker.Companion.checkIfRightIsSubtypeOfLeft
import org.utbot.python.newtyping.PythonTypeHintsStorage
import org.utbot.python.newtyping.general.DefaultSubstitutionProvider
import org.utbot.python.newtyping.pythonAnyType
import org.utbot.python.newtyping.pythonDescription

class SubtypeValueProvider(
    private val typeStorage: PythonTypeHintsStorage
) : PythonValueProvider {
    override fun accept(type: FuzzedUtType): Boolean {
        return type.utType.meta is PythonProtocolDescription ||
                ((type.utType.meta as? PythonConcreteCompositeTypeDescription)?.isAbstract == true)
    }

    private val concreteTypes = typeStorage.simpleTypes.filter {
        isConcreteType(it) && it.pythonDescription().name.name.first() != '_'  // Don't substitute private classes
    }.map {
        DefaultSubstitutionProvider.substituteAll(it, it.parameters.map { pythonAnyType })
    }

    override fun generate(description: PythonMethodDescription, type: FuzzedUtType) = sequence {
        val subtypes = concreteTypes.filter { checkIfRightIsSubtypeOfLeft(type.utType, it, typeStorage) }
        subtypes.forEach { subtype ->
            yield(
                Seed.Recursive(
                    construct = Routine.Create(listOf(subtype).toFuzzed().activateAnyIf(type)) { v -> v.first() },
                    empty = Routine.Empty { PythonFuzzedValue(PythonTree.FakeNode) }
                ))
        }
    }
}