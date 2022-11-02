package org.utbot.fuzzing.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.*
import org.utbot.fuzzer.FuzzedContext
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.providers.ConstantsModelProvider.fuzzed
import org.utbot.fuzzing.FuzzedDescription
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.*
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import kotlin.random.Random

abstract class PrimitiveValueProvider(
    vararg acceptableTypes: ClassId
) : ValueProvider<FuzzedType, FuzzedValue, FuzzedDescription> {
    protected val acceptableTypes = acceptableTypes.toSet()

    final override fun accept(type: FuzzedType) = type.classId in acceptableTypes

    protected suspend fun <T : KnownValue> SequenceScope<Seed<FuzzedType, FuzzedValue>>.yieldKnown(
        value: T,
        description: String = value.toString(),
        toValue: T.() -> Any
    ) {
        yield(Seed.Known(value) { known ->
            UtPrimitiveModel(toValue(known)).fuzzed {
                summary = buildString {
                    append("%var% = ")
                    if (known.mutatedFrom != null) {
                        append("mutated from ")
                    }
                    append(description)
                }
            }
        })
    }
}

object BooleanValueProvider : PrimitiveValueProvider(booleanClassId, booleanWrapperClassId) {
    override fun generate(description: FuzzedDescription, type: FuzzedType) = sequence {
        yieldKnown(Bool.TRUE(), description = "true") { toBoolean() }
        yieldKnown(Bool.FALSE(), description = "false") { toBoolean() }
    }
}

object IntegerValueProvider : PrimitiveValueProvider(
    charClassId, charWrapperClassId,
    byteClassId, byteWrapperClassId,
    shortClassId, shortWrapperClassId,
    intClassId, intWrapperClassId,
    longClassId, longWrapperClassId,
) {

    private val ClassId.typeSize: Int
        get() = when (this) {
            charClassId, charWrapperClassId -> 7
            byteClassId, byteWrapperClassId -> 8
            shortClassId, shortWrapperClassId -> 16
            intClassId, intWrapperClassId -> 32
            longClassId, longWrapperClassId -> 64
            else -> error("unknown type $this")
        }

    /**
     * Returns null when values cannot be cast.
     */
    private fun ClassId.tryCast(value: BitVectorValue): Any? = when (this) {
        charClassId, charWrapperClassId -> value.takeIf { typeSize <= charClassId.typeSize }?.toCharacter()
        byteClassId, byteWrapperClassId -> value.takeIf { typeSize <= byteClassId.typeSize }?.toByte()
        shortClassId, shortWrapperClassId -> value.takeIf { typeSize <= shortClassId.typeSize }?.toShort()
        intClassId, intWrapperClassId -> value.takeIf { typeSize <= intClassId.typeSize }?.toInt()
        longClassId, longWrapperClassId -> value.takeIf { typeSize <= longClassId.typeSize }?.toLong()
        else -> error("unknown type $this")
    }

    private fun ClassId.cast(value: BitVectorValue): Any = tryCast(value)!!

    override fun generate(
        description: FuzzedDescription,
        type: FuzzedType
    ) = sequence {
        description.constants.forEach { (t, v, c) ->
            if (t in acceptableTypes) {
                val value = BitVectorValue.fromValue(v)
                val values = listOfNotNull(
                    value,
                    when (c) {
                        FuzzedContext.Comparison.EQ, FuzzedContext.Comparison.NE, FuzzedContext.Comparison.LE, FuzzedContext.Comparison.GT -> BitVectorValue(value).apply { inc() }
                        FuzzedContext.Comparison.LT, FuzzedContext.Comparison.GE -> BitVectorValue(value).apply { dec() }
                        else -> null
                    }
                )
                values.forEach {
                    if (type.classId.tryCast(it) != null) {
                        yieldKnown(it, description = "$it") {
                            type.classId.cast(this)
                        }
                    }
                }

            }
        }
        Signed.values().forEach { bound ->
            val s = type.classId.typeSize
            val value = bound(s)
            if (type.classId.tryCast(value) != null) {
                yieldKnown(value, description = bound.name.lowercase().replace("_", " ")) {
                    type.classId.cast(this)
                }
            }
        }
    }
}

object FloatValueProvider : PrimitiveValueProvider(
    floatClassId, floatWrapperClassId,
    doubleClassId, doubleWrapperClassId,
) {
    private val ClassId.typeSize: Pair<Int, Int>
        get() = when (this) {
            floatClassId, floatWrapperClassId -> 23 to 8
            doubleClassId, doubleWrapperClassId -> 52 to 11
            else -> error("unknown type $this")
        }

    /**
     * Returns null when values cannot be cast.
     */
    private fun ClassId.cast(value: IEEE754Value): Any = when (this) {
        floatClassId, floatWrapperClassId -> value.toFloat()
        doubleClassId, doubleWrapperClassId -> value.toDouble()
        else -> error("unknown type $this")
    }

    override fun generate(
        description: FuzzedDescription,
        type: FuzzedType
    ) = sequence {
        description.constants.forEach { (t, v, _) ->
            if (t in acceptableTypes) {
                yieldKnown(IEEE754Value.fromValue(v), description = "$v") { type.classId.cast(this) }
            }
        }
        DefaultFloatBound.values().forEach { bound ->
            val (m, e) = type.classId.typeSize
            yieldKnown(bound(m ,e), description = bound.name.lowercase().replace("_", " ")) {
                type.classId.cast(this)
            }
        }
    }
}

object StringValueProvider : PrimitiveValueProvider(stringClassId) {
    override fun generate(
        description: FuzzedDescription,
        type: FuzzedType
    ) = sequence {
        val constants = description.constants
            .filter { it.classId == stringClassId }
        val values = constants
            .mapNotNull { it.value as? String } +
                sequenceOf("", "abc", "\n\t\r")
        values.forEach { yieldKnown(StringValue(it), description = "predefined string") { value } }
        constants
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
            }.forEach {
                yieldKnown(RegexValue(it, Random(0)), description = "regex($it)") { value }
            }
    }

    private fun FuzzedContext.isPatterMatchingContext(): Boolean {
        if (this !is FuzzedContext.Call) return false
        val stringMethodWithRegexArguments = setOf("matches", "split")
        return when {
            method.classId == Pattern::class.java.id -> true
            method.classId == String::class.java.id && stringMethodWithRegexArguments.contains(method.name) -> true
            else -> false
        }
    }
}