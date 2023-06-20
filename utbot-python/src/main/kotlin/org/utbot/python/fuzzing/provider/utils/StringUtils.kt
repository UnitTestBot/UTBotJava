package org.utbot.python.fuzzing.provider.utils

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

fun String.transformQuotationMarks(): String {

    val doubleQuotationMarks = this.startsWith("\"") && this.endsWith("\"")
//    val oneQuotationMarks = this.startsWith("'") && this.endsWith("'")

    val tripleDoubleQuotationMarks = this.startsWith("\"\"\"") && this.endsWith("\"\"\"")
    val tripleOneQuotationMarks = this.startsWith("'''") && this.endsWith("'''")

    if (tripleOneQuotationMarks || tripleDoubleQuotationMarks) {
        return this.drop(3).dropLast(3)
    }

    if (doubleQuotationMarks) {
        return this.drop(1).dropLast(1)
    }

    return this
}

fun String.transformRawString(): String {
    return if (Pattern.matches("r\".*\"", this) || Pattern.matches("r'.*'", this)) {
        this.drop(2).dropLast(1)
    } else {
        this
    }
}

fun String.isPattern(): Boolean {
    return try {
        Pattern.compile(this); true
    } catch (_: PatternSyntaxException) {
        false
    }
}
