package org.utbot.fuzzer.providers

import com.github.curiousoddman.rgxgen.RgxGen
import com.github.curiousoddman.rgxgen.config.RgxGenOption
import com.github.curiousoddman.rgxgen.config.RgxGenProperties
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.fuzzer.FuzzedContext
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldAllValues
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import kotlin.random.Random
import kotlin.random.asJavaRandom

object RegexModelProvider : ModelProvider {

    val rgxGenProperties = RgxGenProperties().apply {
        setProperty(RgxGenOption.INFINITE_PATTERN_REPETITION.key, "5")
    }

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> {
        val parameters = description.parametersMap[stringClassId]
        if (parameters.isNullOrEmpty()) {
            return emptySequence()
        }
        val regexes = description.concreteValues
            .asSequence()
            .filter { it.classId == stringClassId }
            .filter { it.fuzzedContext.isPatterMatchingContext()  }
            .map { it.value as String }
            .distinct()
            .filter { it.isNotBlank() }
            .filter {
                try {
                    Pattern.compile(it); true
                } catch (_: PatternSyntaxException) {
                    false
                }
            }.map {
                it to RgxGen(it).apply {
                    setProperties(rgxGenProperties)
                }.generate(Random(0).asJavaRandom())
            }

        return sequence {
            yieldAllValues(parameters, regexes.map {
                RegexFuzzedValue(UtPrimitiveModel(it.second).fuzzed { summary = "%var% = regex ${it.first}" }, it.first)
            })
        }
    }

    private fun FuzzedContext.isPatterMatchingContext(): Boolean {
        if (this !is FuzzedContext.Call) return false
        return when {
            method.classId == Pattern::class.java.id -> true
            method.classId == String::class.java.id && method.name == "matches" -> true
            else -> false
        }
    }
}

class RegexFuzzedValue(value: FuzzedValue, val regex: String) : FuzzedValue(value.model, value.createdBy)