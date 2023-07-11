/**
 * Brand new model based API.
 *
 * Contains models for everything: primitives, composite objects, arrays, lists, mocks.
 *
 * Note: enum value, class reference and some of concrete objects represented as UtSpecialModel.
 */

package org.utbot.framework.plugin.api

import mu.KotlinLogging
import org.utbot.common.FileUtil
import org.utbot.common.isDefaultValue
import org.utbot.common.withToStringThreadLocalReentrancyGuard
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.util.ModifierFactory
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.byteClassId
import org.utbot.framework.plugin.api.util.charClassId
import org.utbot.framework.plugin.api.util.constructor
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.floatClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.framework.plugin.api.util.isPrimitive
import org.utbot.framework.plugin.api.util.isStatic
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.jField
import org.utbot.framework.plugin.api.util.kClass
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.framework.plugin.api.util.method
import org.utbot.framework.plugin.api.util.primitiveTypeJvmNameOrNull
import org.utbot.framework.plugin.api.util.safeJField
import org.utbot.framework.plugin.api.util.shortClassId
import org.utbot.framework.plugin.api.util.supertypeOfAnonymousClass
import org.utbot.framework.plugin.api.util.toReferenceTypeBytecodeSignature
import org.utbot.framework.plugin.api.util.voidClassId
import soot.ArrayType
import soot.BooleanType
import soot.ByteType
import soot.CharType
import soot.DoubleType
import soot.FloatType
import soot.IntType
import soot.LongType
import soot.RefType
import soot.ShortType
import soot.SootClass
import soot.Type
import soot.VoidType
import soot.jimple.JimpleBody
import soot.jimple.Stmt
import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import org.utbot.common.isAbstract
import org.utbot.common.isStatic
import org.utbot.framework.isFromTrustedLibrary
import org.utbot.framework.plugin.api.TypeReplacementMode.*
import org.utbot.framework.plugin.api.util.SpringModelUtils
import org.utbot.framework.plugin.api.util.allDeclaredFieldIds
import org.utbot.framework.plugin.api.util.allSuperTypes
import org.utbot.framework.plugin.api.util.fieldId
import org.utbot.framework.plugin.api.util.isSubtypeOf
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.framework.process.OpenModulesContainer
import soot.SootField
import soot.SootMethod

const val SYMBOLIC_NULL_ADDR: Int = 0

data class UtMethodTestSet(
    val method: ExecutableId,
    val executions: List<UtExecution> = emptyList(),
    // in idea process this property is null
    val jimpleBody: JimpleBody? = null,
    val errors: Map<String, Int> = emptyMap(),
    val clustersInfo: List<Pair<UtClusterInfo?, IntRange>> = listOf(null to executions.indices)
)

data class Step(
    val stmt: Stmt,
    val depth: Int,
    val decision: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Step

        if (stmt != other.stmt) return false
        if (decision != other.decision) return false

        return true
    }

    override fun hashCode(): Int {
        var result = stmt.hashCode()
        result = 31 * result + decision
        return result
    }
}

/**
 * One symbolic step.
 *
 * @see UtSymbolicExecution.symbolicSteps
 */
data class SymbolicStep(
    val method: SootMethod,
    val lineNumber: Int,
    val callDepth: Int,
)


/**
 * Traverse result.
 *
 * Could be execution with parameters and result, or error.
 */
sealed class UtResult


/**
 * Execution.
 *
 * Contains:
 * - execution parameters, including thisInstance;
 * - result;
 * - coverage information (instructions) if this execution was obtained from the concrete execution.
 * - comments, method names and display names created by utbot-summary module.
 */
abstract class UtExecution(
    val stateBefore: EnvironmentModels,
    val stateAfter: EnvironmentModels,
    val result: UtExecutionResult,
    val coverage: Coverage? = null,
    var summary: List<DocStatement>? = null,
    var testMethodName: String? = null,
    var displayName: String? = null
) : UtResult() {
    abstract fun copy(
        stateBefore: EnvironmentModels = this.stateBefore,
        stateAfter: EnvironmentModels = this.stateAfter,
        result: UtExecutionResult = this.result,
        coverage: Coverage? = this.coverage,
        summary: List<DocStatement>? = this.summary,
        testMethodName: String? = this.testMethodName,
        displayName: String? = this.displayName,
    ): UtExecution
}

/**
 * Symbolic execution.
 *
 * Contains:
 * - execution parameters, including thisInstance;
 * - result;
 * - static fields changed during execution;
 * - required instrumentation details (such as randoms, time, static methods).
 * - coverage information (instructions) if this execution was obtained from the concrete execution.
 * - comments, method names and display names created by utbot-summary module.
 */
class UtSymbolicExecution(
    stateBefore: EnvironmentModels,
    stateAfter: EnvironmentModels,
    result: UtExecutionResult,
    val instrumentation: List<UtInstrumentation>,
    val path: MutableList<Step>,
    val fullPath: List<Step>,
    coverage: Coverage? = null,
    summary: List<DocStatement>? = null,
    testMethodName: String? = null,
    displayName: String? = null,
    /** Convenient view of the full symbolic path */ val symbolicSteps: List<SymbolicStep> = listOf(),
) : UtExecution(stateBefore, stateAfter, result, coverage, summary, testMethodName, displayName) {
    /**
     * By design the 'before' and 'after' states contain info about the same fields.
     * It means that it is not possible for a field to be present at 'before' and to be absent at 'after'.
     * The reverse is also impossible.
     */
    val staticFields: Set<FieldId>
        get() = stateBefore.statics.keys

    var containsMocking: Boolean = false

    override fun copy(
        stateBefore: EnvironmentModels,
        stateAfter: EnvironmentModels,
        result: UtExecutionResult,
        coverage: Coverage?,
        summary: List<DocStatement>?,
        testMethodName: String?,
        displayName: String?
    ): UtExecution = UtSymbolicExecution(
        stateBefore = stateBefore,
        stateAfter = stateAfter,
        result = result,
        instrumentation = instrumentation,
        path = path,
        fullPath = fullPath,
        coverage = coverage,
        summary = summary,
        testMethodName = testMethodName,
        displayName = displayName
    )

    override fun toString(): String = buildString {
        append("UtSymbolicExecution(")
        appendLine()

        append("<State before>:")
        appendLine()
        append(stateBefore)
        appendLine()

        append("<State after>:")
        appendLine()
        append(stateAfter)
        appendLine()

        append("<Result>:")
        appendLine()
        append(result)
        appendLine()

        appendOptional("instrumentation", instrumentation)
        append(")")
    }

    fun copy(
        stateBefore: EnvironmentModels = this.stateBefore,
        stateAfter: EnvironmentModels = this.stateAfter,
        result: UtExecutionResult = this.result,
        coverage: Coverage? = this.coverage,
        summary: List<DocStatement>? = this.summary,
        testMethodName: String? = this.testMethodName,
        displayName: String? = this.displayName,
        instrumentation: List<UtInstrumentation> = this.instrumentation,
        path: MutableList<Step> = this.path,
        fullPath: List<Step> = this.fullPath
    ): UtExecution = UtSymbolicExecution(
        stateBefore = stateBefore,
        stateAfter = stateAfter,
        result = result,
        instrumentation = instrumentation,
        path = path,
        fullPath = fullPath,
        coverage = coverage,
        summary = summary,
        testMethodName = testMethodName,
        displayName = displayName
    )
}

