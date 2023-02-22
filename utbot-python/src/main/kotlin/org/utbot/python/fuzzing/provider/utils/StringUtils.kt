package org.utbot.python.fuzzing.provider.utils

fun String.transformQuotationMarks(): String {

    val doubleQuotationMarks = this.startsWith("\"") && this.endsWith("\"")
    val oneQuotationMarks = this.startsWith("'") && this.endsWith("'")

    val tripleDoubleQuotationMarks = this.startsWith("\"\"\"") && this.endsWith("\"\"\"")
    val tripleOneQuotationMarks = this.startsWith("'''") && this.endsWith("'''")

    if (tripleOneQuotationMarks || tripleDoubleQuotationMarks) {
        return this.drop(3).dropLast(3)
    }

    if (oneQuotationMarks || doubleQuotationMarks) {
        return this.drop(1).dropLast(1)
    }

    return this
}