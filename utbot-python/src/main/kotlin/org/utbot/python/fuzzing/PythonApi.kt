package org.utbot.python.fuzzing

import mu.KotlinLogging
import org.utbot.framework.plugin.api.UtError
import org.utbot.fuzzing.*
import org.utbot.fuzzing.utils.Trie
import org.utbot.python.coverage.PyInstruction
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.PythonUtExecution
import org.utbot.python.fuzzing.provider.*
import org.utbot.python.fuzzing.provider.utils.isAny
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.newtyping.inference.InferredTypeFeedback
import org.utbot.python.newtyping.inference.InvalidTypeFeedback
import org.utbot.python.newtyping.inference.SuccessFeedback
import org.utbot.python.newtyping.inference.baseline.BaselineAlgorithm
import org.utbot.python.utils.ExecutionWithTimoutMode
import org.utbot.python.utils.TestGenerationLimitManager
import org.utbot.python.utils.TimeoutMode
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

data class PythonFuzzedConcreteValue(
    val type: UtType,
    val value: Any,
)

class PythonMethodDescription(
    val name: String,
    parameters: List<UtType>,
    val concreteValues: Collection<PythonFuzzedConcreteValue> = emptyList(),
    val pythonTypeStorage: PythonTypeHintsStorage,
    val tracer: Trie<PyInstruction, *>,
    val random: Random,
    val limitManager: TestGenerationLimitManager,
    val type: FunctionType,
) : Description<UtType>(parameters)

sealed interface FuzzingExecutionFeedback
class ValidExecution(val utFuzzedExecution: PythonUtExecution): FuzzingExecutionFeedback
class InvalidExecution(val utError: UtError): FuzzingExecutionFeedback
class TypeErrorFeedback(val message: String) : FuzzingExecutionFeedback
class ArgumentsTypeErrorFeedback(val message: String) : FuzzingExecutionFeedback
class CachedExecutionFeedback(val cachedFeedback: FuzzingExecutionFeedback) : FuzzingExecutionFeedback
object FakeNodeFeedback : FuzzingExecutionFeedback

data class PythonExecutionResult(
    val fuzzingExecutionFeedback: FuzzingExecutionFeedback,
    val fuzzingPlatformFeedback: PythonFeedback
)

data class PythonFeedback(
    override val control: Control = Control.CONTINUE,
    val result: Trie.Node<PyInstruction> = Trie.emptyNode(),
    val typeInferenceFeedback: InferredTypeFeedback = InvalidTypeFeedback,
    val fromCache: Boolean = false,
) : Feedback<UtType, PythonFuzzedValue> {
    fun fromCache(): PythonFeedback {
        return PythonFeedback(
            control = control,
            result = result,
            typeInferenceFeedback = typeInferenceFeedback,
            fromCache = true,
        )
    }
}

class PythonFuzzedValue(
    val tree: PythonTree.PythonTreeNode,
    val summary: String? = null,
)

fun pythonDefaultValueProviders(typeStorage: PythonTypeHintsStorage) = listOf(
    NoneValueProvider,
    BoolValueProvider,
    IntValueProvider,
    FloatValueProvider,
    ComplexValueProvider,
    StrValueProvider,
    ListValueProvider,
    SetValueProvider,
    DictValueProvider,
    TupleValueProvider,
    TupleFixSizeValueProvider,
    UnionValueProvider,
    BytesValueProvider,
    BytearrayValueProvider,
    ReduceValueProvider,
    RePatternValueProvider,
    ConstantValueProvider,
    TypeAliasValueProvider,
    SubtypeValueProvider(typeStorage)
)

fun pythonAnyTypeValueProviders() = listOf(
    NoneValueProvider,
    BoolValueProvider,
    IntValueProvider,
    FloatValueProvider,
    ComplexValueProvider,
    StrValueProvider,
    BytesValueProvider,
    BytearrayValueProvider,
    ConstantValueProvider,
)

class PythonFuzzing(
    private val pythonTypeStorage: PythonTypeHintsStorage,
    private val typeInferenceAlgorithm: BaselineAlgorithm,
    val execute: suspend (description: PythonMethodDescription, values: List<PythonFuzzedValue>) -> PythonFeedback,
) : Fuzzing<UtType, PythonFuzzedValue, PythonMethodDescription, PythonFeedback> {

    private fun generateDefault(description: PythonMethodDescription, type: UtType)= sequence {
        pythonDefaultValueProviders(pythonTypeStorage).asSequence().forEach { provider ->
            if (provider.accept(type)) {
                logger.debug { "Provider ${provider.javaClass.simpleName} accepts type ${type.pythonTypeRepresentation()}" }
                yieldAll(provider.generate(description, type))
            }
        }
    }

    override fun generate(description: PythonMethodDescription, type: UtType): Sequence<Seed<UtType, PythonFuzzedValue>> {
        var providers = emptyList<Seed<UtType, PythonFuzzedValue>>().asSequence()

        if (type.isAny()) {
            logger.debug("Any does not have provider")
        } else {
            providers += generateDefault(description, type)
        }

        return providers
    }

    override suspend fun handle(description: PythonMethodDescription, values: List<PythonFuzzedValue>): PythonFeedback {
        val result = execute(description, values)
        if (result.typeInferenceFeedback is SuccessFeedback && !result.fromCache) {
            typeInferenceAlgorithm.laudType(description.type)
        }
        if (description.limitManager.isCancelled()) {
            typeInferenceAlgorithm.feedbackState(description.type, result.typeInferenceFeedback)
        }
        return result
    }

    private suspend fun forkType(description: PythonMethodDescription, stats: Statistic<UtType, PythonFuzzedValue>) {
        val type: UtType? = typeInferenceAlgorithm.expandState()
        if (type != null) {
            val newTypes = (type as FunctionType).arguments
            val d = PythonMethodDescription(
                description.name,
                newTypes,
                description.concreteValues,
                description.pythonTypeStorage,
                description.tracer,
                description.random,
                TestGenerationLimitManager(ExecutionWithTimoutMode, description.limitManager.until),
                type
            )
            if (!d.limitManager.isCancelled()) {
                logger.debug { "Fork new type" }
                fork(d, stats)
            }
            logger.debug { "Fork ended" }
        } else {
            description.limitManager.mode = TimeoutMode
        }
    }

    override suspend fun isCancelled(
        description: PythonMethodDescription,
        stats: Statistic<UtType, PythonFuzzedValue>
    ): Boolean {
        if (description.limitManager.isCancelled() || description.parameters.any { it.isAny() }) {
            forkType(description, stats)
            if (description.limitManager.isRootManager) {
                return TimeoutMode.isCancelled(description.limitManager)
            }
        }
        return description.limitManager.isCancelled()
    }
}
