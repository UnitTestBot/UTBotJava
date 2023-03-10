package org.utbot.python.fuzzing

import mu.KotlinLogging
import org.utbot.framework.plugin.api.Instruction
import org.utbot.framework.plugin.api.UtError
import org.utbot.fuzzer.FuzzedContext
import org.utbot.fuzzer.UtFuzzedExecution
import org.utbot.fuzzing.Configuration
import org.utbot.fuzzing.Control
import org.utbot.fuzzing.Description
import org.utbot.fuzzing.Feedback
import org.utbot.fuzzing.Fuzzing
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.Statistic
import org.utbot.fuzzing.utils.Trie
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.fuzzing.provider.*
import org.utbot.python.fuzzing.provider.utils.isAny
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.general.Type

private val logger = KotlinLogging.logger {}

data class PythonFuzzedConcreteValue(
    val type: Type,
    val value: Any,
    val fuzzedContext: FuzzedContext = FuzzedContext.Unknown,
)

class PythonMethodDescription(
    val name: String,
    parameters: List<Type>,
    val concreteValues: Collection<PythonFuzzedConcreteValue> = emptyList(),
    val pythonTypeStorage: PythonTypeStorage,
    val tracer: Trie<Instruction, *>,
) : Description<Type>(parameters)

sealed interface FuzzingExecutionFeedback
class ValidExecution(val utFuzzedExecution: UtFuzzedExecution): FuzzingExecutionFeedback
class InvalidExecution(val utError: UtError): FuzzingExecutionFeedback
class TypeErrorFeedback(val message: String) : FuzzingExecutionFeedback
class ArgumentsTypeErrorFeedback(val message: String) : FuzzingExecutionFeedback

data class PythonExecutionResult(
    val fuzzingExecutionFeedback: FuzzingExecutionFeedback,
    val fuzzingPlatformFeedback: PythonFeedback
)

data class PythonFeedback(
    override val control: Control = Control.CONTINUE,
    val result: Trie.Node<Instruction> = Trie.emptyNode(),
) : Feedback<Type, PythonFuzzedValue>

class PythonFuzzedValue(
    val tree: PythonTree.PythonTreeNode,
    val summary: String? = null,
)

fun pythonDefaultValueProviders() = listOf(
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
    ConstantValueProvider,
    TypeAliasValueProvider
)

class PythonFuzzing(
    private val pythonTypeStorage: PythonTypeStorage,
    val execute: suspend (description: PythonMethodDescription, values: List<PythonFuzzedValue>) -> PythonFeedback,
) : Fuzzing<Type, PythonFuzzedValue, PythonMethodDescription, PythonFeedback> {

    private fun generateDefault(description: PythonMethodDescription, type: Type): Sequence<Seed<Type, PythonFuzzedValue>> {
        var providers = emptyList<Seed<Type, PythonFuzzedValue>>().asSequence()
        pythonDefaultValueProviders().asSequence().forEach { provider ->
            if (provider.accept(type)) {
                logger.debug { "Provider ${provider.javaClass.simpleName} accepts type ${type.pythonTypeRepresentation()}" }
                providers += provider.generate(description, type)
            }
        }
        return providers
    }

    private fun generateSubtype(description: PythonMethodDescription, type: Type): Sequence<Seed<Type, PythonFuzzedValue>> {
        var providers = emptyList<Seed<Type, PythonFuzzedValue>>().asSequence()
        if (type.meta is PythonProtocolDescription || ((type.meta as? PythonConcreteCompositeTypeDescription)?.isAbstract == true)) {
            val subtypes = pythonTypeStorage.allTypes.filter {
                PythonSubtypeChecker.checkIfRightIsSubtypeOfLeft(type, it, pythonTypeStorage)
            }
            subtypes.forEach {
                providers += generateDefault(description, it)
            }
        }
        return providers
    }

    override fun generate(description: PythonMethodDescription, type: Type): Sequence<Seed<Type, PythonFuzzedValue>> {
        var providers = emptyList<Seed<Type, PythonFuzzedValue>>().asSequence()

        if (type.isAny()) {
            logger.debug("Any does not have provider")
        } else {
            providers += generateDefault(description, type)
            providers += generateSubtype(description, type)
        }

        return providers
    }

    override suspend fun handle(description: PythonMethodDescription, values: List<PythonFuzzedValue>): PythonFeedback {
        return execute(description, values)
    }

    override suspend fun update(
        description: PythonMethodDescription,
        statistic: Statistic<Type, PythonFuzzedValue>,
        configuration: Configuration
    ) {
        super.update(description, statistic, configuration)
    }
}