/**
 * Execution that result in an error (e.g., JVM crash or another concrete execution error).
 *
 * Contains:
 * - state before the execution;
 * - result (a [UtExecutionFailure] or its subclass);
 * - coverage information (instructions) if this execution was obtained from the concrete execution.
 * - comments, method names and display names created by utbot-summary module.
 *
 * This execution does not contain any "after" state, as it is generally impossible to obtain
 * in case of failure. [MissingState] is used instead.
 */
class UtFailedExecution(
    stateBefore: EnvironmentModels,
    result: UtExecutionResult,
    coverage: Coverage? = null,
    summary: List<DocStatement>? = null,
    testMethodName: String? = null,
    displayName: String? = null
) : UtExecution(stateBefore, MissingState, result, coverage, summary, testMethodName, displayName) {
    override fun copy(
        stateBefore: EnvironmentModels,
        stateAfter: EnvironmentModels,
        result: UtExecutionResult,
        coverage: Coverage?,
        summary: List<DocStatement>?,
        testMethodName: String?,
        displayName: String?
    ): UtExecution {
        return UtFailedExecution(
            stateBefore,
            result,
            coverage,
            summary,
            testMethodName,
            displayName
        )
    }
}

open class EnvironmentModels(
    val thisInstance: UtModel?,
    val parameters: List<UtModel>,
    val statics: Map<FieldId, UtModel>
) {
    override fun toString() = buildString {
        append("this=$thisInstance")
        appendOptional("parameters", parameters)
        appendOptional("statics", statics)
    }

    operator fun component1(): UtModel? = thisInstance
    operator fun component2(): List<UtModel> = parameters
    operator fun component3(): Map<FieldId, UtModel> = statics

    fun copy(
        thisInstance: UtModel? = this.thisInstance,
        parameters: List<UtModel> = this.parameters,
        statics: Map<FieldId, UtModel> = this.statics
    ) = EnvironmentModels(thisInstance, parameters, statics)
}

/**
 * Represents missing state. Useful for [UtConcreteExecutionFailure] because it does not have [UtSymbolicExecution.stateAfter]
 */
object MissingState : EnvironmentModels(
    thisInstance = null,
    parameters = emptyList(),
    statics = emptyMap()
)

/**
 * Error happened during test cases generation.
 */
data class UtError(
    val description: String,
    val error: Throwable
) : UtResult()

/**
 * Parent class for all models, contains class id.
 *
 * UtNullModel represents nulls, other models represent not-nullable entities.
 */
open class UtModel(
    open val classId: ClassId
)

/**
 * Class representing models for values that might have an address.
 *
 * @param [id] is a unique identifier of the object this model representing. If two models have the same [id], it means
 * that they represent the same object (for example, its initial and final states).
 * It is null if the model represents something that doesn't have an address (i.e. mock of the static primitive field)
 *
 * @param [modelName] is a name used for pretty implementation of toString methods
 */
sealed class UtReferenceModel(
    open val id: Int?,
    classId: ClassId,
    open val modelName: String = id.toString()
) : UtModel(classId)

/**
 * Checks if [UtModel] is a [UtNullModel].
 */
fun UtModel.isNull() = this is UtNullModel

/**
 * Checks if [UtModel] is not a [UtNullModel].
 */
fun UtModel.isNotNull() = !isNull()

/**
 * Checks if [UtModel] represents a default value of it's class
 * and does not require assembling so on.
 */
fun UtModel.hasDefaultValue() =
    isNull() || this is UtPrimitiveModel && value.isDefaultValue()

/**
 * Checks if [UtModel] is a mock.
 */
fun UtModel.isMockModel() = this is UtCompositeModel && isMock

/**
 * Get model id (symbolic null value for UtNullModel)
 * or null if model has no id (e.g., a primitive model) or the id is null.
 */
fun UtModel.idOrNull(): Int? = when (this) {
    is UtNullModel -> SYMBOLIC_NULL_ADDR
    is UtReferenceModel -> id
    else -> null
}

/**
 * Returns the model id if it is available, or throws an [IllegalStateException].
 */
@OptIn(ExperimentalContracts::class)
fun UtModel?.getIdOrThrow(): Int {
    contract {
        returns() implies (this@getIdOrThrow != null)
    }
    return this?.idOrNull() ?: throw IllegalStateException("Model id must not be null: $this")
}

/**
 * Model for nulls.
 */
data class UtNullModel(
    override val classId: ClassId
) : UtModel(classId) {
    override fun toString() = "null"
}

/**
 * Model for primitive value, string literal, void (Unit).
 */
data class UtPrimitiveModel(
    val value: Any,
) : UtModel(primitiveModelValueToClassId(value)) {
    override fun toString() = "$value"
}

/**
 * Constructs [ClassId] by [value] of [UtPrimitiveModel].
 *
 * Note: Java wrappers are used for primitives
 */
fun primitiveModelValueToClassId(value: Any) = when (value) {
    is Byte -> java.lang.Byte.TYPE.id
    is Short -> java.lang.Short.TYPE.id
    is Char -> Character.TYPE.id
    is Int -> Integer.TYPE.id
    is Long -> java.lang.Long.TYPE.id
    is Float -> java.lang.Float.TYPE.id
    is Double -> java.lang.Double.TYPE.id
    is Boolean -> java.lang.Boolean.TYPE.id
    else -> value.javaClass.id
}

object UtVoidModel : UtModel(voidClassId)

/**
 * Model for enum constant
 */
data class UtEnumConstantModel(
    override val id: Int,
    override val classId: ClassId,
    val value: Enum<*>
) : UtReferenceModel(id, classId) {
    // Model id is included for debugging purposes
    override fun toString(): String = "$value@$id"
}

/**
 * Model for class reference
 */
data class UtClassRefModel(
    override val id: Int,
    override val classId: ClassId,
    val value: ClassId
) : UtReferenceModel(id, classId) {
    // Model id is included for debugging purposes
    override fun toString(): String = "$value@$id"
}

/**
 * Model for composite object or mock.
 *
 * Contains:
 * - isMock flag
 * - calculated field values (models)
 * - mocks for methods with return values
 *
 * [fields] contains non-static fields
 */
data class UtCompositeModel(
    override val id: Int?,
    override val classId: ClassId,
    val isMock: Boolean,
    val fields: MutableMap<FieldId, UtModel> = mutableMapOf(),
    val mocks: MutableMap<ExecutableId, List<UtModel>> = mutableMapOf(),
) : UtReferenceModel(id, classId) {
    //TODO: SAT-891 - rewrite toString() method
    override fun toString() = withToStringThreadLocalReentrancyGuard {
        buildString {
            append("$classId{")
            if (isMock) {
                append("MOCK")
            }
            if (fields.isNotEmpty()) {
                append(" ")
                append(fields.entries.joinToString(", ", "{", "}") { (field, value) ->
                    if (value.classId != classId || value.isNull()) "(${field.declaringClass}) ${field.name}: $value" else "${field.name}: not evaluated"
                }) // TODO: here we can get an infinite recursion if we have cyclic dependencies.
            }
            if (mocks.isNotEmpty()) {
                append(", ")
                append(mocks.entries.joinToString(", ", "{", "}") { (method, values) ->
                    "${method.name}() -> ${prettify(values)}"
                })
            }
            append("}")
        }
    }

    /**
     * Prettifies value list by not using brackets for single value.
     */
    private fun prettify(list: List<Any>) = if (list.size == 1) {
        list[0]
    } else {
        list
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtCompositeModel

        return id == other.id
    }

    override fun hashCode(): Int {
        var result = id ?: 0
        result = 31 * result + classId.hashCode()
        return result
    }
}

