package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.seeds.KnownValue
import org.utbot.fuzzing.seeds.RegexValue
import org.utbot.fuzzing.seeds.StringValue
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonStrClassId
import org.utbot.python.fuzzing.FuzzedUtType
import org.utbot.python.fuzzing.FuzzedUtType.Companion.toFuzzed
import org.utbot.python.fuzzing.PythonFuzzedConcreteValue
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.PythonValueProvider
import org.utbot.python.fuzzing.provider.utils.generateSummary
import org.utbot.python.fuzzing.provider.utils.isPattern
import org.utbot.python.fuzzing.provider.utils.transformQuotationMarks
import org.utbot.python.fuzzing.provider.utils.transformRawString

object StrValueProvider : PythonValueProvider {
    override fun accept(type: FuzzedUtType): Boolean {
        return type.pythonTypeName() == pythonStrClassId.canonicalName
    }

    private fun getConstants(concreteValues: Collection<PythonFuzzedConcreteValue>): List<String> {
        return concreteValues
            .filter { accept(it.type.toFuzzed()) }
            .map { it.value as String }
    }

    private fun getStrConstants(concreteValues: Collection<PythonFuzzedConcreteValue>): List<String> {
        return getConstants(concreteValues)
            .filterNot { it.isPattern() }
            .map { it.transformQuotationMarks() }
    }

    private fun getRegexConstants(concreteValues: Collection<PythonFuzzedConcreteValue>): List<String> {
        return getConstants(concreteValues)
            .filter { it.isPattern() }
            .map { it.transformRawString().transformQuotationMarks() }
    }

    override fun generate(description: PythonMethodDescription, type: FuzzedUtType) = sequence {
        val strConstants = getStrConstants(description.concreteValues) + listOf(
            "pythön",
            "foo",
            "",
        )
        strConstants.forEach { yieldStrings(StringValue(it)) { value } }

        val regexConstants = getRegexConstants(description.concreteValues)
        regexConstants.forEach {
            val maxLength = listOf(16, 256, 2048).random(description.random).coerceAtLeast(it.length)
            yieldStrings(RegexValue(it, description.random, maxLength), StringValue::value)
        }
    }

    private suspend fun <T : KnownValue<T>> SequenceScope<Seed<FuzzedUtType, PythonFuzzedValue>>.yieldStrings(value: T, block: T.() -> Any) {
        yield(Seed.Known(value) {
            PythonFuzzedValue(
                PythonTree.fromString(block(it).toString()),
                it.generateSummary(),
            )
        })
    }
}
