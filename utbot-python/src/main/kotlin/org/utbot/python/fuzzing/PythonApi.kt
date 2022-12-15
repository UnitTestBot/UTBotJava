package org.utbot.python.fuzzing

import org.utbot.engine.logger
import org.utbot.fuzzer.FuzzedContext
import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzing.Control
import org.utbot.fuzzing.Description
import org.utbot.fuzzing.Feedback
import org.utbot.fuzzing.Fuzzing
import org.utbot.fuzzing.Seed
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.util.pythonNoneClassId
import org.utbot.python.fuzzing.provider.BoolValueProvider
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
import org.utbot.python.fuzzing.value.UndefValue
import org.utbot.python.newtyping.PythonProtocolDescription
import org.utbot.python.newtyping.PythonSubtypeChecker
import org.utbot.python.newtyping.PythonTypeStorage
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
    ReduceValueProvider(idGenerator)
)

class PythonFuzzing(
    private val pythonTypeStorage: PythonTypeStorage,
    val execute: suspend (description: PythonMethodDescription, values: List<PythonTreeModel>) -> PythonFeedback,
) : Fuzzing<Type, PythonTreeModel, PythonMethodDescription, PythonFeedback> {
    private fun generateDefault(description: PythonMethodDescription, type: Type, idGenerator: IdGenerator<Long>): Sequence<Seed<Type, PythonTreeModel>> {
        var providers = emptyList<Seed<Type, PythonTreeModel>>().asSequence()
        pythonDefaultValueProviders(idGenerator).asSequence().forEach { provider ->
            if (provider.accept(type)) {
                providers += provider.generate(description, type)
            }
        }
        return providers
    }

    private fun generateSubtype(description: PythonMethodDescription, type: Type, idGenerator: IdGenerator<Long>): Sequence<Seed<Type, PythonTreeModel>> {
        var providers = emptyList<Seed<Type, PythonTreeModel>>().asSequence()
        if (type.meta is PythonProtocolDescription) {
            val subtypes = pythonTypeStorage.allTypes.filter {
                PythonSubtypeChecker.checkIfRightIsSubtypeOfLeft(type, it, pythonTypeStorage)
            }
            subtypes.forEach {
                providers += generateDefault(description, it, idGenerator)
//                providers += generateSubtype(description, it, idGenerator)
            }
        }
        return providers
    }

    override fun generate(description: PythonMethodDescription, type: Type): Sequence<Seed<Type, PythonTreeModel>> {
        val idGenerator = PythonIdGenerator()
        var providers = emptyList<Seed<Type, PythonTreeModel>>().asSequence()

        providers += generateDefault(description, type, idGenerator)
        providers += generateSubtype(description, type, idGenerator)

        if (providers.toList().isEmpty()) {
            providers += Seed.Known(UndefValue()) {PythonTreeModel(PythonTree.fromNone(), PythonClassId("UNDEF_VALUE"))}
        }

        return providers
    }

    override suspend fun handle(description: PythonMethodDescription, values: List<PythonTreeModel>): PythonFeedback {
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
