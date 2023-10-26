package org.utbot.python.utils

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
