package org.utbot.python.utils

// numeration from zero
fun getLineNumber(content: String, pos: Int) =
    content.substring(0, pos).count { it == '\n' }

fun getLineOfFunction(code: String, functionName: String? = null): Int? {
    val regex =
        if (functionName != null)
            """(?m)^def +$functionName\(""".toRegex()
        else
            """(?m)^def""".toRegex()

    val trimmedCode = code.replaceIndent()
    return regex.find(trimmedCode)?.range?.first?.let { getLineNumber(trimmedCode, it) }
}

fun String.camelToSnakeCase(): String {
    val camelRegex = "(?<=[a-zA-Z])[\\dA-Z]".toRegex()
    return camelRegex.replace(this) {
        "_${it.value}"
    }.toLowerCase()
}

fun moduleOfType(typeName: String): String? {
    val lastIndex = typeName.lastIndexOf('.')
    return if (lastIndex == -1) null else typeName.substring(0, lastIndex)
}