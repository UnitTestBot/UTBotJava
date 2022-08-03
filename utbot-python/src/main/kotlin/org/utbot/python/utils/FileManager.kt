package org.utbot.python.utils

import java.io.File
import java.nio.file.Paths

object FileManager {
    private var testSourceRoot: String = ""
    private const val tmpFolderName = ".tmp"
    private var nextId = 0

    fun assignTestSourceRoot(testSourceRoot: String) {
        this.testSourceRoot = testSourceRoot
        val tmpFolder = Paths.get(testSourceRoot, tmpFolderName).toFile()
        if (!tmpFolder.exists())
            tmpFolder.mkdir()
    }

    fun assignTemporaryFile(fileName_: String? = null, tag: String? = null): File {
        val fileName = fileName_ ?: ("${nextId++}." + (tag ?: ""))
        val fullpath = Paths.get(testSourceRoot, tmpFolderName, fileName)
        return fullpath.toFile()
    }

    fun writeToAssignedFile(file: File, content: String) {
        file.writeText(content)
        file.createNewFile()
    }

    fun createTemporaryFile(content: String, fileName: String? = null, tag: String? = null): File {
        val file = assignTemporaryFile(fileName, tag)
        writeToAssignedFile(file, content)
        return file
    }
}