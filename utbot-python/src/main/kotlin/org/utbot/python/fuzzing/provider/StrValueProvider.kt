package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.KnownValue
import org.utbot.fuzzing.seeds.StringValue
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.fuzzing.PythonFuzzedConcreteValue
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.provider.utils.generateSummary
import org.utbot.python.fuzzing.provider.utils.transformQuotationMarks
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.pythonTypeName

object StrValueProvider : ValueProvider<Type, PythonFuzzedValue, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        return type.pythonTypeName() == "builtins.str"
    }

    private fun getStrConstants(concreteValues: Collection<PythonFuzzedConcreteValue>): List<StringValue> {
        return concreteValues
            .filter { accept(it.type) }
            .map {
                StringValue((it.value as String).transformQuotationMarks())
            }
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence {
        val strConstants = getStrConstants(description.concreteValues) + listOf(
            StringValue("test"),
            StringValue("abc"),
        )
        strConstants.forEach { yieldStrings(it) { value } }
    }

    private suspend fun <T : KnownValue> SequenceScope<Seed<Type, PythonFuzzedValue>>.yieldStrings(value: T, block: T.() -> Any) {
        yield(Seed.Known(value) {
            PythonFuzzedValue(
                PythonTree.fromString(block(it).toString()),
                it.generateSummary(),
            )
        })
    }
}