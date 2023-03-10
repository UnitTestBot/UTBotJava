package org.utbot.python.fuzzing.provider.utils

import org.utbot.fuzzing.Seed
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.PythonAnyTypeDescription
import org.utbot.python.newtyping.PythonConcreteCompositeTypeDescription
import org.utbot.python.newtyping.PythonSubtypeChecker
import org.utbot.python.newtyping.general.Type

fun Type.isAny(): Boolean {
    return meta is PythonAnyTypeDescription
}

fun getSuitableConstantsFromCode(description: PythonMethodDescription, type: Type): List<Seed<Type, PythonFuzzedValue>> {
    return description.concreteValues.filter {
        PythonSubtypeChecker.checkIfRightIsSubtypeOfLeft(type, it.type, description.pythonTypeStorage)
    }.mapNotNull { value ->
        PythonTree.fromParsedConstant(Pair(value.type, value.value))?.let {
            Seed.Simple(PythonFuzzedValue(it))
        }
    }
}

fun isConcreteType(type: Type): Boolean {
    return (type.meta as? PythonConcreteCompositeTypeDescription)?.isAbstract == false
}