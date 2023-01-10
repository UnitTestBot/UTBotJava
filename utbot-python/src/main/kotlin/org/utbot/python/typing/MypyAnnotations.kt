package org.utbot.python.typing

import mu.KotlinLogging
import org.utbot.python.PythonMethod
import org.utbot.python.code.PythonCodeGenerator.generateMypyCheckCode
import org.utbot.python.framework.api.python.NormalizedPythonAnnotation
import org.utbot.python.utils.*
import java.io.File

private val logger = KotlinLogging.logger {}

object MypyAnnotations {
    const val TEMPORARY_MYPY_FILE = "<TEMPORARY MYPY FILE>"

    private const val configFilename = "mypy.ini"

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
        val fileWithCode = TemporaryFileManager.assignTemporaryFile(tag = "mypy.py")
        val codeWithoutAnnotations = generateMypyCheckCode(
            method,
            emptyMap(),
            directoriesForSysPath,
            moduleToImport,
            listOf("*")
        )

        TemporaryFileManager.writeToAssignedFile(fileWithCode, codeWithoutAnnotations)
        val configFile = setConfigFile(directoriesForSysPath)
        Cleaner.addFunction { stopMypy(pythonPath) }

        logger.debug("First mypy run")
        val defaultOutputAsString = mypyCheck(pythonPath, fileWithCode, configFile)
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
            return@sequence
        }

        PriorityCartesianProduct(candidates).getSequence().forEach { generatedAnnotations ->
            if (isCancelled()) {
                return@sequence
            }

            logger.debug("Checking annotations: ${
                generatedAnnotations.joinToString { "${it.first}: ${it.second}" }
            }")

            val annotationMap = generatedAnnotations.toMap()
            val codeWithAnnotations = generateMypyCheckCode(
                method,
                annotationMap,
                directoriesForSysPath,
                moduleToImport,
                listOf("*")
            )
            TemporaryFileManager.writeToAssignedFile(fileWithCode, codeWithAnnotations)

            val mypyOutputAsString = mypyCheck(pythonPath, fileWithCode, configFile)
            val mypyOutput = getErrorsAndNotes(mypyOutputAsString, codeWithAnnotations, fileWithCode)
            val errorNum = getErrorNumber(mypyOutput)

            if (errorNum <= defaultErrorNum) {
                yield(annotationMap.mapValues { entry ->
                    entry.value
                })
            }
        }
    }

    private fun setConfigFile(directoriesForSysPath: Set<String>): File {
        val file = TemporaryFileManager.assignTemporaryFile(configFilename)
        val configContent = """
            [mypy]
            mypy_path = ${directoriesForSysPath.joinToString(separator = ":")}
            namespace_packages = True
            explicit_package_bases = True
            show_absolute_path = True
            """.trimIndent()
        TemporaryFileManager.writeToAssignedFile(file, configContent)
        return file
    }

    private fun stopMypy(pythonPath: String): Int {
        val result = runCommand(
            listOf(
                pythonPath,
                "-m",
                "mypy.dmypy",
                "stop"
            )
        )
        return result.exitValue
    }

    private fun mypyCheck(pythonPath: String, fileWithCode: File, configFile: File): String {
        val result = runCommand(
            listOf(
                pythonPath,
                "-m",
                "mypy.dmypy",
                "run",
                "--",
                fileWithCode.path,
                "--config-file",
                configFile.path
            )
        )
        return result.stdout
    }

    private fun getErrorNumber(mypyReport: List<MypyReportLine>) =
        mypyReport.count { it.type == "error" && it.file == TEMPORARY_MYPY_FILE }

    private fun getErrorsAndNotes(mypyOutput: String, mypyCode: String, fileWithCode: File): List<MypyReportLine> {
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

