package org.utbot.python.framework.api.python.util


fun moduleOfType(typeName: String): String? {
    val lastIndex = typeName.lastIndexOf('.')
    return if (lastIndex == -1) null else typeName.substring(0, lastIndex)
}

fun String.toSnakeCase(): String {
    val splitSymbols = "_"
    return this.mapIndexed { index: Int, c: Char ->
        if (c.isLowerCase() || c.isDigit() || splitSymbols.contains(c)) c
        else if (c.isUpperCase()) {
            (if (index > 0) "_" else "") + c.lowercase()
        } else c
    }.joinToString("")
}

fun String.toPythonRepr(): String {
    val repr = this
        .replace("\"", "\\\"")
        .replace("\\\\\"", "\\\"")
    return "\"$repr\""
}
