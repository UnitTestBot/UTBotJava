package org.utbot.framework.codegen.util

import org.utbot.framework.plugin.api.util.IndentUtil.TAB

fun String.escapeControlChars() =
    replace("\b", "\\b")
        .replace("\n", "\\n")
        .replace("\t", TAB)
        .replace("\r", "\\r")
        .replace("\\u","\\\\u")