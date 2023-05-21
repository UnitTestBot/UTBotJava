package org.utbot.spring.utils

import kotlin.io.path.Path

object PathsUtils {
    const val FILE_PREFIX = "file:"
    const val CLASSPATH_PREFIX = "classpath:"

    fun createFakeFilePath(fileName: String): String = "fake_${Path(fileName).fileName}"

    fun deletePathPrefix(path: String): String {
        var newPath = path
        newPath = newPath.removePrefix(FILE_PREFIX)
        newPath = newPath.removePrefix(CLASSPATH_PREFIX)
        return newPath
    }

    fun getPathPrefix(path: String): String {
        if(path.startsWith(FILE_PREFIX)) return FILE_PREFIX
        if(path.startsWith(CLASSPATH_PREFIX)) return CLASSPATH_PREFIX
        return ""
    }
}
