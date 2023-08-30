package org.utbot.python.newtyping.mypy

fun String.modifyWindowsPath(): String {
    return if (this.contains(":")) {
        val (disk, path) = this.split(":", limit = 2)
        "\\\\localhost\\$disk$${path.replace("/", "\\")}"
    } else this
}

/**
 * Remove .__init__ suffix if it exists.
  It resolves problem with duplication module names in mypy, e.g. mod.__init__ and mod
 */
fun String.dropInitFile(): String {
    return if (this.endsWith(".__init__")) {
        this.dropLast(9)
    } else this
}