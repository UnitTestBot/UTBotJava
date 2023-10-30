package org.utbot.python.engine.fuzzing.typeinference

import org.utbot.python.PythonMethod
import org.utbot.python.newtyping.PythonCallableTypeDescription
import org.utbot.python.newtyping.PythonFuncItemDescription
import org.utbot.python.newtyping.PythonFunctionDefinition
import org.utbot.python.newtyping.PythonSubtypeChecker
import org.utbot.python.newtyping.PythonTypeHintsStorage
import org.utbot.python.newtyping.PythonTypeVarDescription
import org.utbot.python.newtyping.general.CompositeType
import org.utbot.python.newtyping.general.DefaultSubstitutionProvider
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.general.TypeParameter
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.newtyping.general.getBoundedParameters
import org.utbot.python.newtyping.general.hasBoundedParameters
import org.utbot.python.newtyping.getPythonAttributeByName
import org.utbot.python.newtyping.pythonAnyType
import org.utbot.python.newtyping.pythonDescription
import org.utbot.python.newtyping.pythonTypeRepresentation
import org.utbot.python.newtyping.utils.isRequired
import org.utbot.python.utils.PriorityCartesianProduct

private const val MAX_SUBSTITUTIONS = 10

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
    return PriorityCartesianProduct(params.map { getCandidates(it, typeStorage) }).getSequence().map { subst ->
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
            ?: method.definition.type
        val newFuncTypes = generateTypesAfterSubstitution(funcType, typeStorage)
        newFuncTypes.map { newFuncType ->
            val def = PythonFunctionDefinition(method.definition.meta, newFuncType as FunctionType)
            PythonMethod(
                method.name,
                method.moduleFilename,
                newClass as? CompositeType,
                method.codeAsString,
                def,
                method.ast
            )
        }
    }.take(MAX_SUBSTITUTIONS)
}

fun createShortForm(method: PythonMethod): Pair<PythonMethod, String>? {
    val meta = method.definition.type.pythonDescription() as PythonCallableTypeDescription
    val argKinds = meta.argumentKinds
    if (argKinds.any { !isRequired(it) }) {
        val originalDef = method.definition
        val shortType = meta.removeNotRequiredArgs(originalDef.type)
        val shortMeta = PythonFuncItemDescription(
            originalDef.meta.name,
            originalDef.meta.args.filterIndexed { index, _ -> isRequired(argKinds[index]) }
        )
        val additionalVars = originalDef.meta.args
            .filterIndexed { index, _ -> !isRequired(argKinds[index]) }
            .mapIndexed { index, arg ->
                "${arg.name}: ${method.argumentsWithoutSelf[index].annotation ?: pythonAnyType.pythonTypeRepresentation()}"
            }
            .joinToString(separator = "\n", prefix = "\n")
        val shortDef = PythonFunctionDefinition(shortMeta, shortType)
        val shortMethod = PythonMethod(
            method.name,
            method.moduleFilename,
            method.containingPythonClass,
            method.codeAsString,
            shortDef,
            method.ast
        )
        return Pair(shortMethod, additionalVars)
    }
    return null
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
            createShortForm(newMethod),
            (newMethod to "")
        )
    }.map {
        ModifiedAnnotation(it.first, it.second)
    }
}