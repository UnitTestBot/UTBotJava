package org.utbot.python.fuzzing

import mu.KotlinLogging
import org.utbot.framework.plugin.api.Instruction
import org.utbot.fuzzer.FuzzedContext
import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzing.Configuration
import org.utbot.fuzzing.Control
import org.utbot.fuzzing.Description
import org.utbot.fuzzing.Feedback
import org.utbot.fuzzing.Fuzzing
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.Statistic
import org.utbot.fuzzing.utils.Trie
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.fuzzing.provider.BoolValueProvider
import org.utbot.python.fuzzing.provider.BytearrayValueProvider
import org.utbot.python.fuzzing.provider.BytesValueProvider
import org.utbot.python.fuzzing.provider.ComplexValueProvider
import org.utbot.python.fuzzing.provider.ConstantValueProvider
import org.utbot.python.fuzzing.provider.DictValueProvider
import org.utbot.python.fuzzing.provider.FloatValueProvider
import org.utbot.python.fuzzing.provider.IntValueProvider
import org.utbot.python.fuzzing.provider.ListValueProvider
import org.utbot.python.fuzzing.provider.NoneValueProvider
import org.utbot.python.fuzzing.provider.ReduceValueProvider
import org.utbot.python.fuzzing.provider.SetValueProvider
import org.utbot.python.fuzzing.provider.StrValueProvider
import org.utbot.python.fuzzing.provider.TupleFixSizeValueProvider
import org.utbot.python.fuzzing.provider.TupleValueProvider
import org.utbot.python.fuzzing.provider.UnionValueProvider
import org.utbot.python.fuzzing.provider.utils.isAny
import org.utbot.python.newtyping.PythonProtocolDescription
import org.utbot.python.newtyping.PythonSubtypeChecker
import org.utbot.python.newtyping.PythonTypeStorage
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.pythonTypeRepresentation
import java.util.*
import java.util.concurrent.atomic.AtomicLong

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

data class PythonFeedback(
    override val control: Control = Control.CONTINUE,
    val result: Trie.Node<Instruction> = Trie.emptyNode(),
) : Feedback<Type, PythonFuzzedValue>

class PythonFuzzedValue(
    val tree: PythonTree.PythonTreeNode,
    val summary: String? = null,
)

fun pythonDefaultValueProviders(idGenerator: IdGenerator<Long>) = listOf(
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
)

class PythonFuzzing(
    private val pythonTypeStorage: PythonTypeStorage,
    val execute: suspend (description: PythonMethodDescription, values: List<PythonFuzzedValue>) -> PythonFeedback,
) : Fuzzing<Type, PythonFuzzedValue, PythonMethodDescription, PythonFeedback> {

    private fun generateDefault(description: PythonMethodDescription, type: Type, idGenerator: IdGenerator<Long>): Sequence<Seed<Type, PythonFuzzedValue>> {
        var providers = emptyList<Seed<Type, PythonFuzzedValue>>().asSequence()
        pythonDefaultValueProviders(idGenerator).asSequence().forEach { provider ->
            if (provider.accept(type)) {
                logger.info { "Provider ${provider.javaClass.simpleName} accepts type ${type.pythonTypeRepresentation()}" }
                providers += provider.generate(description, type)
            }
        }
        return providers
    }

    private fun generateSubtype(description: PythonMethodDescription, type: Type, idGenerator: IdGenerator<Long>): Sequence<Seed<Type, PythonFuzzedValue>> {
        var providers = emptyList<Seed<Type, PythonFuzzedValue>>().asSequence()
        if (type.meta is PythonProtocolDescription) {
            val subtypes = pythonTypeStorage.allTypes.filter {
                PythonSubtypeChecker.checkIfRightIsSubtypeOfLeft(type, it, pythonTypeStorage)
            }
            subtypes.forEach {
                providers += generateDefault(description, it, idGenerator)
            }
        }
        return providers
    }

    override fun generate(description: PythonMethodDescription, type: Type): Sequence<Seed<Type, PythonFuzzedValue>> {
        val idGenerator = PythonIdGenerator()
        var providers = emptyList<Seed<Type, PythonFuzzedValue>>().asSequence()

        if (type.isAny()) {
            logger.info("Any does not have provider")
        } else {
            providers += generateDefault(description, type, idGenerator)
            providers += generateSubtype(description, type, idGenerator)
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

class PythonIdGenerator(lowerBound: Long = DEFAULT_LOWER_BOUND) : IdentityPreservingIdGenerator<Long> {
    private val lastId: AtomicLong = AtomicLong(lowerBound)
    private val cache: IdentityHashMap<Any?, Long> = IdentityHashMap()

    override fun getOrCreateIdForValue(value: Any): Long {
        return cache.getOrPut(value) { createId() }
    }

    override fun createId(): Long {
        return lastId.incrementAndGet()
    }

    companion object {
        const val DEFAULT_LOWER_BOUND: Long = 1500_000_000
    }
}
