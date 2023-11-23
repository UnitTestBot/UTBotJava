package org.utbot.python

import org.parsers.python.ast.Block
import org.utbot.framework.plugin.api.UtClusterInfo
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.general.CompositeType
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.utils.isNamed
import org.utbot.python.newtyping.utils.isRequired

data class PythonArgument(
    val name: String,
    val annotation: String?,
    val isNamed: Boolean = false,
)

class PythonMethodHeader(
    val name: String,
    val moduleFilename: String,
    val containingPythonClassId: PythonClassId?,
    val decorators: List<PyDecorator> = emptyList(),
)


interface PythonMethod {
    val name: String
    val moduleFilename: String
    val containingPythonClass: CompositeType?
    val codeAsString: String
    val ast: Block

    fun methodSignature(): String
    val hasThisArgument: Boolean
    val arguments: List<PythonArgument>
    val argumentsWithoutSelf: List<PythonArgument>
    val thisObjectName: String?
    val argumentsNames: List<String>
    val argumentsNamesWithoutSelf: List<String>

    val methodType: FunctionType
    val methodMeta: PythonDefinitionDescription

    fun makeCopyWithNewType(newFunctionType: FunctionType): PythonMethod
    fun createShortForm(): Pair<PythonMethod, String>?
    fun changeDefinition(signature: FunctionType)

    fun renderMethodName(): String
}

class PythonBaseMethod(
    override val name: String,
    override val moduleFilename: String,
    override val containingPythonClass: CompositeType?,
    override val codeAsString: String,
    private var definition: PythonFunctionDefinition,
    override val ast: Block
) : PythonMethod {
    override fun methodSignature(): String = "$name(" + arguments.joinToString(", ") {
        "${it.name}: ${it.annotation ?: pythonAnyClassId.name}"
    } + ")"

    /*
    Check that the first argument is `self` of `cls`.
    TODO: We should support `@property` decorator
     */
    override val hasThisArgument: Boolean
        get() = containingPythonClass != null && definition.meta.args.any { it.isSelf }

    override val arguments: List<PythonArgument>
        get() {
                val meta = definition.type.pythonDescription() as PythonCallableTypeDescription
                return (definition.type.arguments).mapIndexed { index, type ->
                    PythonArgument(
                        meta.argumentNames[index]!!,
                        type.pythonTypeRepresentation(),  // TODO: improve pythonTypeRepresentation
                        isNamed(meta.argumentKinds[index])
                    )
                }
            }

    override val argumentsWithoutSelf: List<PythonArgument>
        get() = if (hasThisArgument) arguments.drop(1) else arguments

    override val thisObjectName: String?
        get() = if (hasThisArgument) arguments[0].name else null

    override val argumentsNames: List<String>
        get() = arguments.map { it.name }

    override val argumentsNamesWithoutSelf: List<String>
        get() = argumentsNames.drop(if (hasThisArgument) 1 else 0)

    override val methodType: FunctionType = definition.type

    override val methodMeta: PythonDefinitionDescription = definition.meta

    override fun makeCopyWithNewType(newFunctionType: FunctionType): PythonMethod {
        val newDefinition = PythonFunctionDefinition(definition.meta, newFunctionType)
        return PythonBaseMethod(name, moduleFilename, containingPythonClass, codeAsString, newDefinition, ast)
    }

    override fun createShortForm(): Pair<PythonBaseMethod, String>? {
        val meta = methodType.pythonDescription() as PythonCallableTypeDescription
        val argKinds = meta.argumentKinds
        if (argKinds.any { !isRequired(it) }) {
            val originalDef = definition
            val shortType = meta.removeNotRequiredArgs(originalDef.type)
            val shortMeta = PythonFuncItemDescription(
                originalDef.meta.name,
                originalDef.meta.args.filterIndexed { index, _ -> isRequired(argKinds[index]) }
            )
            val additionalVars = originalDef.meta.args
                .filterIndexed { index, _ -> !isRequired(argKinds[index]) }
                .mapIndexed { index, arg ->
                    "${arg.name}: ${argumentsWithoutSelf[index].annotation ?: pythonAnyType.pythonTypeRepresentation()}"
                }
                .joinToString(separator = "\n", prefix = "\n")
            val shortDef = PythonFunctionDefinition(shortMeta, shortType)
            val shortMethod = PythonBaseMethod(
                name,
                moduleFilename,
                containingPythonClass,
                codeAsString,
                shortDef,
                ast
            )
            return Pair(shortMethod, additionalVars)
        }
        return null
    }

    fun changeDefinition(newDefinition: PythonDefinition) {
        require(newDefinition is PythonFunctionDefinition)
        definition = newDefinition
    }

    override fun changeDefinition(signature: FunctionType) {
        val newDefinition = PythonFunctionDefinition(
            definition.meta,
            signature
        )
        changeDefinition(newDefinition)
    }

    override fun renderMethodName(): String {
        return name
    }
}

