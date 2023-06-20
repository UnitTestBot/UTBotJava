package org.utbot.python.fuzzing.provider.utils

import org.utbot.fuzzing.seeds.BitVectorValue
import org.utbot.fuzzing.seeds.DefaultFloatBound
import org.utbot.fuzzing.seeds.IEEE754Value
import org.utbot.fuzzing.seeds.KnownValue
import org.utbot.fuzzing.seeds.Signed
import org.utbot.fuzzing.seeds.StringValue

fun <T : KnownValue<T>> T.valueToString(): String {
    when (this) {
        is BitVectorValue -> {
            for (defaultBound in Signed.values()) {
                if (defaultBound.test(this)) {
                    return defaultBound.name.lowercase()
                }
            }
            return when (size) {
                1 -> get(0).toString().uppercase()
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
        is StringValue -> {
            if (value.contains("\"\"\"")) {
                val newValue = value.replace("\"", "\\\"")
                return "'$newValue'"
            }
            return "'$value'"
        }
        else -> return toString()
    }
}

fun <T: KnownValue<T>> T.generateSummary(): String {
    return buildString {
        append("%var% = ${valueToString()}")
        if (mutatedFrom != null) {
            append(" (mutated from ${mutatedFrom?.valueToString()})")
        }
    }
}