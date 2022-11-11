package org.utbot.cli.ts

import java.io.File

internal object TsUtils {

    fun makeAbsolutePath(path: String): String {
//        println("I got \"$path\", user.dir is ${System.getProperty("user.dir")}")
        var rawPath = when {
            File(path).isAbsolute -> ""
            else -> System.getProperty("user.dir") + if (path != "") "/" else ""
        }
        rawPath += path
        return rawPath.replace("\\", "/")
    }
}