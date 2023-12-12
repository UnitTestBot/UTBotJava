package org.utbot.python.newtyping.mypy

import org.utbot.python.utils.runCommand
import java.io.File

private const val configFilename = "config.ini"
private const val mypyCacheDirectoryName = ".mypy_cache"
private const val annotationsFilename = "annotations.json"
private const val mypyStdoutFilename = "mypy.out"
private const val mypyStderrFilename = "mypy.err"
private const val mypyExitStatusFilename = "mypy.exit"

class MypyBuildDirectory(
    val root: File,
    val directoriesForSysPath: Set<String>
) {
    val configFile = File(root, configFilename)
    val fileForAnnotationStorage = File(root, annotationsFilename)
    val fileForMypyStdout = File(root, mypyStdoutFilename)
    val fileForMypyStderr = File(root, mypyStderrFilename)
    val fileForMypyExitStatus = File(root, mypyExitStatusFilename)
    val dirForCache = File(root, mypyCacheDirectoryName)

    init {
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
        writeText(configFile, configContent)
    }
}

fun buildMypyInfo(
    pythonPath: String,
    sourcePaths: List<String>,
    modules: List<String>,
    mypyBuildDir: MypyBuildDirectory,
    moduleForTypes: String? = null,
    indent: Int? = null
) {
    val cmdPrefix = listOf(
        pythonPath,
        "-X",
        "utf8",
        "-m",
        "utbot_mypy_runner",
        "--config",
        mypyBuildDir.configFile.absolutePath,
        "--annotations_out",
        mypyBuildDir.fileForAnnotationStorage.absolutePath,
        "--mypy_stdout",
        mypyBuildDir.fileForMypyStdout.absolutePath,
        "--mypy_stderr",
        mypyBuildDir.fileForMypyStderr.absolutePath,
        "--mypy_exit_status",
        mypyBuildDir.fileForMypyExitStatus.absolutePath,
    )
    val cmdIndent = if (indent != null) listOf("--indent", indent.toString()) else emptyList()
    val cmdSources = listOf("--sources") + sourcePaths.map { it.modifyWindowsPath() }
    val cmdModules = listOf("--modules") + modules
    val cmdModuleForTypes = if (moduleForTypes != null) listOf("--module_for_types", moduleForTypes) else emptyList()
    val cmd = cmdPrefix + cmdIndent + cmdSources + cmdModules + cmdModuleForTypes
    val result = runCommand(cmd)
    val stderr = if (mypyBuildDir.fileForMypyStderr.exists()) mypyBuildDir.fileForMypyStderr.readText() else null
    val stdout = if (mypyBuildDir.fileForMypyStdout.exists()) mypyBuildDir.fileForMypyStdout.readText() else null
    val mypyExitStatus = if (mypyBuildDir.fileForMypyExitStatus.exists()) mypyBuildDir.fileForMypyExitStatus.readText() else null
    if (result.exitValue != 0 || mypyExitStatus != "0")
        error("Something went wrong in initial mypy run. " +
                "\nPython stdout:\n${result.stdout}" +
                "\nPython stderr:\n${result.stderr}" +
                "\nMypy stderr:\n$stderr" +
                "\nMypy stdout:\n$stdout")
}

fun readMypyAnnotationStorageAndInitialErrors(
    pythonPath: String,
    sourcePath: String,
    module: String,
    mypyBuildDir: MypyBuildDirectory
): Pair<MypyInfoBuild, List<MypyReportLine>> {
    buildMypyInfo(pythonPath, listOf(sourcePath), listOf(module), mypyBuildDir, module)
    return Pair(
        readMypyInfoBuild(mypyBuildDir),
        getErrorsAndNotes(mypyBuildDir.fileForMypyStdout.readText())
    )
}

data class MypyReportLine(
    val line: Int,
    val type: String,
    val message: String,
    val file: String
)

fun getErrorNumber(mypyReport: List<MypyReportLine>, filename: String, startLine: Int, endLine: Int) =
    mypyReport.count { it.type == "error" && it.file == filename && it.line >= startLine && it.line <= endLine }

fun getErrorsAndNotes(mypyOutput: String): List<MypyReportLine> {
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

private fun writeText(file: File, content: String) {
    file.parentFile?.mkdirs()
    file.writeText(content)
    file.createNewFile()
}