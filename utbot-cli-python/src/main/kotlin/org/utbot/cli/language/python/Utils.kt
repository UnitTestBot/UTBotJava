package org.utbot.cli.language.python

import org.utbot.python.utils.Fail
import org.utbot.python.utils.Optional
import org.utbot.python.utils.Success
import org.utbot.python.utils.getModuleName
import java.io.File

fun findCurrentPythonModule(
    directoriesForSysPath: Collection<String>,
    sourceFile: String
): Optional<String> {
    directoriesForSysPath.forEach { path ->
        val module = getModuleName(path.toAbsolutePath(), sourceFile.toAbsolutePath())
        if (module != null)
            return Success(module)
    }
    return Fail("Couldn't find path for $sourceFile in --sys-path option. Please, specify it.")
}

fun String.toAbsolutePath(): String =
    File(this).canonicalPath

fun writeToFileAndSave(filename: String, fileContent: String) {
    val file = File(filename)
    file.parentFile?.mkdirs()
    file.writeText(fileContent)
    file.createNewFile()
}