/**
 * Model for array.
 *
 * Contains const value and stores, which in summary represent all values in array.
 */

data class UtArrayModel(
    override val id: Int,
    override val classId: ClassId,
    val length: Int = 0,
    var constModel: UtModel,
    val stores: MutableMap<Int, UtModel>
) : UtReferenceModel(id, classId) {
    override fun toString() = withToStringThreadLocalReentrancyGuard {
        (0 until length).map { stores[it] ?: constModel }.joinToString(", ", "[", "]")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtArrayModel

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id
}

/**
 * Model for complex objects with assemble instructions.
 *
 * The default constructor is made private to enforce using a safe constructor.
 *
 * @param instantiationCall is an [UtExecutableCallModel] to instantiate represented object. It **must** not return `null`.
 * @param modificationsChain is a chain of [UtStatementModel] to construct object state.
 */
data class UtAssembleModel private constructor(
    override val id: Int?,
    override val classId: ClassId,
    override val modelName: String,
    val instantiationCall: UtStatementCallModel,
    val modificationsChain: List<UtStatementModel>,
    val origin: UtCompositeModel?
) : UtReferenceModel(id, classId, modelName) {

    /**
     * Creates a new [UtAssembleModel].
     *
     * Please note, that it's the caller responsibility to properly cache [UtModel]s to prevent an infinite recursion.
     * The order of the calling:
     * 1. [instantiationCall]
     * 2. [constructor]
     * 3. [modificationsChainProvider]. Possible caching should be made at the beginning of this method.
     *
     * @param instantiationCall defines the single instruction, which provides a [UtAssembleModel]. It could be a
     * constructor or a method of another class, which returns the object of the [classId] type.
     *
     * @param modificationsChainProvider used for creating modifying statements. Its receiver corresponds to newly
     * created [UtAssembleModel], so you can use it for caching and for creating [UtExecutableCallModel]s with it
     * as [UtExecutableCallModel.instance].
     */
    constructor(
        id: Int?,
        classId: ClassId,
        modelName: String,
        instantiationCall: UtStatementCallModel,
        origin: UtCompositeModel? = null,
        modificationsChainProvider: UtAssembleModel.() -> List<UtStatementModel> = { emptyList() }
    ) : this(id, classId, modelName, instantiationCall, mutableListOf(), origin) {
        val modificationChainStatements = modificationsChainProvider()
        (modificationsChain as MutableList<UtStatementModel>).addAll(modificationChainStatements)
    }

    override fun toString() = withToStringThreadLocalReentrancyGuard {
        buildString {
            append("UtAssembleModel(${classId.simpleName} $modelName) ")
            append(instantiationCall)
            if (modificationsChain.isNotEmpty()) {
                append(" ")
                append(modificationsChain.joinToString(" "))
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtAssembleModel
        return id == other.id
    }

    override fun hashCode(): Int = id ?: 0
}

/**
 * Model for lambdas.
 *
 * Lambdas in Java represent the implementation of a single abstract method (SAM) of a functional interface.
 * They can be used to create an instance of said functional interface, but **they are not classes**.
 * In Java lambdas are compiled into synthetic methods of a class they are declared in.
 * Depending on the captured variables, this method will be either static or non-static.
 *
 * Since lambdas are not classes we cannot use a class loader to get info about them as we can do for other models.
 * Hence, the necessity for this specific lambda model that will be processed differently:
 * instead of working with a class we will be working with the synthetic method that represents our lambda.
 *
 * @property id see documentation on [UtReferenceModel.id]
 * @property samType the type of functional interface that this lambda will be used for (e.g. [java.util.function.Predicate]).
 * `sam` means single abstract method. See https://kotlinlang.org/docs/fun-interfaces.html for more details about it in Kotlin.
 * In Java it means the same.
 * @property declaringClass a class where the lambda is located.
 * We need this class, because the synthetic method the lambda is compiled into will be located in it.
 * @property lambdaName the name of synthetic method the lambda is compiled into.
 * We need it to find this method in the [declaringClass]
 * @property capturedValues models of values captured by lambda.
 * Lambdas can capture local variables, method arguments, static and non-static fields.
 */
// TODO: what about support for Kotlin lambdas and function types? See https://github.com/UnitTestBot/UTBotJava/issues/852
class UtLambdaModel(
    override val id: Int,
    val samType: ClassId,
    val declaringClass: ClassId,
    val lambdaName: String,
    val capturedValues: MutableList<UtModel> = mutableListOf(),
) : UtReferenceModel(id, samType) {

    val isFake: Boolean = lambdaName == fakeName

    val lambdaMethodId: MethodId
        get() {
            if (isFake) {
                val targetMethod = samType.jClass.declaredMethods.single { it.isAbstract }
                return object : MethodId(
                    declaringClass,
                    fakeName,
                    targetMethod.returnType.id,
                    targetMethod.parameterTypes.map { it.id }
                ) {
                    override val modifiers: Int = ModifierFactory.invoke {
                        public = true
                        static = true
                        final = true
                    }
                }
            }
            return declaringClass.jClass
                .declaredMethods
                .singleOrNull { it.name == lambdaName }
                ?.executableId // synthetic lambda methods should not have overloads, so we always expect there to be only one method with the given name
                ?: error("More than one method with name $lambdaName found in class: ${declaringClass.canonicalName}")
        }

    override fun toString(): String = "Anonymous function $lambdaName implementing functional interface $declaringClass"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtLambdaModel

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id

    companion object {
        private const val fakeName = "<FAKE>"

        /**
         * Create a non-existent lambda with fake method.
         *
         * That's temporary solution for building lambdas from concrete values.
         */
        fun createFake(id: Int, samType: ClassId, declaringClass: ClassId) =
            UtLambdaModel(id, samType, declaringClass, fakeName)
    }
}

object UtSpringContextModel : UtReferenceModel(
    id = null,
    classId = SpringModelUtils.applicationContextClassId,
    modelName = "applicationContext"
) {
    // NOTE that overriding equals is required just because without it
    // we will lose equality for objects after deserialization
    override fun equals(other: Any?): Boolean = other is UtSpringContextModel

    override fun hashCode(): Int = 0
}

data class SpringRepositoryId(
    val repositoryBeanName: String,
    val repositoryClassId: ClassId,
    val entityClassId: ClassId,
)

/**
 * Model for a step to obtain [UtAssembleModel].
 */
sealed class UtStatementModel(
    open val instance: UtReferenceModel?,
)

/**
 * Step of assemble instruction that calls executable or accesses the field.
 */
sealed class UtStatementCallModel(
    override val instance: UtReferenceModel?,
    open val statement: StatementId,
    open val params: List<UtModel>,
): UtStatementModel(instance) {
    override fun toString() = withToStringThreadLocalReentrancyGuard {
        buildString {

            val executableName = when (statement) {
                is ConstructorId -> statement.classId.name
                is DirectFieldAccessId -> statement.name
                is MethodId -> statement.name
            }

            instance?.let { append("${it.modelName}.") }

            append(executableName)
            val paramsRepresentation = params.map { param ->
                when (param) {
                    is UtAssembleModel -> param.modelName
                    else -> param
                }
            }
            append(paramsRepresentation.joinToString(prefix = "(", postfix = ")"))
        }
    }
}

/**
 * Step of assemble instruction that calls executable.
 *
 * Contains executable to call, call parameters and an instance model before call.
 * @param [instance] **must be** `null` for static methods and constructors
 */
data class UtExecutableCallModel(
    override val instance: UtReferenceModel?,
    val executable: ExecutableId,
    override val params: List<UtModel>,
) : UtStatementCallModel(instance, executable, params) {
    override fun toString(): String = super.toString()
}

/**
 * Step of assemble instruction that directly accesses a field.
 *
 * Contains parameter value to set and an instance model before call.
 */
data class UtDirectGetFieldModel(
    override val instance: UtReferenceModel,
    val fieldAccess: DirectFieldAccessId,
) : UtStatementCallModel(instance, fieldAccess, emptyList()) {
    override fun toString(): String = super.toString()
}

/**
 * Step of assemble instruction that sets public field with direct setter.
 *
 * Contains instance model, fieldId to set value and a model of this value.
 */
data class UtDirectSetFieldModel(
    override val instance: UtReferenceModel,
    val fieldId: FieldId,
    val fieldModel: UtModel,
) : UtStatementModel(instance) {
    override fun toString(): String = withToStringThreadLocalReentrancyGuard {
        val modelRepresentation = when (fieldModel) {
            is UtAssembleModel -> fieldModel.modelName
            else -> fieldModel.toString()
        }
        "${instance.modelName}.${fieldId.name} = $modelRepresentation"
    }

}

/**
 * Instrumentation required for execution.
 *
 * Used for mocking new instance creation and mocking static method.
 */
sealed class UtInstrumentation

/**
 * Instrumentation for mocking new instance creation.
 *
 * Contains class id for instance, instances (represented as models) to create instead of calling "new" and call sites
 * where creation takes place.
 *
 * Note: call sites required by mock framework to know which classes to instrument.
 */
data class UtNewInstanceInstrumentation(
    val classId: ClassId,
    val instances: List<UtModel>,
    val callSites: Set<ClassId>
) : UtInstrumentation()

/**
 * Instrumentation for mocking static method.
 *
 * Contains method id and values to return.
 */
@Suppress("unused")
data class UtStaticMethodInstrumentation(
    val methodId: MethodId,
    val values: List<UtModel>
) : UtInstrumentation()

val SootClass.id: ClassId
    get() = ClassId(name)

val RefType.id: ClassId
    get() = ClassId(className)
val ArrayType.id: ClassId
    get() {
        val elementId = elementType.classId
        val elementTypeName = when {
            elementId.isArray -> elementId.name
            elementId.isPrimitive -> elementId.primitiveTypeJvmNameOrNull()!!
            else -> "L${elementId.name};"
        }
        return ClassId("[$elementTypeName", elementId)
    }

/**
 * Converts Soot Type to class id.
 */
val Type.classId: ClassId
    get() = when (this) {
        is VoidType -> voidClassId
        is BooleanType -> booleanClassId
        is ByteType -> byteClassId
        is CharType -> charClassId
        is ShortType -> shortClassId
        is IntType -> intClassId
        is LongType -> longClassId
        is FloatType -> floatClassId
        is DoubleType -> doubleClassId
        is ArrayType -> this.id
        is RefType -> this.id
        else -> error("Unknown type $this")
    }

/**
 * Class id. Contains name, not a full qualified name.
 *
 * If class represents array, element class id is not null.
 * For multidimensional arrays element class represents array of dimensions-1.
 *
 * Note: currently uses class$innerClass form to load classes with classloader.
 *
 * [name] the name of the class in the form returned by [java.lang.Class.getName].
 * It is important not to use other forms of class names for the [name] property.
 *
 * [elementClassId] if this class id represents an array class, then this property
 * represents the class id of the array's elements. Otherwise, this property is null.
 */
open class ClassId @JvmOverloads constructor(
    val name: String,
    val elementClassId: ClassId? = null,
    // Treat simple class ids as non-nullable
    open val isNullable: Boolean = false
) {
    open val modifiers: Int
        get() = jClass.modifiers

    open val canonicalName: String
        get() = jClass.canonicalName ?: error("ClassId $name does not have canonical name")

    open val simpleName: String get() = jClass.simpleName

    /**
     * For regular classes this is just a simple name.
     * For anonymous classes this includes the containing class and numeric indices of the anonymous class.
     *
     * Note: according to [java.lang.Class.getCanonicalName] documentation, local and anonymous classes
     * do not have canonical names, as well as arrays whose elements don't have canonical classes themselves.
     * In these cases prettified names are constructed using [ClassId.name] instead of [ClassId.canonicalName].
     */
    val prettifiedName: String
        get() {
            val baseName = when {
                // anonymous classes have empty simpleName and their canonicalName is null,
                // so we create a specific name for them
                isAnonymous -> "Anonymous${supertypeOfAnonymousClass.prettifiedName}"
                // in other cases where canonical name is still null, we use ClassId.name instead
                else -> jClass.canonicalName ?: name // Explicit jClass reference to get null instead of exception
            }
            return baseName
                .substringAfterLast(".")
                .replace(Regex("[^a-zA-Z0-9]"), "")
                .let { if (this.isArray) it + "Array" else it }
        }

    open val packageName: String get() = jClass.`package`?.name ?: "" // empty package for primitives

    open val isInDefaultPackage: Boolean
        get() = packageName.isEmpty()

    open val isAnonymous: Boolean
        get() = jClass.isAnonymousClass

    open val isLocal: Boolean
        get() = jClass.isLocalClass

    open val isInner: Boolean
        get() = jClass.isMemberClass && !isStatic

    open val isNested: Boolean
        get() = jClass.enclosingClass != null

    open val isSynthetic: Boolean
        get() = jClass.isSynthetic

    open val isKotlinObject: Boolean
        get() = kClass.objectInstance != null

    /**
     * Collects all declared methods (including private and protected) from class and all its superclasses to sequence
     */
    open val allMethods: Sequence<MethodId>
        get() = generateSequence(jClass) { it.superclass }
            .flatMap { it.interfaces.toMutableList() + it }
            .mapNotNull { it.declaredMethods }
            .flatMap { it.toList() }
            .map { it.executableId }

    /**
     * Collects all declared constructors (including private and protected) from class to sequence
     */
    open val allConstructors: Sequence<ConstructorId>
        get() = jClass.declaredConstructors.asSequence().map { it.executableId }

    open val typeParameters: TypeParameters
        get() = TypeParameters()

    open val outerClass: Class<*>?
        get() = jClass.enclosingClass

    open val superclass: Class<*>?
        get() = jClass.superclass

    open val interfaces: Array<Class<*>>
        get() = jClass.interfaces

    /**
     * For member classes returns a name including
     * enclosing classes' simple names e.g. `A.B`.
     *
     * For other classes returns [simpleName].
     *
     * It is needed because [simpleName] for inner classes does not
     * take into account enclosing classes' names.
     */
    open val simpleNameWithEnclosingClasses: String
        get() {
            val clazz = jClass
            return if (clazz.isMemberClass) {
                "${clazz.enclosingClass.id.simpleNameWithEnclosingClasses}.$simpleName"
            } else {
                simpleName
            }
        }

    val jvmName: String
        get() = when {
            isArray -> "[${elementClassId!!.jvmName}"
            isPrimitive -> primitiveTypeJvmNameOrNull()!!
            else -> name.toReferenceTypeBytecodeSignature()
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClassId) return false

        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String =
        if (elementClassId == null) {
            name
        } else {
            "$name, elementClass=${elementClassId}"
        }
}

/**
 * By default, we assume that class represented by BuiltinClassId is not nested and package is calculated in this assumption
 * (it is important because name for nested classes contains $ as a delimiter between nested and outer classes)
 */
class BuiltinClassId(
    elementClassId: ClassId? = null,
    override val canonicalName: String,
    override val simpleName: String,
    // set name manually only if it differs from canonical (e.g. for nested classes)
    name: String = canonicalName,
    // by default, we assume that the class is not a member class
    override val simpleNameWithEnclosingClasses: String = simpleName,
    override val isNullable: Boolean = false,
    isPublic: Boolean = true,
    isProtected: Boolean = false,
    isPrivate: Boolean = false,
    isFinal: Boolean = false,
    isStatic: Boolean = false,
    isAbstract: Boolean = false,
    override val isAnonymous: Boolean = false,
    override val isLocal: Boolean = false,
    override val isInner: Boolean = false,
    override val isNested: Boolean = false,
    override val isSynthetic: Boolean = false,
    override val isKotlinObject: Boolean = false,
    override val typeParameters: TypeParameters = TypeParameters(),
    override val allMethods: Sequence<MethodId> = emptySequence(),
    override val allConstructors: Sequence<ConstructorId> = emptySequence(),
    override val outerClass: Class<*>? = null,
    // by default, we assume that the class does not have a superclass (other than Object)
    override val superclass: Class<*>? = java.lang.Object::class.java,
    // by default, we assume that the class does not implement any interfaces
    override val interfaces: Array<Class<*>> = emptyArray(),
    override val packageName: String =
        when (val index = canonicalName.lastIndexOf('.')) {
            -1, 0 -> ""
            else -> canonicalName.substring(0, index)
        },
) : ClassId(
    name = name,
    elementClassId = elementClassId,
    isNullable = isNullable,
) {
    override val modifiers: Int = ModifierFactory {
        public = isPublic
        protected = isProtected
        private = isPrivate
        final = isFinal
        static = isStatic
        abstract = isAbstract
    }

    init {
        BUILTIN_CLASSES_BY_NAMES[name] = this
    }

    companion object {
        /**
         * Stores all created builtin classes by their names. Useful when we want to create ClassId only from name
         */
        // TODO replace ClassId constructor with a factory?
        val BUILTIN_CLASSES_BY_NAMES: MutableMap<String, BuiltinClassId> = mutableMapOf()

        /**
         * Returns [BuiltinClassId] if any [BuiltinClassId] was created with such [name], null otherwise
         */
        fun getBuiltinClassByNameOrNull(name: String): BuiltinClassId? = BUILTIN_CLASSES_BY_NAMES[name]
    }
}

/**
 * Field id. Contains field name.
 *
 * Created to avoid usage String objects as a key.
 */
open class FieldId(val declaringClass: ClassId, val name: String) {
    open val modifiers: Int
        get() = jField.modifiers

    open val isSynthetic: Boolean
        get() = jField.isSynthetic

    open val type: ClassId
        get() = jField.type.id

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FieldId

        if (declaringClass != other.declaringClass) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = declaringClass.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString() = safeJField?.toString() ?: "[unresolved] $declaringClass.$name"
}

/**
 * The same as [FieldId], except it represents the fields
 * of classes that may not be present on the classpath.
 *
 * Some properties are passed to the constructor directly in order to
 * avoid using class loader to load a possibly missing class.
 */
@Suppress("unused")
class BuiltinFieldId(
    declaringClass: ClassId,
    name: String,
    override val type: ClassId,
    // by default we assume that the builtin field is public and non-final
    isPublic: Boolean = true,
    isPrivate: Boolean = false,
    isFinal: Boolean = false,
    override val isSynthetic: Boolean = false,
) : FieldId(declaringClass, name) {
    override val modifiers = ModifierFactory {
        public = isPublic
        private = isPrivate
        final = isFinal
    }

}

sealed class StatementId {
    abstract val classId: ClassId
    abstract val name: String
}

/**
 * Direct access to public field id.
 */
class DirectFieldAccessId(
    override val classId: ClassId,
    override val name: String,
    val fieldId: FieldId,
) : StatementId()


sealed class ExecutableId : StatementId() {
    abstract override val classId: ClassId
    abstract override val name: String
    abstract val returnType: ClassId
    abstract val parameters: List<ClassId>

    /**
     * Normally during concrete execution every executable is executed in a
     * [sandbox](https://github.com/UnitTestBot/UTBotJava/blob/main/docs/Sandboxing.md).
     *
     * However, if this flag is set to `true`, then `this` particular executable is executed without a sandbox.
     */
    abstract val bypassesSandbox: Boolean

    abstract val modifiers: Int

    val signature: String
        get() {
            val args = parameters.joinToString(separator = "") { it.jvmName }
            val retType = returnType.jvmName
            return "$name($args)$retType"
        }

    fun describesSameMethodAs(other: ExecutableId): Boolean {
        return classId == other.classId && signature == other.signature
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return describesSameMethodAs(other as ExecutableId)
    }

    override fun hashCode(): Int {
        var result = classId.hashCode()
        result = 31 * result + signature.hashCode()
        return result
    }

    override fun toString() = "$classId.$name"
}

/**
 * Method id.
 *
 * Using extension property 'signature' of this class
 * one can get a signature that identifies method unambiguously
 */
open class MethodId(
    override val classId: ClassId,
    override val name: String,
    override val returnType: ClassId,
    override val parameters: List<ClassId>,
    override val bypassesSandbox: Boolean = false,
) : ExecutableId() {
    override val modifiers: Int
        get() = method.modifiers
}

open class ConstructorId(
    override val classId: ClassId,
    override val parameters: List<ClassId>,
    override val bypassesSandbox: Boolean = false,
) : ExecutableId() {
    final override val name: String = "<init>"
    final override val returnType: ClassId = voidClassId

    override val modifiers: Int
        get() = constructor.modifiers

}

class BuiltinMethodId(
    classId: ClassId,
    name: String,
    returnType: ClassId,
    parameters: List<ClassId>,
    bypassesSandbox: Boolean = false,
    // by default we assume that the builtin method is non-static and public
    isStatic: Boolean = false,
    isPublic: Boolean = true,
    isProtected: Boolean = false,
    isPrivate: Boolean = false
) : MethodId(classId, name, returnType, parameters, bypassesSandbox) {
    override val modifiers: Int = ModifierFactory {
        static = isStatic
        public = isPublic
        private = isPrivate
        protected = isProtected
    }
}

class BuiltinConstructorId(
    classId: ClassId,
    parameters: List<ClassId>,
    bypassesSandbox: Boolean = false,
    // by default, we assume that the builtin constructor is public
    isPublic: Boolean = true,
    isProtected: Boolean = false,
    isPrivate: Boolean = false
) : ConstructorId(classId, parameters, bypassesSandbox) {
    override val modifiers: Int = ModifierFactory {
        public = isPublic
        private = isPrivate
        protected = isProtected
    }
}

open class TypeParameters(val parameters: List<ClassId> = emptyList()) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TypeParameters

        if (parameters != other.parameters) return false

        return true
    }

    override fun hashCode(): Int {
        return parameters.hashCode()
    }
}

object WildcardTypeParameter : TypeParameters(emptyList())

/**
 * Describes the way to replace abstract types with concrete implementors.
 */
enum class TypeReplacementMode {
    /**
     * Any possible implementor (that is preferred by solver) may be used.
     */
    AnyImplementor,

    /**
     * There is a known implementor to be used.
     * For example, it is obtained from bean definitions in Spring application.
     */
    KnownImplementor,

    /**
     * Using implementors is not allowed.
     * If mocking is allowed, mock of this type will be used.
     * Otherwise, branch will be pruned as unsatisfiable.
     */
    NoImplementors,
}

interface CodeGenerationContext

interface SpringCodeGenerationContext : CodeGenerationContext {
    val springTestType: SpringTestType
    val springSettings: SpringSettings
    val springContextLoadingResult: SpringContextLoadingResult?
}

/**
 * A context to use when no specific data is required.
 *
 * @param mockFrameworkInstalled shows if we have installed framework dependencies
 * @param staticsMockingIsConfigured shows if we have installed static mocking tools
 */
open class ApplicationContext(
    val mockFrameworkInstalled: Boolean = true,
    staticsMockingIsConfigured: Boolean = true,
) : CodeGenerationContext {
    var staticsMockingIsConfigured = staticsMockingIsConfigured
        private set

    init {
        /**
         * Situation when mock framework is not installed but static mocking is configured is semantically incorrect.
         *
         * However, it may be obtained in real application after this actions:
         * - fully configure mocking (dependency installed + resource file created)
         * - remove mockito-core dependency from project
         * - forget to remove mock-maker file from resource directory
         *
         * Here we transform this configuration to semantically correct.
         */
        if (!mockFrameworkInstalled && staticsMockingIsConfigured) {
            this.staticsMockingIsConfigured = false
        }
    }

    /**
     * Shows if there are any restrictions on type implementors.
     */
    open val typeReplacementMode: TypeReplacementMode = AnyImplementor

    /**
     * Finds a type to replace the original abstract type
     * if it is guided with some additional information.
     */
    open fun replaceTypeIfNeeded(type: RefType): ClassId? = null

    /**
     * Sets the restrictions on speculative not null
     * constraints in current application context.
     *
     * @see docs/SpeculativeFieldNonNullability.md for more information.
     */
    open fun avoidSpeculativeNotNullChecks(field: SootField): Boolean =
        UtSettings.maximizeCoverageUsingReflection || !field.declaringClass.isFromTrustedLibrary()

    /**
     * Checks whether accessing [field] (with a method invocation or field access) speculatively
     * cannot produce [NullPointerException] (according to its finality or accessibility).
     *
     * @see docs/SpeculativeFieldNonNullability.md for more information.
     */
    open fun speculativelyCannotProduceNullPointerException(
        field: SootField,
        classUnderTest: ClassId,
    ): Boolean = field.isFinal || !field.isPublic

    open fun preventsFurtherTestGeneration(): Boolean = false

    open fun getErrors(): List<UtError> = emptyList()
}

sealed class SpringConfiguration(val fullDisplayName: String) {
    class JavaConfiguration(val classBinaryName: String) : SpringConfiguration(classBinaryName)
    class XMLConfiguration(val absolutePath: String) : SpringConfiguration(absolutePath)
}

sealed interface SpringSettings {
    class AbsentSpringSettings : SpringSettings {
        // Denotes no configuration and no profile setting

        // NOTICE:
        // `class` should not be replaced with `object`
        // in order to avoid issues caused by Kryo deserialization
        // that creates new instances breaking `when` expressions
        // that check reference equality instead of type equality
    }

    class PresentSpringSettings(
        val configuration: SpringConfiguration,
        val profiles: Array<String>
    ) : SpringSettings
}

/**
 * [contextLoaded] can be `true` while [exceptions] is not empty,
 * if we failed to use most specific SpringApi available (e.g. SpringBoot), but
 * were able to successfully fall back to less specific SpringApi (e.g. PureSpring).
 */
class SpringContextLoadingResult(
    val contextLoaded: Boolean,
    val exceptions: List<Throwable>
)

/**
 * Data we get from Spring application context
 * to manage engine and code generator behaviour.
 *
 * @param beanDefinitions describes bean definitions (bean name, type, some optional additional data)
 * @param shouldUseImplementors describes it we want to replace interfaces with injected types or not
 */
// TODO move this class to utbot-framework so we can use it as abstract factory
//  to get rid of numerous `when`s and polymorphically create things like:
//    - Instrumentation<UtConcreteExecutionResult>
//    - FuzzedType (to get rid of thisInstanceFuzzedTypeWrapper)
//    - JavaValueProvider
//    - CgVariableConstructor
//    - CodeGeneratorResult (generateForSpringClass)
//  Right now this refactoring is blocked because some interfaces need to get extracted and moved to utbot-framework-api
//  As an alternative we can just move ApplicationContext itself to utbot-framework
class SpringApplicationContext(
    mockInstalled: Boolean,
    staticsMockingIsConfigured: Boolean,
    val beanDefinitions: List<BeanDefinitionData> = emptyList(),
    private val shouldUseImplementors: Boolean,
    override val springTestType: SpringTestType,
    override val springSettings: SpringSettings,
): ApplicationContext(mockInstalled, staticsMockingIsConfigured), SpringCodeGenerationContext {

    override var springContextLoadingResult: SpringContextLoadingResult? = null

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private var areInjectedClassesInitialized : Boolean = false

    // Classes representing concrete types that are actually used in Spring application
    private val springInjectedClasses: Set<ClassId>
        get() {
            if (!areInjectedClassesInitialized) {
                for (beanTypeName in beanDefinitions.map { it.beanTypeName }) {
                    try {
                        val beanClass = utContext.classLoader.loadClass(beanTypeName)
                        if (!beanClass.isAbstract && !beanClass.isInterface &&
                            !beanClass.isLocalClass && (!beanClass.isMemberClass || beanClass.isStatic)) {
                            springInjectedClassesStorage += beanClass.id
                        }
                    } catch (e: Throwable) {
                        // For some Spring beans (e.g. with anonymous classes)
                        // it is possible to have problems with classes loading.
                        when (e) {
                            is ClassNotFoundException, is NoClassDefFoundError, is IllegalAccessError ->
                                logger.warn { "Failed to load bean class for $beanTypeName (${e.message})" }

                            else -> throw e
                        }
                    }
                }

                // This is done to be sure that this storage is not empty after the first class loading iteration.
                // So, even if all loaded classes were filtered out, we will not try to load them again.
                areInjectedClassesInitialized = true
            }

            return springInjectedClassesStorage
        }

    // This is a service field to model the lazy behavior of [springInjectedClasses].
    // Do not call it outside the getter.
    //
    // Actually, we should just call [springInjectedClasses] with `by lazy`, but  we had problems
    // with a strange `kotlin.UNINITIALIZED_VALUE` in `speculativelyCannotProduceNullPointerException` method call.
    private val springInjectedClassesStorage = mutableSetOf<ClassId>()

    override val typeReplacementMode: TypeReplacementMode =
        if (shouldUseImplementors) KnownImplementor else NoImplementors

    /**
     * Replaces an interface type with its implementor type
     * if there is the unique implementor in bean definitions.
     */
    override fun replaceTypeIfNeeded(type: RefType): ClassId? =
        if (type.isAbstractType) {
            springInjectedClasses.singleOrNull { it.isSubtypeOf(type.id) }
        } else {
            null
        }

    override fun avoidSpeculativeNotNullChecks(field: SootField): Boolean = false

    /**
     * In Spring applications we can mark as speculatively not null
     * fields if they are mocked and injecting into class under test so on.
     *
     * Fields are not mocked if their actual type is obtained from [springInjectedClasses].
     *
     */
    override fun speculativelyCannotProduceNullPointerException(
        field: SootField,
        classUnderTest: ClassId,
    ): Boolean {
        val injectedTypes = springInjectedClasses.flatMap { it.allSuperTypes() }
        return field.fieldId in classUnderTest.allDeclaredFieldIds && field.type.classId !in injectedTypes
    }

    override fun preventsFurtherTestGeneration(): Boolean =
        super.preventsFurtherTestGeneration() || springContextLoadingResult?.contextLoaded == false

    override fun getErrors(): List<UtError> =
        springContextLoadingResult?.exceptions?.map { exception ->
            UtError(
                "Failed to load Spring application context",
                exception
            )
        }.orEmpty() + super.getErrors()

    fun getBeansAssignableTo(classId: ClassId): List<BeanDefinitionData> = beanDefinitions.filter { beanDef ->
        // some bean classes may fail to load
        runCatching {
            val beanClass = ClassId(beanDef.beanTypeName).jClass
            classId.jClass.isAssignableFrom(beanClass)
        }.getOrElse { false }
    }
}

enum class SpringTestType(
    override val id: String,
    override val displayName: String,
    override val description: String,
) : CodeGenerationSettingItem {
    UNIT_TEST(
        "Unit tests",
        "Unit tests",
        "Generate unit tests mocking other classes"
    ),
    INTEGRATION_TEST(
        "Integration tests",
        "Integration tests",
        "Generate integration tests autowiring real instance"
    );

    override fun toString() = id

    companion object : CodeGenerationSettingBox {
        override val defaultItem = UNIT_TEST
        override val allItems: List<SpringTestType> = values().toList()
    }
}

/**
 * Describes information about beans obtained from Spring analysis process.
 *
 * Contains the name of the bean, its type (class or interface) and optional additional data.
 *
 * @param beanTypeName a name in a form obtained by [java.lang.Class.getName] method.
 */
data class BeanDefinitionData(
    val beanName: String,
    val beanTypeName: String,
    val additionalData: BeanAdditionalData?,
)

/**
 * Describes some additional information about beans obtained from Spring analysis process.
 *
 * Sometimes the actual type of the bean can not be obtained from bean definition.
 * Then we try to recover it by method and class defining bean (e.g. using Psi elements).
 *
 * @param configClassName a name in a form obtained by [java.lang.Class.getName] method.
 */
data class BeanAdditionalData(
    val factoryMethodName: String,
    val parameterTypes: List<String>,
    val configClassName: String,
)

val RefType.isAbstractType
 get() = this.sootClass.isAbstract || this.sootClass.isInterface

interface CodeGenerationSettingItem {
    val id: String
    val displayName: String
    val description: String
}

interface CodeGenerationSettingBox {
    val defaultItem: CodeGenerationSettingItem
    val allItems: List<CodeGenerationSettingItem>

    fun labels(): Array<String> = allItems.map { it.displayName }.toTypedArray()
}

enum class MockStrategyApi(
    override val id: String,
    override val displayName: String,
    override val description: String
) : CodeGenerationSettingItem {
    NO_MOCKS("No mocks", "Do not mock", "Do not use mock frameworks at all"),
    OTHER_PACKAGES(
        "Other packages: Mockito",
        "Mock everything outside the package",
        "Mock all classes outside the current package except system ones"
    ),
    OTHER_CLASSES(
        "Other classes: Mockito",
        "Mock everything outside the class",
        "Mock all classes outside the class under test except system ones"
    );

    override fun toString() = id

    // Get is mandatory because of the initialization order of the inheritors.
    // Otherwise, in some cases we could get an incorrect value
    companion object : CodeGenerationSettingBox {
        override val defaultItem = OTHER_PACKAGES
        override val allItems: List<MockStrategyApi> = values().toList()

        // Mock strategy gains more meaning in Spring Projects.
        // We use OTHER_CLASSES strategy as default one in `No configuration` mode
        // and as unique acceptable in other modes (combined with type replacement).
        val springDefaultItem = OTHER_CLASSES
        // We use NO_MOCKS strategy in integration tests because they are based on fuzzer that is not compatible with mocks
        val springIntegrationTestItem = NO_MOCKS
    }
}

enum class TreatOverflowAsError(
    override val id: String,
    override val displayName: String,
    override val description: String,
) : CodeGenerationSettingItem {
    AS_ERROR(
        id = "Treat overflows as errors",
        displayName = "Treat overflows as errors",
        description = "Generate tests that treat possible overflows in arithmetic operations as errors " +
                "that throw Arithmetic Exception",
    ),
    IGNORE(
        id = "Ignore overflows",
        displayName = "Ignore overflows",
        description = "Ignore possible overflows in arithmetic operations",
    );

    override fun toString(): String = id

    // Get is mandatory because of the initialization order of the inheritors.
    // Otherwise, in some cases we could get an incorrect value
    companion object : CodeGenerationSettingBox {
        override val defaultItem: TreatOverflowAsError get() = if (UtSettings.treatOverflowAsError) AS_ERROR else IGNORE
        override val allItems: List<TreatOverflowAsError> = values().toList()
    }
}

enum class JavaDocCommentStyle(
    override val id: String,
    override val displayName: String,
    override val description: String,
) : CodeGenerationSettingItem {
    CUSTOM_JAVADOC_TAGS(
        id = "Structured via custom Javadoc tags",
        displayName = "Structured via custom Javadoc tags",
        description = "Uses custom Javadoc tags to describe test's execution path."
    ),
    FULL_SENTENCE_WRITTEN(
        id = "Plain text",
        displayName = "Plain text",
        description = "Uses plain text to describe test's execution path."
    );

    override fun toString(): String = displayName

    companion object : CodeGenerationSettingBox {
        override val defaultItem = if (UtSettings.useCustomJavaDocTags) CUSTOM_JAVADOC_TAGS else FULL_SENTENCE_WRITTEN
        override val allItems = JavaDocCommentStyle.values().toList()
    }
}

enum class MockFramework(
    override val id: String = "Mockito",
    override val displayName: String,
    override val description: String = "Use $displayName as mock framework",
    var isInstalled: Boolean = false
) : CodeGenerationSettingItem {
    MOCKITO(displayName = "Mockito");

    override fun toString() = id

    companion object : CodeGenerationSettingBox {
        override val defaultItem: MockFramework = MOCKITO
        override val allItems: List<MockFramework> = values().toList()
    }
}

enum class CodegenLanguage(
    override val id: String,
    override val displayName: String,
    @Suppress("unused") override val description: String = "Generate unit tests in $displayName"
) : CodeGenerationSettingItem {
    JAVA(id = "Java", displayName = "Java"),
    KOTLIN(id = "Kotlin", displayName = "Kotlin (experimental)");

    enum class OperatingSystem {
        WINDOWS,
        UNIX;

        companion object {
            fun fromSystemProperties(): OperatingSystem {
                val osName = System.getProperty("os.name")
                return when {
                    osName.startsWith("Windows") -> WINDOWS
                    else -> UNIX
                }
            }
        }
    }

    private val operatingSystem: OperatingSystem = OperatingSystem.fromSystemProperties()
    private val kotlinCompiler = if (operatingSystem == OperatingSystem.WINDOWS) "kotlinc.bat" else "kotlinc"
    private val jvmTarget = "1.8"

    private val compilerExecutableAbsolutePath: String
        get() = when (this) {
            JAVA -> listOf(System.getenv("JAVA_HOME"), "bin", "javac")
            KOTLIN -> listOf(System.getenv("KOTLIN_HOME"), "bin", kotlinCompiler)
        }.joinToString(File.separator)

    val extension: String
        get() = when (this) {
            JAVA -> ".java"
            KOTLIN -> ".kt"
        }

    val executorInvokeCommand: String
        get() = when (this) {
            JAVA -> listOf(System.getenv("JAVA_HOME"), "bin", "java")
            KOTLIN -> listOf(System.getenv("JAVA_HOME"), "bin", "java")
        }.joinToString(File.separator)

    override fun toString(): String = id

    fun getCompilationCommand(buildDirectory: String, classPath: String, sourcesFiles: List<String>): List<String> {
        val arguments = when (this) {
            JAVA -> listOf(
                "-d", buildDirectory,
                "-cp", classPath,
                "-XDignore.symbol.file", // to let javac use classes from rt.jar
            ).plus(OpenModulesContainer.javaVersionSpecificArguments.toMutableList().apply {
                if (last().contains("illegal"))
                    removeLast()
            }).plus(sourcesFiles)

            // TODO: -Xskip-prerelease-check is needed to handle #1262, check if this is good enough solution
            KOTLIN -> listOf("-d", buildDirectory, "-jvm-target", jvmTarget, "-cp", classPath, "-Xskip-prerelease-check").plus(sourcesFiles)
        }
        if (this == KOTLIN && System.getenv("KOTLIN_HOME") == null) {
            throw RuntimeException("'KOTLIN_HOME' environment variable is not defined. Standard location is {IDEA installation dir}/plugins/Kotlin/kotlinc")
        }

        return listOf(compilerExecutableAbsolutePath) + isolateCommandLineArgumentsToArgumentFile(arguments)
    }

    // Get is mandatory because of the initialization order of the inheritors.
    // Otherwise, in some cases we could get an incorrect value
    companion object : CodeGenerationSettingBox {
        override val defaultItem: CodegenLanguage get() = JAVA
        override val allItems: List<CodegenLanguage> = values().toList()
    }
}
//TODO #1279
fun CodegenLanguage?.isSummarizationCompatible() = this == CodegenLanguage.JAVA

// https://docs.oracle.com/javase/7/docs/technotes/tools/windows/javac.html#commandlineargfile
fun isolateCommandLineArgumentsToArgumentFile(arguments: List<String>): String {
    val argumentFile = FileUtil.createTempFile("cmd-args", "").toFile()
    argumentFile.writeText(
        arguments.joinToString(" ") {
            // If a filename contains embedded spaces, put the whole filename in double quotes,
            // and double each backslash ("My Files\\Stuff.java").
            "\"${it.replace(File.separator, File.separator.repeat(2))}\""
        }
    )
    return argumentFile.absolutePath.let { "@$it" }
}

private fun StringBuilder.appendOptional(name: String, value: Collection<*>) {
    if (value.isNotEmpty()) {
        append(", $name=$value")
    }
}

private fun StringBuilder.appendOptional(name: String, value: Map<*, *>) {
    if (value.isNotEmpty()) {
        append(", $name=$value")
    }
}

/**
 * Entity that represents cluster information that should appear in the comment.
 */
data class UtClusterInfo(
    val header: String? = null,
    val content: String? = null
)

/**
 * Entity that represents cluster of executions.
 */
data class UtExecutionCluster(val clusterInfo: UtClusterInfo, val executions: List<UtExecution>)

/**
 * Entity that represents various types of statements in comments.
 */
sealed class DocStatement

sealed class DocTagStatement(val content: List<DocStatement>) : DocStatement()

class DocPreTagStatement(content: List<DocStatement>) : DocTagStatement(content) {
    override fun toString(): String = content.joinToString(separator = "")

    override fun equals(other: Any?): Boolean =
        if (other is DocPreTagStatement) this.hashCode() == other.hashCode() else false

    override fun hashCode(): Int = content.hashCode()
}

data class DocCustomTagStatement(val statements: List<DocStatement>) : DocTagStatement(statements) {
    override fun toString(): String = content.joinToString(separator = "")
}

open class DocClassLinkStmt(val className: String) : DocStatement() {
    override fun toString(): String = className

    override fun equals(other: Any?): Boolean =
        if (other is DocClassLinkStmt) this.hashCode() == other.hashCode() else false

    override fun hashCode(): Int = className.hashCode()
}

class DocMethodLinkStmt(className: String, val methodName: String) : DocClassLinkStmt(className) {
    override fun toString(): String = super.toString() + "::" + methodName

    override fun equals(other: Any?): Boolean =
        if (other is DocMethodLinkStmt) this.hashCode() == other.hashCode() else false

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + methodName.hashCode()
        return result
    }
}

class DocCodeStmt(val stmt: String) : DocStatement() {
    override fun toString(): String = stmt

    override fun equals(other: Any?): Boolean =
        if (other is DocCodeStmt) this.hashCode() == other.hashCode() else false

    override fun hashCode(): Int = stmt.hashCode()
}

class DocRegularStmt(val stmt: String) : DocStatement() {
    override fun toString(): String = stmt

    override fun equals(other: Any?): Boolean =
        if (other is DocRegularStmt) this.hashCode() == other.hashCode() else false

    override fun hashCode(): Int = stmt.hashCode()
}

class DocRegularLineStmt(val stmt: String) : DocStatement() {
    override fun toString(): String = stmt

    override fun equals(other: Any?): Boolean =
        if (other is DocRegularLineStmt) this.hashCode() == other.hashCode() else false

    override fun hashCode(): Int = stmt.hashCode()
}
