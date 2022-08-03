package org.utbot.python.utils

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.pythonAnyClassId
import org.utbot.python.typing.PythonTypesStorage


fun normalizeAnnotation(
    annotation: String,
    pythonPath: String,
    projectRoot: String,
    fileOfAnnotation: String,
    filesToAddToSysPath: List<String>
): String {

    val scriptContent = PythonTypesStorage::class.java.getResource("/normalize_annotation.py")
        ?.readText()
        ?: error("Didn't find normalize_annotation.py")

    val scriptFile = FileManager.createTemporaryFile(scriptContent, tag = "normalize_annotation")
    val result = runCommand(
        listOf(
            pythonPath,
            scriptFile.path,
            annotation,
            projectRoot,
            fileOfAnnotation,
        ) + filesToAddToSysPath,
    )
    scriptFile.delete()

    return if (result.exitValue == 0)
        result.stdout
    else
        annotation
}

fun annotationToClassId(
    annotation: String?,
    pythonPath: String,
    projectRoot: String,
    fileOfAnnotation: String,
    filesToAddToSysPath: List<String>
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
            )
        )