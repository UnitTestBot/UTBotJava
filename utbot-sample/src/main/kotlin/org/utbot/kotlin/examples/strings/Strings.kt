package org.utbot.kotlin.examples.strings

fun isNotBlank(cs: CharSequence): Boolean {
    return !isBlank(cs)
}

fun nullableStringBuffer(buffer: StringBuffer, i: Int): String {
    if (i >= 0) {
        buffer.append("Positive")
    } else {
        buffer.append("Negative")
    }
    return buffer.toString()
}

fun isValidUuid(uuid: String): Boolean {
    return isNotBlank(uuid) && uuid
        .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".toRegex())
}

fun isValidUuidShortVersion(uuid: String?): Boolean {
    return uuid != null && uuid.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".toRegex())
}

fun isBlank(cs: CharSequence): Boolean {
    val strLen = length(cs)
    if (strLen != 0) {
        for (i in 0 until strLen) {
            if (!Character.isWhitespace(cs[i])) {
                return false
            }
        }
    }
    return true
}

fun length(cs: CharSequence): Int {
    return cs.length
}
