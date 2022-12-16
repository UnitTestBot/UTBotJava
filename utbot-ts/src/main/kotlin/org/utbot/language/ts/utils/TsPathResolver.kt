package org.utbot.language.ts.utils

import java.nio.file.Paths

object TsPathResolver {

    fun getRelativePath(to: String, from: String): String {
        val toPath = Paths.get(to)
        val fromPath = Paths.get(from)
        return toPath.relativize(fromPath).toString().replace("\\", "/")
    }
}
