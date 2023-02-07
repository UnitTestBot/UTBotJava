package org.utbot.cli.go.util

import java.io.File

fun String.toAbsolutePath(): String = File(this).canonicalPath

fun createFile(filePath: String): File = createFile(File(filePath).canonicalFile)

fun createFile(file: File): File {
    return file.also {
        it.parentFile?.mkdirs()
        it.createNewFile()
    }
}