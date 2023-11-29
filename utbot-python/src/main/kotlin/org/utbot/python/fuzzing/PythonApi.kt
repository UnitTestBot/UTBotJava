package org.utbot.python.fuzzing

import mu.KotlinLogging
import org.utbot.fuzzer.FuzzedContext
import org.utbot.fuzzing.Control
import org.utbot.fuzzing.Description
import org.utbot.fuzzing.Feedback
import org.utbot.fuzzing.Fuzzing
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.Statistic
import org.utbot.fuzzing.utils.Trie
import org.utbot.python.coverage.PyInstruction
import org.utbot.python.engine.ExecutionFeedback
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.fuzzing.provider.BoolValueProvider
import org.utbot.python.fuzzing.provider.BytearrayValueProvider
import org.utbot.python.fuzzing.provider.BytesValueProvider
import org.utbot.python.fuzzing.provider.ComplexValueProvider
import org.utbot.python.fuzzing.provider.ConstantValueProvider
import org.utbot.python.fuzzing.provider.DictValueProvider
import org.utbot.python.fuzzing.provider.FloatValueProvider
import org.utbot.python.fuzzing.provider.IntValueProvider
import org.utbot.python.fuzzing.provider.IteratorValueProvider
import org.utbot.python.fuzzing.provider.ListValueProvider
import org.utbot.python.fuzzing.provider.NoneValueProvider
import org.utbot.python.fuzzing.provider.OptionalValueProvider
import org.utbot.python.fuzzing.provider.RePatternValueProvider
import org.utbot.python.fuzzing.provider.ReduceValueProvider
import org.utbot.python.fuzzing.provider.SetValueProvider
import org.utbot.python.fuzzing.provider.StrValueProvider
import org.utbot.python.fuzzing.provider.SubtypeValueProvider
import org.utbot.python.fuzzing.provider.TupleFixSizeValueProvider
import org.utbot.python.fuzzing.provider.TupleValueProvider
import org.utbot.python.fuzzing.provider.TypeAliasValueProvider
import org.utbot.python.fuzzing.provider.UnionValueProvider
import org.utbot.python.fuzzing.provider.utils.isAny
import org.utbot.python.newtyping.PythonTypeHintsStorage
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.newtyping.inference.InferredTypeFeedback
import org.utbot.python.newtyping.inference.InvalidTypeFeedback
import org.utbot.python.newtyping.inference.SuccessFeedback
import org.utbot.python.newtyping.inference.baseline.BaselineAlgorithm
import org.utbot.python.newtyping.pythonTypeRepresentation
import org.utbot.python.utils.ExecutionWithTimoutMode
import org.utbot.python.utils.FakeWithTimeoutMode
import org.utbot.python.utils.TestGenerationLimitManager
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

data class PythonFuzzedConcreteValue(
    val type: UtType,
    val value: Any,
    val fuzzedContext: FuzzedContext = FuzzedContext.Unknown,
)

class PythonMethodDescription(
    val name: String,
    val concreteValues: Collection<PythonFuzzedConcreteValue> = emptyList(),
    val pythonTypeStorage: PythonTypeHintsStorage,
    val tracer: Trie<PyInstruction, *>,
    val random: Random,
    val limitManager: TestGenerationLimitManager,
    val type: FunctionType,
) : Description<UtType>(type.arguments)

data class PythonExecutionResult(
    val executionFeedback: ExecutionFeedback,
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
    OptionalValueProvider,
    UnionValueProvider,
    BytesValueProvider,
    BytearrayValueProvider,
    ReduceValueProvider,
    RePatternValueProvider,
    ConstantValueProvider,
    TypeAliasValueProvider,
    IteratorValueProvider,
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
    private val globalIsCancelled: () -> Boolean,
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
            val d = PythonMethodDescription(
                description.name,
                description.concreteValues,
                description.pythonTypeStorage,
                description.tracer,
                description.random,
                TestGenerationLimitManager(ExecutionWithTimoutMode, description.limitManager.until),
                type as FunctionType
            )
            if (!d.limitManager.isCancelled()) {
                logger.debug { "Fork new type" }
                fork(d, stats)
            }
            logger.debug { "Fork ended" }
        } else {
            description.limitManager.mode = FakeWithTimeoutMode
        }
    }

    override suspend fun isCancelled(
        description: PythonMethodDescription,
        stats: Statistic<UtType, PythonFuzzedValue>
    ): Boolean {
        if (globalIsCancelled()) {
            return true
        }
        if (description.limitManager.isCancelled() || description.parameters.any { it.isAny() }) {
            forkType(description, stats)
            if (description.limitManager.isRootManager) {
                return FakeWithTimeoutMode.isCancelled(description.limitManager)
            }
        }
        return description.limitManager.isCancelled()
    }
}
