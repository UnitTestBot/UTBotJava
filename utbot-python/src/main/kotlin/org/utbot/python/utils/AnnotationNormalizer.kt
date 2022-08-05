package org.utbot.python.utils

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.pythonAnyClassId
import java.io.File


object AnnotationNormalizer {
    private fun getFileWithScript(resourceName: String): File {
        val scriptContent = AnnotationNormalizer::class.java.getResource(resourceName)
            ?.readText()
            ?: error("Didn't find $resourceName")

        return FileManager.createTemporaryFile(scriptContent, tag = "normalize_annotation")
    }

    private fun normalizeAnnotationFromProject(
        annotation: String,
        pythonPath: String,
        projectRoot: String,
        fileOfAnnotation: String,
        filesToAddToSysPath: List<String>
    ): String {
        val scriptFile = getFileWithScript("/normalize_annotation_from_project.py")
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
        return if (result.exitValue == 0) result.stdout else annotation
    }

    fun annotationFromProjectToClassId(
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
                normalizeAnnotationFromProject(
                    annotation,
                    pythonPath,
                    projectRoot,
                    fileOfAnnotation,
                    filesToAddToSysPath
                )
            )

    fun annotationFromStubToClassId(
        annotation: String,
        pythonPath: String,
        moduleOfAnnotation: String
    ): ClassId {
        val scriptFile = getFileWithScript("/normalize_annotation_from_stub.py")
        val result = runCommand(listOf(
            pythonPath,
            scriptFile.path,
            annotation,
            moduleOfAnnotation
        ))
        scriptFile.delete()
        return ClassId(
            if (result.exitValue == 0) result.stdout else annotation
        )
    }
}