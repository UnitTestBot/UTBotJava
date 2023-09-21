package org.utbot.python.utils

import org.utbot.common.FileUtil
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

object TemporaryFileManager {
    private var tmpDirectory: Path
    private var nextId = 0

    init {
        tmpDirectory = initialize()
    }

    fun initialize(): Path {
        tmpDirectory = FileUtil.createTempDirectory("python-test-generation-${nextId++}")
        Cleaner.addFunction { tmpDirectory.toFile().deleteRecursively() }
        return tmpDirectory
    }

    fun assignTemporaryFile(fileName_: String? = null, tag: String? = null, addToCleaner: Boolean = true): File {
        val fileName = fileName_ ?: ("tmp_${nextId++}_" + (tag ?: ""))
        val fullpath = Paths.get(tmpDirectory.toString(), fileName)
        val result = fullpath.toFile()
        if (addToCleaner)
            Cleaner.addFunction { result.delete() }
        return result
    }

    fun writeToAssignedFile(file: File, content: String) {
        file.writeText(content)
        file.parentFile?.mkdirs()
        file.createNewFile()
    }

    fun createTemporaryFile(
        content: String,
        fileName: String? = null,
        tag: String? = null,
        addToCleaner: Boolean = true
    ): File {
        val file = assignTemporaryFile(fileName, tag, addToCleaner)
        writeToAssignedFile(file, content)
        return file
    }
}