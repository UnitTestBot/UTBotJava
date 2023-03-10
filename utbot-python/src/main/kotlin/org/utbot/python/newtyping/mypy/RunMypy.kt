package org.utbot.python.newtyping.mypy

import mu.KotlinLogging
import org.utbot.python.PythonMethod
import org.utbot.python.code.PythonCodeGenerator.generateMypyCheckCode
import org.utbot.python.utils.TemporaryFileManager
import org.utbot.python.utils.runCommand
import java.io.File

private val logger = KotlinLogging.logger {}

fun readMypyAnnotationStorageAndInitialErrors(
    pythonPath: String,
    sourcePath: String,
    module: String,
    configFile: File
): Pair<MypyAnnotationStorage, List<MypyReportLine>> {
    val fileForAnnotationStorage = TemporaryFileManager.assignTemporaryFile(tag = "annotations.json")
    val fileForMypyStdout = TemporaryFileManager.assignTemporaryFile(tag = "mypy.out")
    val fileForMypyStderr = TemporaryFileManager.assignTemporaryFile(tag = "mypy.err")
    val fileForMypyExitStatus = TemporaryFileManager.assignTemporaryFile(tag = "mypy.exit")
    val result = runCommand(
        listOf(
            pythonPath,
            "-X",
            "utf8",
            "-m",
            "utbot_mypy_runner",
            "--config",
            configFile.absolutePath,
            "--sources",
            sourcePath.modifyWindowsPath(),
            "--modules",
            module,
            "--annotations_out",
            fileForAnnotationStorage.absolutePath,
            "--mypy_stdout",
            fileForMypyStdout.absolutePath,
            "--mypy_stderr",
            fileForMypyStderr.absolutePath,
            "--mypy_exit_status",
            fileForMypyExitStatus.absolutePath,
            "--module_for_types",
            module
        )
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
    val dirForCache = TemporaryFileManager.assignTemporaryFile(tag = "mypy_cache")
    val configContent = """
            [mypy]
            mypy_path = ${directoriesForSysPath.joinToString(separator = ":") { it.modifyWindowsPath() } }
            namespace_packages = True
            cache_dir = ${dirForCache.absolutePath}
            show_absolute_path = True
            cache_fine_grained = True
            check_untyped_defs = True
            disable_error_code = assignment,union-attr
            implicit_optional = True
            strict_optional = False
            allow_redefinition = True
            local_partial_types = True
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
    initialErrorNumber: Int,
    additionalVars: String
): Boolean {
    val annotationMap =
        (method.definition.meta.args.map { it.name } zip method.definition.type.arguments).associate {
            Pair(it.first, it.second)
        }
    val mypyCode = generateMypyCheckCode(method, annotationMap, directoriesForSysPath, moduleToImport, namesInModule, additionalVars)
    // logger.debug(mypyCode)
    TemporaryFileManager.writeToAssignedFile(fileForMypyCode, mypyCode)
    val mypyOutput = checkWithDMypy(pythonPath, fileForMypyCode.canonicalPath, configFile)
    val report = getErrorsAndNotes(mypyOutput)
    val errorNumber = getErrorNumber(report, fileForMypyCode.canonicalPath, 0, mypyCode.length)
    if (errorNumber > initialErrorNumber)
        logger.debug(mypyOutput)
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
    // logger.debug(mypyOutput)
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