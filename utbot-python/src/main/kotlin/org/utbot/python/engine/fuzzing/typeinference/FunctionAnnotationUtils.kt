package org.utbot.python.engine.fuzzing.typeinference

import org.utbot.python.PythonMethod
import org.utbot.python.newtyping.PythonSubtypeChecker
import org.utbot.python.newtyping.PythonTypeHintsStorage
import org.utbot.python.newtyping.PythonTypeVarDescription
import org.utbot.python.newtyping.general.DefaultSubstitutionProvider
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.general.TypeParameter
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.newtyping.general.getBoundedParameters
import org.utbot.python.newtyping.general.hasBoundedParameters
import org.utbot.python.newtyping.getPythonAttributeByName
import org.utbot.python.newtyping.pythonDescription
import org.utbot.python.newtyping.pythonTypeName
import org.utbot.python.utils.PriorityCartesianProduct

private const val MAX_SUBSTITUTIONS = 10
private val BAD_TYPES = setOf(
    "builtins.function",
    "builtins.super",
    "builtins.type",
    "builtins.slice",
)

fun getCandidates(param: TypeParameter, typeStorage: PythonTypeHintsStorage): List<UtType> {
    val meta = param.pythonDescription() as PythonTypeVarDescription
    return when (meta.parameterKind) {
        PythonTypeVarDescription.ParameterKind.WithConcreteValues -> {
            param.constraints.map { it.boundary }
        }

        PythonTypeVarDescription.ParameterKind.WithUpperBound -> {
            typeStorage.simpleTypes.filter {
                if (it.hasBoundedParameters())
                    return@filter false
                val bound = param.constraints.first().boundary
                PythonSubtypeChecker.checkIfRightIsSubtypeOfLeft(bound, it, typeStorage)
            }
        }
    }
}

fun generateTypesAfterSubstitution(type: UtType, typeStorage: PythonTypeHintsStorage): List<UtType> {
    val params = type.getBoundedParameters()
    return PriorityCartesianProduct(params.map { getCandidates(it, typeStorage) }).getSequence()
        .filter { it.all { it.pythonTypeName() !in BAD_TYPES } }
        .map { subst ->
            DefaultSubstitutionProvider.substitute(type, (params zip subst).associate { it })
        }.take(MAX_SUBSTITUTIONS).toList()
}

fun substituteTypeParameters(
    method: PythonMethod,
    typeStorage: PythonTypeHintsStorage,
): List<PythonMethod> {
    val newClasses = method.containingPythonClass?.let {
        generateTypesAfterSubstitution(it, typeStorage)
    } ?: listOf(null)
    return newClasses.flatMap { newClass ->
        val funcType = newClass?.getPythonAttributeByName(typeStorage, method.name)?.type as? FunctionType
            ?: method.methodType
        val newFuncTypes = generateTypesAfterSubstitution(funcType, typeStorage)
        newFuncTypes.map { newFuncType ->
            method.makeCopyWithNewType(newFuncType as FunctionType)
        }
    }.take(MAX_SUBSTITUTIONS)
}


data class ModifiedAnnotation(
    val method: PythonMethod,
    val additionalVars: String
)

fun createMethodAnnotationModifications(
    method: PythonMethod,
    typeStorage: PythonTypeHintsStorage,
): List<ModifiedAnnotation> {
    return substituteTypeParameters(method, typeStorage).flatMap { newMethod ->
        listOfNotNull(
            newMethod.createShortForm(),
            (newMethod to "")
        )
    }.map {
        ModifiedAnnotation(it.first, it.second)
    }
}