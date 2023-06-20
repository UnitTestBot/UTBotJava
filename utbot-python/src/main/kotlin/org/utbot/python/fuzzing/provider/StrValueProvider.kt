package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.KnownValue
import org.utbot.fuzzing.seeds.RegexValue
import org.utbot.fuzzing.seeds.StringValue
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonStrClassId
import org.utbot.python.fuzzing.PythonFuzzedConcreteValue
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.provider.utils.generateSummary
import org.utbot.python.fuzzing.provider.utils.isPattern
import org.utbot.python.fuzzing.provider.utils.transformQuotationMarks
import org.utbot.python.fuzzing.provider.utils.transformRawString
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.pythonTypeName
import kotlin.random.Random

object StrValueProvider : ValueProvider<Type, PythonFuzzedValue, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        return type.pythonTypeName() == pythonStrClassId.canonicalName
    }

    private fun getStrConstants(concreteValues: Collection<PythonFuzzedConcreteValue>): List<String> {
        return concreteValues
            .filter { accept(it.type) }
            .map {
                val value = it.value as String
                value.transformRawString()
            }
            .map {
                it.transformQuotationMarks()
            }
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence {
        val strConstants = getStrConstants(description.concreteValues) + listOf(
            "pythön",
            "foo",
            "\t\n\r",
        )
        strConstants.forEach { yieldStrings(StringValue(it)) { value } }

        strConstants
            .filter {
                it.isPattern()
            }
            .forEach {
                yieldStrings(RegexValue(it, Random(0)), StringValue::value)
            }
    }

    private suspend fun <T : KnownValue<T>> SequenceScope<Seed<Type, PythonFuzzedValue>>.yieldStrings(value: T, block: T.() -> Any) {
        yield(Seed.Known(value) {
            PythonFuzzedValue(
                PythonTree.fromString(block(it).toString()),
                it.generateSummary(),
            )
        })
    }
}
