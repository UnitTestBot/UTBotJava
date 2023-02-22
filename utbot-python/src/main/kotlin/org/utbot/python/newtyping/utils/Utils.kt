package org.utbot.python.newtyping.utils

fun getOffsetLine(sourceFileContent: String, offset: Int): Int {
    return sourceFileContent.take(offset).count { it == '\n' } + 1
}
