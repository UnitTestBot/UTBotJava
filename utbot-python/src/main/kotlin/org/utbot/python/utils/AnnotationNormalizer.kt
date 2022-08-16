package org.utbot.python.utils

import org.utbot.framework.plugin.api.NormalizedPythonAnnotation
import org.utbot.framework.plugin.api.PythonClassId
import org.utbot.framework.plugin.api.pythonAnyClassId
import org.utbot.python.Cleaner
import java.io.File


object AnnotationNormalizer {
    private fun getFileWithScript(resourceName: String): File {
        val scriptContent = AnnotationNormalizer::class.java.getResource(resourceName)
            ?.readText()
            ?: error("Didn't find $resourceName")

        return FileManager.createTemporaryFile(scriptContent, tag = "normalize_annotation.py")
    }

    // TODO: remove copy-paste
    private var normalizeAnnotationFromProjectScript_: File? = null
    private val normalizeAnnotationFromProjectScript: File
        get() {
            val result = normalizeAnnotationFromProjectScript_
            if (result == null || !result.exists()) {
                val result1 = getFileWithScript("/normalize_annotation_from_project.py")
                normalizeAnnotationFromProjectScript_ = result1
                return result1
            }
            return result
        }

    private var normalizeAnnotationFromStubScript_: File? = null
    private val normalizeAnnotationFromStubScript: File
        get() {
            val result = normalizeAnnotationFromStubScript_
            if (result == null || !result.exists()) {
                val result1 = getFileWithScript("/normalize_annotation_from_stub.py")
                normalizeAnnotationFromStubScript_ = result1
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

    private val stubAnnotationCache: MutableMap<String, NormalizedPythonAnnotation> = mutableMapOf()

    fun annotationFromStubToClassId(
        annotation: String,
        pythonPath: String,
        moduleOfAnnotation: String
    ): NormalizedPythonAnnotation {
        val cached = stubAnnotationCache[annotation]
        if (cached != null)
            return cached

        val result = runCommand(listOf(
            pythonPath,
            normalizeAnnotationFromStubScript.path,
            annotation,
            moduleOfAnnotation
        ))

        val ret = NormalizedPythonAnnotation(
            substituteTypes(
                if (result.exitValue == 0) result.stdout else annotation
            )
        )
        stubAnnotationCache[annotation] = ret
        return ret
    }

    val substitutionMapFirstStage = listOf(
        "builtins.list" to "typing.List",
        "builtins.dict" to "typing.Dict",
        "builtins.set" to "typing.Set"
    )

    val substitutionMapSecondStage = listOf(
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