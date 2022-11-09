package org.utbot.python.utils

import org.utbot.common.PathUtil.toPath
import java.io.File
import java.nio.file.Paths

// numeration from zero
fun getLineNumber(content: String, pos: Int) =
    content.substring(0, pos).count { it == '\n' }

fun getLineOfFunction(code: String, functionName: String? = null): Int? {
    val regex =
        if (functionName != null)
            """(?m)^def +$functionName\(""".toRegex()
        else
            """(?m)^def""".toRegex()

    val trimmedCode = code.replaceIndent()
    return regex.find(trimmedCode)?.range?.first?.let { getLineNumber(trimmedCode, it) }
}

fun String.camelToSnakeCase(): String {
    val camelRegex = "(?<=[a-zA-Z])[\\dA-Z]".toRegex()
    return camelRegex.replace(this) {
        "_${it.value}"
    }.lowercase()
}

fun moduleOfType(typeName: String): String? {
    val lastIndex = typeName.lastIndexOf('.')
    return if (lastIndex == -1) null else typeName.substring(0, lastIndex)
}

fun checkIfFileLiesInPath(path: String, fileWithClassPath: String): Boolean {
    val parentPath = Paths.get(path).toAbsolutePath()
    val childPath = Paths.get(fileWithClassPath).toAbsolutePath()
    return childPath.startsWith(parentPath)
}

fun getModuleNameWithoutCheck(path: File, fileWithClass: File): String =
    path.toURI().relativize(fileWithClass.toURI()).path.removeSuffix(".py").toPath().joinToString(".")

fun getModuleName(path: String, fileWithClassPath: String): String? {
    if (checkIfFileLiesInPath(path, fileWithClassPath))
        return getModuleNameWithoutCheck(File(path), File(fileWithClassPath))
    return null
}