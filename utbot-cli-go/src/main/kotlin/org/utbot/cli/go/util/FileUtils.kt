package org.utbot.cli.go.util

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

fun String.toAbsolutePath(): Path = Paths.get(this).toAbsolutePath()

fun createFile(filePath: String): File = createFile(File(filePath).canonicalFile)

fun createFile(file: File): File {
    return file.also {
        it.parentFile?.mkdirs()
        it.createNewFile()
    }
}