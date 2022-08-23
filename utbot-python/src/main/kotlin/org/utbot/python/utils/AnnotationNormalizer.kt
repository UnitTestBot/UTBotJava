package org.utbot.python.utils

import org.utbot.framework.plugin.api.NormalizedPythonAnnotation
import org.utbot.framework.plugin.api.PythonClassId
import org.utbot.framework.plugin.api.pythonAnyClassId
import java.io.File


object AnnotationNormalizer {
    private val scriptContent = AnnotationNormalizer::class.java
        .getResource("/normalize_annotation_from_project.py")
        ?.readText()
        ?: error("Didn't find /normalize_annotation_from_project.py")

    private var normalizeAnnotationFromProjectScript_: File? = null
    private val normalizeAnnotationFromProjectScript: File
        get() {
            val result = normalizeAnnotationFromProjectScript_
            if (result == null || !result.exists()) {
                val result1 = FileManager.createTemporaryFile(scriptContent, tag = "normalize_annotation.py")
                normalizeAnnotationFromProjectScript_ = result1
                return result1
            }
            return result
        }

    private fun normalizeAnnotationFromProject(
        annotation: String,
        pythonPath: String,
        curPythonModule: String,
        fileOfAnnotation: String,
        filesToAddToSysPath: Set<String>
    ): String {
        val result = runCommand(
            listOf(
                pythonPath,
                normalizeAnnotationFromProjectScript.path,
                annotation,
                curPythonModule,
                fileOfAnnotation,
            ) + filesToAddToSysPath,
        )
        return if (result.exitValue == 0) result.stdout else annotation
    }

    fun annotationFromProjectToClassId(
        annotation: String?,
        pythonPath: String,
        curPythonModule: String,
        fileOfAnnotation: String,
        filesToAddToSysPath: Set<String>
    ): NormalizedPythonAnnotation =
        if (annotation == null)
            pythonAnyClassId
        else
            NormalizedPythonAnnotation(
                substituteTypes(
                    normalizeAnnotationFromProject(
                        annotation,
                        pythonPath,
                        curPythonModule,
                        fileOfAnnotation,
                        filesToAddToSysPath
                    )
                )
            )

    private val substitutionMapFirstStage = listOf(
        "builtins.list" to "typing.List",
        "builtins.dict" to "typing.Dict",
        "builtins.set" to "typing.Set"
    )

    private val substitutionMapSecondStage = listOf(
        Regex("typing.List *([^\\[]|$)") to "typing.List[typing.Any]",
        Regex("typing.Dict *([^\\[]|$)") to "typing.Dict[typing.Any, typing.Any]",
        Regex("typing.Set *([^\\[]|$)") to "typing.Set[typing.Any]"
    )

    private fun substituteTypes(annotation: String): String {
        val firstStage = substitutionMapFirstStage.fold(annotation) { acc, (old, new) ->
            acc.replace(old, new)
        }
        return substitutionMapSecondStage.fold(firstStage) { acc, (re, new) ->
            acc.replace(re, new)
        }
    }

    fun pythonClassIdToNormalizedAnnotation(classId: PythonClassId): NormalizedPythonAnnotation {
        return NormalizedPythonAnnotation(substituteTypes(classId.name))
    }
}