package org.utbot.python.typing

import org.utbot.framework.plugin.api.ClassId
import org.utbot.python.PythonMethod
import org.utbot.python.code.PythonCodeGenerator.generateMypyCheckCode
import java.io.File


object MypyAnnotations {
    fun mypyCheckAnnotations(
        method: PythonMethod,
        functionArgAnnotations: Map<String, List<String>>,
        testSourcePath: String,
        moduleToImport: String,
        directoriesForSysPath: List<String>,
        pythonPath: String
    ) = sequence {
        val codeFilename = "${testSourcePath}/__${method.name}__mypy_types.py"

        generateMypyCheckCode(
            method,
            emptyMap(),
            codeFilename,
            directoriesForSysPath,
            moduleToImport.split(".").last(),
        )
        startMypyDaemon(pythonPath, testSourcePath, directoriesForSysPath)
        val defaultOutput = runMypy(pythonPath, codeFilename, testSourcePath)
        val defaultErrorNum = getErrorNumber(defaultOutput)

        val candidates = functionArgAnnotations.entries.map { (key, value) ->
            value.map {
                Pair(key, it)
            }
        }

        if (candidates.any { it.isEmpty() })
            return@sequence

        val annotations = PriorityCartesianProduct(candidates).asSequence()

        annotations.forEach {
            val annotationMap = it.toMap()
            val functionFile = generateMypyCheckCode(
                method,
                annotationMap,
                codeFilename,
                directoriesForSysPath,
                moduleToImport.split(".").last(),
            )
            val mypyOutput = runMypy(pythonPath, codeFilename, testSourcePath)
            val errorNum = getErrorNumber(mypyOutput)
            if (errorNum <= defaultErrorNum) {
                yield(annotationMap.mapValues { entry ->
                    ClassId(entry.value)
                })
            }
            functionFile.deleteOnExit()
        }
    }

    fun getConfigFile(testSourcePath: String): File = File(testSourcePath, "mypy.ini")

    private fun runMypy(
        pythonPath: String,
        codeFilename: String,
        testSourcePath: String,
    ): String {
        val command = "$pythonPath -m mypy.dmypy run $codeFilename -- --config-file ${getConfigFile(testSourcePath).path}"
        val process = Runtime.getRuntime().exec(
            command
        )
        process.waitFor()
        return process.inputStream.readBytes().decodeToString()
    }

    private fun startMypyDaemon(
        pythonPath: String,
        testSourcePath: String,
        directoriesForSysPath: List<String>
    ): String {

        val configContent = "[mypy]\nmypy_path = ${directoriesForSysPath.joinToString(separator = ":")}"
        val configFile = getConfigFile(testSourcePath)
        configFile.writeText(configContent)
        configFile.createNewFile()

        val command = "$pythonPath -m mypy.dmypy start"
        val process = Runtime.getRuntime().exec(
            command
        )
        process.waitFor()
        return process.inputStream.readBytes().decodeToString()
    }

    private fun getErrorNumber(mypyOutput: String): Int {
        val regex = Regex("Found ([0-9]*) error")
        val match = regex.find(mypyOutput)
        return match?.groupValues?.getOrNull(1)?.toInt() ?: 0
    }
}

