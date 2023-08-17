package org.utbot.python.fuzzing

import mu.KotlinLogging
import org.utbot.framework.plugin.api.Instruction
import org.utbot.framework.plugin.api.UtError
import org.utbot.fuzzer.FuzzedContext
import org.utbot.fuzzing.*
import org.utbot.fuzzing.utils.Trie
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.PythonUtExecution
import org.utbot.python.fuzzing.provider.*
import org.utbot.python.fuzzing.provider.utils.isAny
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.general.UtType
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

data class PythonFuzzedConcreteValue(
    val type: UtType,
    val value: Any,
    val fuzzedContext: FuzzedContext = FuzzedContext.Unknown,
)

class PythonMethodDescription(
    val name: String,
    parameters: List<UtType>,
    val concreteValues: Collection<PythonFuzzedConcreteValue> = emptyList(),
    val pythonTypeStorage: PythonTypeStorage,
    val tracer: Trie<Instruction, *>,
    val random: Random,
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
    val result: Trie.Node<Instruction> = Trie.emptyNode(),
) : Feedback<UtType, PythonFuzzedValue>

class PythonFuzzedValue(
    val tree: PythonTree.PythonTreeNode,
    val summary: String? = null,
)

fun pythonDefaultValueProviders(typeStorage: PythonTypeStorage) = listOf(
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

class PythonFuzzing(
    private val pythonTypeStorage: PythonTypeStorage,
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
        return execute(description, values)
    }
}
