package org.utbot.python

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.pythonAnyClassId
import org.utbot.python.code.PythonCodeGenerator.saveToFile
import org.utbot.python.typing.PythonTypesStorage
import java.io.BufferedReader
import java.io.InputStreamReader


fun normalizeAnnotation(
    annotation: String,
    pythonPath: String,
    projectRoot: String,
    fileOfAnnotation: String,
    filesToAddToSysPath: List<String>,
    testSourcePath: String
): String {

    val codeFilename = "${testSourcePath}/__annotation_check.py"
    val scriptContent = PythonTypesStorage::class.java.getResource("/normalize_annotation.py")
        ?.readText()
        ?: error("Didn't find normalize_annotation.py")
    saveToFile(codeFilename, scriptContent)

    val process = ProcessBuilder(
        listOf(
            pythonPath,
            codeFilename,
            annotation,
            projectRoot,
            fileOfAnnotation,
        ) + filesToAddToSysPath,
    ).start()
//    val command = "$pythonPath $codeFilename '$annotation' $projectRoot $fileOfAnnotation " +
//            filesToAddToSysPath.joinToString(separator = " ")
//    val process = Runtime.getRuntime().exec(command)
    process.waitFor()
    return process.inputStream.readBytes().decodeToString().trimIndent()
}

fun annotationToClassId(
    annotation: String?,
    pythonPath: String,
    projectRoot: String,
    fileOfAnnotation: String,
    filesToAddToSysPath: List<String>,
    testSourcePath: String
): ClassId =
    if (annotation == null)
        pythonAnyClassId
    else
        ClassId(
            normalizeAnnotation(
                annotation,
                pythonPath,
                projectRoot,
                fileOfAnnotation,
                filesToAddToSysPath,
                testSourcePath
            )
        )