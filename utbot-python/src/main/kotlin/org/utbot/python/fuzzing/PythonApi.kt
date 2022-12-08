package org.utbot.python.fuzzing

import org.utbot.engine.logger
import org.utbot.fuzzer.FuzzedContext
import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzing.Control
import org.utbot.fuzzing.Description
import org.utbot.fuzzing.Feedback
import org.utbot.fuzzing.Fuzzing
import org.utbot.fuzzing.Seed
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.fuzzing.provider.BoolValueProvider
import org.utbot.python.fuzzing.provider.DictValueProvider
import org.utbot.python.fuzzing.provider.FloatValueProvider
import org.utbot.python.fuzzing.provider.IntValueProvider
import org.utbot.python.fuzzing.provider.ListValueProvider
import org.utbot.python.fuzzing.provider.NoneValueProvider
import org.utbot.python.fuzzing.provider.SetValueProvider
import org.utbot.python.fuzzing.provider.StrValueProvider
import org.utbot.python.fuzzing.provider.TupleFixSizeValueProvider
import org.utbot.python.fuzzing.provider.TupleValueProvider
import org.utbot.python.fuzzing.provider.UnionValueProvider
import org.utbot.python.newtyping.general.Type

data class PythonFuzzedConcreteValue(
    val classId: Type,
    val value: Any,
    val fuzzedContext: FuzzedContext = FuzzedContext.Unknown,
)

class PythonMethodDescription(
    val name: String,
    parameters: List<Type>,
    val concreteValues: Collection<PythonFuzzedConcreteValue> = emptyList()
) : Description<Type>(parameters)

class PythonFeedback(
    override val control: Control = Control.CONTINUE
) : Feedback<Type, PythonTreeModel> {
    override fun equals(other: Any?): Boolean {
        val castOther = other as? PythonFeedback
        return control == castOther?.control
    }

    override fun hashCode(): Int {
        return control.hashCode()
    }
}

fun pythonDefaultValueProviders(idGenerator: IdGenerator<Long>) = listOf(
    NoneValueProvider,
    BoolValueProvider,
    IntValueProvider,
    FloatValueProvider,
    StrValueProvider,
    ListValueProvider,
    SetValueProvider,
    DictValueProvider,
    TupleValueProvider,
    TupleFixSizeValueProvider,
    UnionValueProvider,
//    ReduceValueProvider(idGenerator)
)

class PythonFuzzing(
    val execute: suspend (description: PythonMethodDescription, values: List<PythonTreeModel>) -> PythonFeedback
) : Fuzzing<Type, PythonTreeModel, PythonMethodDescription, PythonFeedback> {
    override fun generate(description: PythonMethodDescription, type: Type): Sequence<Seed<Type, PythonTreeModel>> {
        val idGenerator = PythonIdGenerator()
        return pythonDefaultValueProviders(idGenerator).asSequence().flatMap { provider ->
            try {
                if (provider.accept(type)) {
                    provider.generate(description, type)
                } else {
                    emptySequence()
                }
            } catch (t: Throwable) {
                logger.error(t) { "Error occurs in value provider: $provider" }
                emptySequence()
            }
        }
    }

    override suspend fun run(description: PythonMethodDescription, values: List<PythonTreeModel>): PythonFeedback {
        return execute(description, values)
    }
}

class PythonIdGenerator : IdGenerator<Long> {
    private var _id: Long = 0

    override fun createId(): Long {
        _id += 1
        return _id
    }

}
