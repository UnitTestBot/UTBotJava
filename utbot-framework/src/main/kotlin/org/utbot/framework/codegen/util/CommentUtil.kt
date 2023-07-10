package org.utbot.framework.codegen.util

const val TAB = "    "

fun String.escapeControlChars() =
    replace("\b", "\\b")
        .replace("\n", "\\n")
        .replace("\t", TAB)
        .replace("\r", "\\r")
        .replace("\\u","\\\\u")