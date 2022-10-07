package org.utbot.python.utils

import org.utbot.common.FileUtil
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.deleteExisting

object TemporaryFileManager {
    private lateinit var tmpDirectory: Path
    private var nextId = 0

    fun setup() {
        tmpDirectory = FileUtil.createTempDirectory("python-test-generation-${nextId++}")
        Cleaner.addFunction { tmpDirectory.deleteExisting() }
    }

    fun assignTemporaryFile(fileName_: String? = null, tag: String? = null, addToCleaner: Boolean = true): File {
        val fileName = fileName_ ?: ("${nextId++}_" + (tag ?: ""))
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