package org.utbot.python.newtyping.mypy

fun String.modifyWindowsPath(): String {
    return if (this.contains(":")) {
        val (disk, path) = this.split(":", limit = 2)
        "\\\\localhost\\$disk$${path.replace("/", "\\")}"
    } else this
}