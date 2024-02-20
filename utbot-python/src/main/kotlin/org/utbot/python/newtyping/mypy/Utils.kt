package org.utbot.python.newtyping.mypy

/**
 * Remove .__init__ suffix if it exists.
It resolves problem with duplication module names in mypy, e.g. mod.__init__ and mod
 */
fun String.dropInitFile(): String {
    return if (this.endsWith(".__init__")) {
        this.dropLast(9)
    } else this
}