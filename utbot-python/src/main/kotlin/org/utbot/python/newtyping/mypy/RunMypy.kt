package org.utbot.python.newtyping.mypy

import org.utbot.python.PythonMethod
import org.utbot.python.code.PythonCodeGenerator.generateMypyCheckCode
import org.utbot.python.framework.api.python.NormalizedPythonAnnotation
import org.utbot.python.newtyping.*
import org.utbot.python.utils.TemporaryFileManager
import org.utbot.python.utils.runCommand
import java.io.File

fun readMypyAnnotationStorageAndInitialErrors(
    pythonPath: String,
    sourcePath: String,
    module: String,
    configFile: File,
    fileForTypeImport: String? = null
): Pair<MypyAnnotationStorage, List<MypyReportLine>> {
    val fileForAnnotationStorage = TemporaryFileManager.assignTemporaryFile(tag = "annotations.json")
    val fileForMypyStdout = TemporaryFileManager.assignTemporaryFile(tag = "mypy.out")
    val fileForMypyStderr = TemporaryFileManager.assignTemporaryFile(tag = "mypy.err")
    val fileForMypyExitStatus = TemporaryFileManager.assignTemporaryFile(tag = "mypy.exit")
    val result = runCommand(
        listOf(
            pythonPath,
            "-m",
            "utbot_mypy_runner",
            "--config",
            configFile.absolutePath,
            "--sources",
            sourcePath,
            "--modules",
            module,
            "--annotations_out",
            fileForAnnotationStorage.absolutePath,
            "--mypy_stdout",
            fileForMypyStdout.absolutePath,
            "--mypy_stderr",
            fileForMypyStderr.absolutePath,
            "--mypy_exit_status",
            fileForMypyExitStatus.absolutePath
        ) + if (fileForTypeImport != null) listOf("--file_for_types", fileForTypeImport) else emptyList()
    )
    val stderr = if (fileForMypyStderr.exists()) fileForMypyStderr.readText() else null
    val mypyExitStatus = if (fileForMypyExitStatus.exists()) fileForMypyExitStatus.readText() else null
    if (result.exitValue != 0 || mypyExitStatus != "0")
        error("Something went wrong in initial mypy run. " +
                "\nPython stderr ${result.stderr}" +
                "\nMypy stderr: $stderr")
    return Pair(
        readMypyAnnotationStorage(fileForAnnotationStorage.readText()),
        getErrorsAndNotes(fileForMypyStdout.readText())
    )
}

fun checkWithDMypy(pythonPath: String, fileWithCodePath: String, configFile: File): String {
    val result = runCommand(
        listOf(
            pythonPath,
            "-m",
            "mypy.dmypy",
            "run",
            "--",
            fileWithCodePath,
            "--config-file",
            configFile.path
        )
    )
    return result.stdout
}

fun setConfigFile(directoriesForSysPath: Set<String>): File {
    val file = TemporaryFileManager.assignTemporaryFile(configFilename)
    val configContent = """
            [mypy]
            mypy_path = ${directoriesForSysPath.joinToString(separator = ":")}
            namespace_packages = True
            explicit_package_bases = True
            show_absolute_path = True
            cache_fine_grained = True
            check_untyped_defs = True
            """.trimIndent()
    TemporaryFileManager.writeToAssignedFile(file, configContent)
    return file
}

fun checkSuggestedSignatureWithDMypy(
    method: PythonMethod,
    directoriesForSysPath: Set<String>,
    moduleToImport: String,
    namesInModule: Collection<String>,
    fileForMypyCode: File,
    pythonPath: String,
    configFile: File,
    initialErrorNumber: Int
): Boolean {
    val description = method.type.pythonDescription() as PythonCallableTypeDescription
    val annotationMap =
        (description.argumentNames zip method.type.arguments.map { it.pythonTypeRepresentation() }).associate {
            Pair(it.first, NormalizedPythonAnnotation(it.second))
        }
    val mypyCode = generateMypyCheckCode(method, annotationMap, directoriesForSysPath, moduleToImport, namesInModule)
    TemporaryFileManager.writeToAssignedFile(fileForMypyCode, mypyCode)
    val mypyOutput = checkWithDMypy(pythonPath, fileForMypyCode.canonicalPath, configFile)
    val report = getErrorsAndNotes(mypyOutput)
    val errorNumber = getErrorNumber(report, fileForMypyCode.canonicalPath, 0, mypyCode.length)
    return errorNumber <= initialErrorNumber
}

private const val configFilename = "config.ini"

data class MypyReportLine(
    val line: Int,
    val type: String,
    val message: String,
    val file: String
)

fun getErrorNumber(mypyReport: List<MypyReportLine>, filename: String, startLine: Int, endLine: Int) =
    mypyReport.count { it.type == "error" && it.file == filename && it.line >= startLine && it.line <= endLine }

private fun getErrorsAndNotes(mypyOutput: String): List<MypyReportLine> {
    val regex = Regex("(?m)^([^\n]*):([0-9]*): (error|note): ([^\n]*)\n")
    return regex.findAll(mypyOutput).toList().map { match ->
        val file = match.groupValues[1]
        MypyReportLine(
            match.groupValues[2].toInt(),
            match.groupValues[3],
            match.groupValues[4],
            file
        )
    }
}