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