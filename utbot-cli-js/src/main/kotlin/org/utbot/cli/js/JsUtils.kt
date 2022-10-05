package org.utbot.cli.js

import java.io.File

internal object JsUtils {

    @Suppress("NAME_SHADOWING")
    fun makeAbsolutePath(path: String): String {
        val path = path.replace("/", File.separator)
        return when {
            File(path).isAbsolute -> path
            else -> System.getProperty("user.dir") + File.separator + path
        }
    }
}