package org.utbot.python.fuzzing

import org.utbot.engine.logger
import org.utbot.fuzzer.FuzzedContext
import org.utbot.fuzzing.BaseFuzzing
import org.utbot.fuzzing.Control
import org.utbot.fuzzing.Description
import org.utbot.fuzzing.Feedback
import org.utbot.fuzzing.Fuzzing
import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.fuzz
import org.utbot.fuzzing.providers.StringValueProvider
import org.utbot.fuzzing.seeds.BitVectorValue
import org.utbot.fuzzing.seeds.Bool
import org.utbot.fuzzing.seeds.KnownValue
import org.utbot.fuzzing.seeds.Signed
import org.utbot.fuzzing.seeds.StringValue
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.util.pythonBoolClassId
import org.utbot.python.framework.api.python.util.pythonDictClassId
import org.utbot.python.framework.api.python.util.pythonFloatClassId
import org.utbot.python.framework.api.python.util.pythonIntClassId
import org.utbot.python.framework.api.python.util.pythonListClassId
import org.utbot.python.framework.api.python.util.pythonNoneClassId
import org.utbot.python.framework.api.python.util.pythonSetClassId
import org.utbot.python.framework.api.python.util.pythonStrClassId
import org.utbot.python.framework.api.python.util.pythonTupleClassId
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
import org.utbot.python.newtyping.PythonAnyTypeDescription
import org.utbot.python.newtyping.PythonCompositeTypeDescription
import org.utbot.python.newtyping.PythonConcreteCompositeTypeDescription
import org.utbot.python.newtyping.PythonNoneTypeDescription
import org.utbot.python.newtyping.PythonOverloadTypeDescription
import org.utbot.python.newtyping.PythonProtocolDescription
import org.utbot.python.newtyping.PythonSpecialAnnotation
import org.utbot.python.newtyping.PythonTupleTypeDescription
import org.utbot.python.newtyping.PythonUnionTypeDescription
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

fun pythonDefaultValueProviders() = listOf(
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
)

class PythonFuzzing(
    val execute: suspend (description: PythonMethodDescription, values: List<PythonTreeModel>) -> PythonFeedback
) : Fuzzing<Type, PythonTreeModel, PythonMethodDescription, PythonFeedback> {
    override fun generate(description: PythonMethodDescription, type: Type): Sequence<Seed<Type, PythonTreeModel>> {
        return pythonDefaultValueProviders().asSequence().flatMap { provider ->
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