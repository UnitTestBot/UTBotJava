package org.utbot.python.newtyping.mypy

import mu.KotlinLogging
import org.utbot.python.PythonMethod
import org.utbot.python.code.PythonCodeGenerator.generateMypyCheckCode
import org.utbot.python.utils.TemporaryFileManager
import org.utbot.python.utils.runCommand
import java.io.File

private val logger = KotlinLogging.logger {}

fun checkWithDMypy(pythonPath: String, fileWithCodePath: String, configFile: File, timeout: Long? = null): String? {
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
        ),
        timeout = timeout
    )
    if (result.terminatedByTimeout)
        return null
    return result.stdout
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
    additionalVars: String,
    timeout: Long? = null
): Boolean {
    val annotationMap =
        (method.argumentsNames zip method.methodType.arguments).associate {
            Pair(it.first, it.second)
        }
    val mypyCode = generateMypyCheckCode(method, annotationMap, directoriesForSysPath, moduleToImport, namesInModule, additionalVars)
    // logger.debug(mypyCode)
    TemporaryFileManager.writeToAssignedFile(fileForMypyCode, mypyCode)
    val mypyOutput = checkWithDMypy(pythonPath, fileForMypyCode.canonicalPath, configFile, timeout = timeout)
        ?: return true
    val report = getErrorsAndNotes(mypyOutput)
    val errorNumber = getErrorNumber(report, fileForMypyCode.canonicalPath, 0, mypyCode.length)
    if (errorNumber > initialErrorNumber)
        logger.debug(mypyOutput)
    return errorNumber <= initialErrorNumber
}
