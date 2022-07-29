package org.utbot.python

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.io.filefilter.DirectoryFileFilter
import org.apache.commons.io.filefilter.RegexFileFilter
import org.utbot.python.code.ClassInfoCollector
import org.utbot.python.code.PythonClass
import org.utbot.python.code.PythonCode
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets

object PythonCodeCollector {
    data class ProjectClass(
        val pythonClass: PythonClass,
        val info: ClassInfoCollector.Storage
    )

    var projectClasses: List<ProjectClass> = emptyList()

    fun getPythonFiles(dirPath: String): Collection<File> =
        FileUtils.listFiles(
            File(dirPath),
            RegexFileFilter("^.*[.]py"),
            DirectoryFileFilter.DIRECTORY
        )

    fun refreshProjectClassesList(dirPath: String) {
        val pythonFiles = getPythonFiles(dirPath)
        projectClasses = pythonFiles.flatMap { file ->
            val content = IOUtils.toString(FileInputStream(file), StandardCharsets.UTF_8)
            val code = PythonCode.getFromString(content, file.path)
            code.getToplevelClasses().map { pyClass ->
                val collector = ClassInfoCollector(pyClass)
                ProjectClass(pyClass, collector.storage)
            }
        }
    }
}