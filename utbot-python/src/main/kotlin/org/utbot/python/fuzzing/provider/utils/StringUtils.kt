package org.utbot.python.fuzzing.provider.utils

import org.utbot.fuzzing.seeds.isSupportedPattern

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
    return if (this.isRawString()) {
        this.substring(2, this.length-1)
    } else {
        this
    }
}

fun String.makeRawString(): String {
    return if (this.isRawString()) {
        this
    } else {
        "r${this}"
    }
}

fun String.isRawString(): Boolean {
    val rawStringWithDoubleQuotationMarks = this.startsWith("r\"") && this.endsWith("\"")
    val rawStringWithOneQuotationMarks = this.startsWith("r'") && this.endsWith("'")
    return rawStringWithOneQuotationMarks || rawStringWithDoubleQuotationMarks
}

fun String.isPattern(): Boolean {
    return if (this.isRawString()) {
        val stringContent = this.transformRawString()
        return stringContent.isSupportedPattern()
    } else false
}
