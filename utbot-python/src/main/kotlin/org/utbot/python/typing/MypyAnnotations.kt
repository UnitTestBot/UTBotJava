package org.utbot.python.typing

import org.utbot.framework.plugin.api.NormalizedPythonAnnotation
import org.utbot.python.utils.FileManager
import org.utbot.python.PythonMethod
import org.utbot.python.code.PythonCodeGenerator.generateMypyCheckCode
import org.utbot.python.utils.PriorityCartesianProduct
import org.utbot.python.utils.getLineOfFunction
import org.utbot.python.utils.runCommand
import java.io.File


object MypyAnnotations {
    private const val mypyVersion = "0.971"

    const val TEMPORARY_MYPY_FILE = "<TEMPORARY MYPY FILE>"

    data class MypyReportLine(
        val line: Int,
        val type: String,
        val message: String,
        val file: String
    )

    fun getCheckedByMypyAnnotations(
        method: PythonMethod,
        functionArgAnnotations: Map<String, List<NormalizedPythonAnnotation>>,
        moduleToImport: String,
        directoriesForSysPath: Set<String>,
        pythonPath: String,
        isCancelled: () -> Boolean,
        storageForMypyMessages: MutableList<MypyReportLine>? = null
    ) = sequence {
        val fileWithCode = FileManager.assignTemporaryFile(tag = "mypy")
        val codeWithoutAnnotations = generateMypyCheckCode(
            method,
            emptyMap(),
            directoriesForSysPath,
            moduleToImport
        )
        FileManager.writeToAssignedFile(fileWithCode, codeWithoutAnnotations)

        val configFile = setConfigFile(directoriesForSysPath)
        val defaultOutputAsString = runMypy(pythonPath, fileWithCode, configFile)
        val defaultErrorsAndNotes = getErrorsAndNotes(defaultOutputAsString, codeWithoutAnnotations, fileWithCode)

        if (storageForMypyMessages != null) {
            defaultErrorsAndNotes.forEach { storageForMypyMessages.add(it) }
        }

        val defaultErrorNum = getErrorNumber(defaultErrorsAndNotes)

        val candidates = functionArgAnnotations.entries.map { (key, value) ->
            value.map {
                Pair(key, it)
            }
        }
        if (candidates.any { it.isEmpty() }) {
            fileWithCode.delete()
            configFile.delete()
            return@sequence
        }

        PriorityCartesianProduct(candidates).getSequence().forEach { generatedAnnotations ->
            if (isCancelled()) {
                fileWithCode.delete()
                configFile.delete()
                return@sequence
            }

            val annotationMap = generatedAnnotations.toMap()
            val codeWithAnnotations = generateMypyCheckCode(
                method,
                annotationMap,
                directoriesForSysPath,
                moduleToImport
            )
            FileManager.writeToAssignedFile(fileWithCode, codeWithAnnotations)
            val mypyOutputAsString = runMypy(pythonPath, fileWithCode, configFile)
            val mypyOutput = getErrorsAndNotes(mypyOutputAsString, codeWithAnnotations, fileWithCode)
            val errorNum = getErrorNumber(mypyOutput)

            if (errorNum <= defaultErrorNum) {
                yield(annotationMap.mapValues { entry ->
                    entry.value
                })
            }
        }

        fileWithCode.delete()
        configFile.delete()
    }

    private const val configFilename = "mypy.ini"

    private fun setConfigFile(directoriesForSysPath: Set<String>): File {
        val file = FileManager.assignTemporaryFile(configFilename)
        val configContent = "[mypy]\nmypy_path = ${directoriesForSysPath.joinToString(separator = ":")}"
        FileManager.writeToAssignedFile(file, configContent)
        return file
    }

    private fun runMypy(
        pythonPath: String,
        fileWithCode: File,
        configFile: File
    ): String {
        val result = runCommand(listOf(
            pythonPath,
            "-m",
            "mypy.dmypy",
            "run",
            "--",
            fileWithCode.path,
            "--config-file",
            configFile.path
        ))
        return result.stdout
    }

    fun mypyInstalled(pythonPath: String): Boolean {
        val result = runCommand(listOf(pythonPath, "-m", "pip", "show", "mypy"))
        if (result.exitValue != 0)
            return false
        val regex = Regex("Version: ([0-9.]*)")
        val version = regex.find(result.stdout)?.groupValues?.getOrNull(1) ?: return false
        return version == mypyVersion
    }

    fun installMypy(pythonPath: String): Int {
        val result = runCommand(listOf(pythonPath, "-m", "pip", "install", "mypy==$mypyVersion"))
        return result.exitValue
    }

    private fun getErrorNumber(mypyReport: List<MypyReportLine>) =
        mypyReport.count { it.type == "error" && it.file == TEMPORARY_MYPY_FILE }

    fun getErrorsAndNotes(mypyOutput: String, mypyCode: String, fileWithCode: File): List<MypyReportLine> {
        val regex = Regex("(?m)^([^\n]*):([0-9]*): (error|note): ([^\n]*)\n")
        return regex.findAll(mypyOutput).toList().map { match ->
            val file = match.groupValues[1]
            MypyReportLine(
                match.groupValues[2].toInt() - getLineOfFunction(mypyCode)!!,
                match.groupValues[3],
                match.groupValues[4],
                if (file == fileWithCode.path) TEMPORARY_MYPY_FILE else file
            )
        }
    }
}

