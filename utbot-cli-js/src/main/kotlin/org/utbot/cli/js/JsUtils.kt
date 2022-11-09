package org.utbot.cli.js

import java.io.File

internal object JsUtils {

    fun makeAbsolutePath(path: String): String {
        val rawPath = when {
            File(path).isAbsolute -> path
            else -> System.getProperty("user.dir") + "/" + path
        }
        return rawPath.replace("\\", "/")
    }
}