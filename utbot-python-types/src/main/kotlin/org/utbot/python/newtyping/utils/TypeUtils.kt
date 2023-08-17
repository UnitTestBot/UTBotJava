package org.utbot.python.newtyping.utils

import org.utbot.python.newtyping.PythonCallableTypeDescription

fun getOffsetLine(sourceFileContent: String, offset: Int): Int {
    return sourceFileContent.take(offset).count { it == '\n' } + 1
}

fun isRequired(kind: PythonCallableTypeDescription.ArgKind) =
    listOf(PythonCallableTypeDescription.ArgKind.ARG_POS, PythonCallableTypeDescription.ArgKind.ARG_NAMED).contains(kind)

fun isNamed(kind: PythonCallableTypeDescription.ArgKind) =
    listOf(PythonCallableTypeDescription.ArgKind.ARG_NAMED_OPT, PythonCallableTypeDescription.ArgKind.ARG_NAMED).contains(kind)