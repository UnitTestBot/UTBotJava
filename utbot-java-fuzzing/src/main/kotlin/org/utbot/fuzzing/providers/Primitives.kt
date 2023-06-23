package org.utbot.fuzzing.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.*
import org.utbot.fuzzer.FuzzedContext
import org.utbot.fuzzer.FuzzedContext.Comparison.*
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.fuzzed
import org.utbot.fuzzing.*
import org.utbot.fuzzing.seeds.*
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import kotlin.random.Random

abstract class PrimitiveValueProvider(
    vararg acceptableTypes: ClassId
) : ValueProvider<FuzzedType, FuzzedValue, FuzzedDescription> {
    protected val acceptableTypes = acceptableTypes.toSet()

    final override fun accept(type: FuzzedType) = type.classId in acceptableTypes

    protected suspend fun <T : KnownValue<T>> SequenceScope<Seed<FuzzedType, FuzzedValue>>.yieldKnown(
        value: T,
        toValue: T.() -> Any
    ) {
        yield(Seed.Known(value) { known ->
            UtPrimitiveModel(toValue(known)).fuzzed {
                summary = buildString {
                    append("%var% = ${known.valueToString()}")
                    if (known.mutatedFrom != null) {
                        append(" (mutated from ${known.mutatedFrom?.valueToString()})")
                    }
                }
            }
        })
    }

    private fun <T : KnownValue<T>> T.valueToString(): String {
        when (this) {
            is BitVectorValue -> {
                for (defaultBound in Signed.values()) {
                    if (defaultBound.test(this)) {
                        return defaultBound.name.lowercase()
                    }
                }
                return when (size) {
                    8 -> toByte().toString()
                    16 -> toShort().toString()
                    32 -> toInt().toString()
                    64 -> toLong().toString()
                    else -> toString(10)
                }
            }
            is IEEE754Value -> {
                for (defaultBound in DefaultFloatBound.values()) {
                    if (defaultBound.test(this)) {
                        return defaultBound.name.lowercase().replace("_", " ")
                    }
                }
                return when {
                    is32Float() -> toFloat().toString()
                    is64Float() -> toDouble().toString()
                    else -> toString()
                }
            }
            is RegexValue -> {
                return "'${value.substringToLength(10, "...")}' from $pattern"
            }
            is StringValue -> {
                return "'${value.substringToLength(10, "...")}'"
            }
            else -> return toString()
        }
    }

    private fun String.substringToLength(size: Int, postfix: String): String {
        return when {
            length <= size -> this
            else -> substring(0, size) + postfix
        }
    }
}

object BooleanValueProvider : PrimitiveValueProvider(booleanClassId, booleanWrapperClassId) {
    override fun generate(description: FuzzedDescription, type: FuzzedType) = sequence {
        yieldKnown(Bool.TRUE()) { toBoolean() }
        yieldKnown(Bool.FALSE()) { toBoolean() }
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

    private val randomStubWithNoUsage = Random(0)
    private val configurationStubWithNoUsage = Configuration()

    private fun BitVectorValue.change(func: BitVectorValue.() -> Unit): BitVectorValue {
        return Mutation<KnownValue<*>> { _, _, _ ->
            BitVectorValue(this).apply { func() }
        }.mutate(this, randomStubWithNoUsage, configurationStubWithNoUsage) as BitVectorValue
    }

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
                        EQ, NE, LE, GT -> value.change { inc() }
                        LT, GE -> value.change { dec() }
                        else -> null
                    }
                )
                values.forEach {
                    if (type.classId.tryCast(it) != null) {
                        yieldKnown(it) {
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
                yieldKnown(value) {
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
                yieldKnown(IEEE754Value.fromValue(v)) { type.classId.cast(this) }
            }
        }
        DefaultFloatBound.values().forEach { bound ->
            val (m, e) = type.classId.typeSize
            yieldKnown(bound(m ,e)) {
                type.classId.cast(this)
            }
        }
    }
}

object StringValueProvider : PrimitiveValueProvider(stringClassId, java.lang.CharSequence::class.java.id) {
    override fun generate(
        description: FuzzedDescription,
        type: FuzzedType
    ) = sequence {
        val constants = description.constants
            .filter { it.classId == stringClassId }
        val values = constants
            .mapNotNull { it.value as? String } +
                sequenceOf("", "abc", "XZ", "#$\\\"'", "\n\t\r", "10", "-3")
        values.forEach { yieldKnown(StringValue(it), StringValue::value) }
        constants
            .filter { it.fuzzedContext.isPatterMatchingContext()  }
            .map { it.value as String }
            .distinct()
            .filter(String::isSupportedPattern)
            .forEach {
                yieldKnown(RegexValue(it, Random(0)), StringValue::value)
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