class PythonDecoratedMethod(
    override val name: String,
    override val moduleFilename: String,
    override val containingPythonClass: CompositeType?,
    override val codeAsString: String,
    private var definition: PythonDefinition,
    override val ast: Block,
    val decorator: PyDecorator,
) : PythonMethod {
    override val methodType: FunctionType = definition.type as FunctionType
    override val methodMeta: PythonDefinitionDescription = definition.meta
    val typeMeta: PythonCallableTypeDescription = definition.type.pythonDescription() as PythonCallableTypeDescription

    fun changeDefinition(newDefinition: PythonDefinition) {
        require(checkDefinition(newDefinition)) { error("Cannot test non-function object") }
        definition = newDefinition
    }

    override fun changeDefinition(signature: FunctionType) {
        val newDefinition = PythonDefinition(
            methodMeta,
            signature
        )
        checkDefinition(newDefinition)
        changeDefinition(newDefinition)
    }

    override fun renderMethodName(): String {
        return decorator.generateCallableName(this)
    }

    override fun makeCopyWithNewType(newFunctionType: FunctionType): PythonMethod {
        val newDefinition = PythonDefinition(methodMeta, newFunctionType)
        return PythonDecoratedMethod(
            name, moduleFilename, containingPythonClass, codeAsString, newDefinition, ast, decorator
        )
    }

    override fun createShortForm(): Pair<PythonBaseMethod, String>? = null

    init {
        assert(checkDefinition(definition)) { error("Cannot test non-function object") }
    }
    override fun methodSignature(): String = "${decorator.generateCallableName(this)}(" + arguments.joinToString(", ") {
        "${it.name}: ${it.annotation ?: pythonAnyClassId.name}"
    } + ")"

    /*
    Check that the first argument is `self` of `cls`.
     */
    override val hasThisArgument: Boolean
        get() = containingPythonClass != null && decorator.hasSelfArgument()

    override val arguments: List<PythonArgument>
        get() {
            return (methodType.arguments).mapIndexed { index, type ->
                PythonArgument(
                    typeMeta.argumentNames[index] ?: "arg$index",
                    type.pythonTypeRepresentation(),  // TODO: improve pythonTypeRepresentation
                    isNamed(typeMeta.argumentKinds[index])
                )
            }
        }

    override val argumentsWithoutSelf: List<PythonArgument>
        get() = if (hasThisArgument) arguments.drop(1) else arguments

    override val thisObjectName: String?
        get() = if (hasThisArgument) arguments[0].name else null

    override val argumentsNames: List<String>
        get() = arguments.map { it.name }

    override val argumentsNamesWithoutSelf: List<String>
        get() = argumentsNames.drop(if (hasThisArgument) 1 else 0)

    companion object {
        fun checkDefinition(definition: PythonDefinition): Boolean {
            val type = definition.type
            val meta = definition.type.pythonDescription()
            return type is FunctionType && meta is PythonCallableTypeDescription
        }
    }
}

data class PythonTestSet(
    val method: PythonMethod,
    val executions: List<UtExecution>,
    val errors: List<UtError>,
    val executionsNumber: Int = 0,
    val clustersInfo: List<Pair<UtClusterInfo?, IntRange>> = listOf(null to executions.indices)
)

data class FunctionArguments(
    val thisObject: PythonTreeModel?,
    val thisObjectName: String?,
    val arguments: List<PythonTreeModel>,
    val names: List<String?>,
) {
    val allArguments: List<PythonTreeModel> = (listOf(thisObject) + arguments).filterNotNull()
}

sealed interface PyDecorator {
    fun generateCallableName(method: PythonMethod, baseName: String? = null): String
    fun hasSelfArgument(): Boolean
    val type: PythonClassId

    object StaticMethod : PyDecorator {
        override fun generateCallableName(method: PythonMethod, baseName: String?) =
            "${method.containingPythonClass!!.pythonName()}.${method.name}"

        override fun hasSelfArgument() = false

        override val type: PythonClassId = PythonClassId("staticmethod")
    }

    object ClassMethod : PyDecorator {
        override fun generateCallableName(method: PythonMethod, baseName: String?) =
            "${method.containingPythonClass!!.pythonName()}.${method.name}"

        override fun hasSelfArgument() = true

        override val type: PythonClassId = PythonClassId("classmethod")
    }

    class UnknownDecorator(
        override val type: PythonClassId,
    ) : PyDecorator {
        override fun generateCallableName(method: PythonMethod, baseName: String?) =
            baseName ?: method.name

        override fun hasSelfArgument() = true
    }

    companion object {
        fun decoratorByName(decoratorName: String): PyDecorator {
            return when (decoratorName) {
                "classmethod" -> ClassMethod
                "staticmethod" -> StaticMethod
                else -> UnknownDecorator(PythonClassId(decoratorName))
            }
        }
    }
}