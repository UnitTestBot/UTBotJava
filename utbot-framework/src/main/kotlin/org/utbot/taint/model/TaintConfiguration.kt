package org.utbot.taint.model

import kotlinx.serialization.Serializable
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId

/**
 * Storage for all taint rules.
 */
@Serializable
data class TaintConfiguration(
    private val sources: List<TaintSource> = listOf(),
    private val passes: List<TaintPass> = listOf(),
    private val cleaners: List<TaintCleaner> = listOf(),
    private val sinks: List<TaintSink> = listOf(),
) {

    fun getSourcesBy(executableId: ExecutableId): List<TaintSource> =
        sources.filter {
            equalParameters(executableId, it.signature) && equalMethodNames(executableId, it.methodFqn)
        }

    fun getPassesBy(executableId: ExecutableId): List<TaintPass> =
        passes.filter {
            equalParameters(executableId, it.signature) && equalMethodNames(executableId, it.methodFqn)
        }

    fun getCleanersBy(executableId: ExecutableId): List<TaintCleaner> =
        cleaners.filter {
            equalParameters(executableId, it.signature) && equalMethodNames(executableId, it.methodFqn)
        }

    fun getSinksBy(executableId: ExecutableId): List<TaintSink> =
        sinks.filter {
            equalParameters(executableId, it.signature) && equalMethodNames(executableId, it.methodFqn)
        }

    operator fun plus(other: TaintConfiguration): TaintConfiguration =
        TaintConfiguration(
            sources = sources + other.sources,
            passes = passes + other.passes,
            cleaners = cleaners + other.cleaners,
            sinks = sinks + other.sinks
        )

    // internal

    private fun equalParameters(executableId: ExecutableId, signature: TaintSignature): Boolean =
        when (signature) {
            is TaintSignatureAny -> true
            is TaintSignatureList -> {
                val equalSizes = signature.argumentTypes.size == executableId.parameters.size
                val equalElementwise = (signature.argumentTypes zip executableId.parameters).all { (type, parameter) ->
                    equalTypes(parameter, type)
                }
                equalSizes && equalElementwise
            }
        }

    private fun equalTypes(classId: ClassId, argumentType: ArgumentType): Boolean =
        when (argumentType) {
            ArgumentTypeAny -> true
            is ArgumentTypeString -> argumentType.typeFqn == classId.name
        }

    private fun equalMethodNames(executableId: ExecutableId, methodFqn: MethodFqn): Boolean =
        methodFqn == executableId.getMethodFqn()

    private fun ExecutableId.getMethodFqn(): MethodFqn? =
        runCatching {
            MethodFqn(classId.packageName.split('.'), classId.simpleName, name)
        }.getOrNull()
}

/**
 * @param methodFqn method fully qualified name
 * @param addTo objects to be marked
 * @param marks marks that should be added to the objects from the [addTo] list
 * @param signature list of argument types (at compile time)
 * @param condition condition that must be met to trigger this rule
 */
@Serializable
data class TaintSource(
    val methodFqn: MethodFqn,
    val addTo: TaintEntities,
    val marks: TaintMarks,
    val signature: TaintSignature,
    val condition: TaintCondition,
)

/**
 * @param methodFqn method fully qualified name
 * @param getFrom the sources of marks
 * @param addTo objects that will be marked if any element from [getFrom] has any mark
 * @param marks actual marks that can be passed from one object to another
 * @param signature list of argument types (at compile time)
 * @param condition condition that must be met to trigger this rule
 */
@Serializable
data class TaintPass(
    val methodFqn: MethodFqn,
    val getFrom: TaintEntities,
    val addTo: TaintEntities,
    val marks: TaintMarks,
    val signature: TaintSignature,
    val condition: TaintCondition,
)

/**
 * @param methodFqn method fully qualified name
 * @param removeFrom objects from which marks should be removed
 * @param marks marks to be removed
 * @param signature list of argument types (at compile time)
 * @param condition condition that must be met to trigger this rule
 */
@Serializable
data class TaintCleaner(
    val methodFqn: MethodFqn,
    val removeFrom: TaintEntities,
    val marks: TaintMarks,
    val signature: TaintSignature,
    val condition: TaintCondition,
)

/**
 * @param methodFqn method fully qualified name
 * @param check objects that will be checked for marks
 * @param marks when one of the [marks] is found in one of the objects from the [check],
 *              the analysis will report the problem found
 * @param signature list of argument types (at compile time)
 * @param condition condition that must be met to trigger this rule
 */
@Serializable
data class TaintSink(
    val methodFqn: MethodFqn,
    val check: TaintEntities,
    val marks: TaintMarks,
    val signature: TaintSignature,
    val condition: TaintCondition,
)