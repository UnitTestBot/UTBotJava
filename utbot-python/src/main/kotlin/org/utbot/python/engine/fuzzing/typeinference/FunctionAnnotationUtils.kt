package org.utbot.python.engine.fuzzing.typeinference

import org.utbot.python.PythonMethod
import org.utpython.types.PythonSubtypeChecker
import org.utpython.types.PythonTypeHintsStorage
import org.utpython.types.PythonTypeVarDescription
import org.utpython.types.general.DefaultSubstitutionProvider
import org.utpython.types.general.FunctionType
import org.utpython.types.general.TypeParameter
import org.utpython.types.general.UtType
import org.utpython.types.general.getBoundedParameters
import org.utpython.types.general.hasBoundedParameters
import org.utpython.types.getPythonAttributeByName
import org.utpython.types.pythonDescription
import org.utpython.types.pythonTypeName
import org.utbot.python.utils.PriorityCartesianProduct

private const val MAX_SUBSTITUTIONS = 10
private val BAD_TYPES = setOf(
    "builtins.function",
    "builtins.super",
    "builtins.type",
    "builtins.slice",
    "builtins.range",
    "builtins.memoryview",
    "builtins.object",
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
        .filter { types -> types.all { it.pythonTypeName() !in BAD_TYPES } }
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