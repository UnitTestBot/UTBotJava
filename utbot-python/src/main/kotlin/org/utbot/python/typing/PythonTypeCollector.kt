package org.utbot.python.typing

import com.beust.klaxon.Klaxon
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.io.filefilter.DirectoryFileFilter
import org.apache.commons.io.filefilter.FileFilterUtils
import org.apache.commons.io.filefilter.NameFileFilter
import org.apache.commons.io.filefilter.RegexFileFilter
import org.utbot.common.PathUtil.toPath
import org.utbot.framework.plugin.api.NormalizedPythonAnnotation
import org.utbot.framework.plugin.api.PythonClassId
import org.utbot.python.code.ClassInfoCollector
import org.utbot.python.code.PythonClass
import org.utbot.python.code.PythonCode
import org.utbot.python.utils.AnnotationNormalizer
import org.utbot.python.utils.AnnotationNormalizer.annotationFromProjectToClassId
import org.utbot.python.utils.AnnotationNormalizer.annotationFromStubToClassId
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets

class PythonClassIdInfo(
    val pythonClassId: PythonClassId,
    val initSignature: List<NormalizedPythonAnnotation>?,
    val preprocessedInstances: List<String>?,
    val methods: Set<String>,
    val fields: Set<String>
)

fun moduleOfType(typeName: String): String? {
    val lastIndex = typeName.lastIndexOf('.')
    return if (lastIndex == -1) null else typeName.substring(0, lastIndex)
}

object PythonTypesStorage {
    private var projectClasses: List<ProjectClass> = emptyList()
    var pythonPath: String? = null
    private const val PYTHON_NOT_SPECIFIED = "PythonPath in PythonTypeCollector not specified"

    private fun mapToClassId(typesFromStubs: Collection<StubFileFinder.SearchResult>): List<NormalizedPythonAnnotation> =
        typesFromStubs.map {
            annotationFromStubToClassId(it.typeName, pythonPath ?: error(PYTHON_NOT_SPECIFIED), it.module)
        }

    fun findTypeWithMethod(
        methodName: String
    ): Set<NormalizedPythonAnnotation> {
        val fromStubs = mapToClassId(StubFileFinder.findTypeWithMethod(methodName))
        val fromProject = projectClasses.mapNotNull {
            if (it.info.methods.contains(methodName))
                AnnotationNormalizer.pythonClassIdToNormalizedAnnotation(it.name)
            else
                null
        }
        return (fromStubs union fromProject).toSet()
    }

    fun findTypeWithField(
        fieldName: String
    ): Set<NormalizedPythonAnnotation> {
        val fromStubs = mapToClassId(StubFileFinder.findTypeWithField(fieldName))
        val fromProject = projectClasses.mapNotNull {
            if (it.info.fields.contains(fieldName))
                AnnotationNormalizer.pythonClassIdToNormalizedAnnotation(it.name)
            else
                null
        }
        return (fromStubs union fromProject).toSet()
    }

    fun findTypeByFunctionWithArgumentPosition(
        functionName: String,
        argumentName: String? = null,
        argumentPosition: Int? = null,
    ): Set<NormalizedPythonAnnotation> =
        mapToClassId(
            StubFileFinder.findAnnotationByFunctionWithArgumentPosition(functionName, argumentName, argumentPosition)
        ).toSet()

    fun findTypeByFunctionReturnValue(functionName: String): Set<NormalizedPythonAnnotation> =
        mapToClassId(StubFileFinder.findAnnotationByFunctionReturnValue(functionName)).toSet()

    fun isClassFromProject(typeName: NormalizedPythonAnnotation): Boolean {
        return projectClasses.any { it.name.name == typeName.name }
    }

    fun findPythonClassIdInfoByName(classIdName: String): PythonClassIdInfo? {
        val fromStub = StubFileFinder.nameToClassMap[classIdName]
        val result =
            if (fromStub != null) {
                val fromPreprocessed = TypesFromJSONStorage.typeNameMap[classIdName]
                val classId = PythonClassId(fromStub.className)
                return PythonClassIdInfo(
                    classId,
                    fromStub.methods.find { it.name == "__init__" }
                        ?.args
                        ?.drop(1) // drop 'self' parameter
                        ?.map { annotationFromStubToClassId(
                            it.annotation,
                            pythonPath ?: error(PYTHON_NOT_SPECIFIED),
                            classId.moduleName
                        ) },
                    fromPreprocessed?.instances,
                    fromStub.methods.map { it.name }.toSet(),
                    fromStub.fields.map { it.name }.toSet()
                )
            } else {
                projectClasses.find { it.name.name == classIdName } ?.let { projectClass ->
                    PythonClassIdInfo(
                        projectClass.name,
                        projectClass.initAnnotation,
                        null,
                        projectClass.info.methods,
                        projectClass.info.fields
                    )
                }
            }

        return result
    }

    val builtinTypes: List<String>
        get() = TypesFromJSONStorage.preprocessedTypes.mapNotNull {
            if (it.name.startsWith("builtins.")) it.name.removePrefix("builtins.") else null
        }

    private data class ProjectClass(
        val pythonClass: PythonClass,
        val info: ClassInfoCollector.Storage,
        val initAnnotation: List<NormalizedPythonAnnotation>?,
        val name: PythonClassId
    )

    private fun getPythonFiles(dirPath: String): Collection<File> =
        FileUtils.listFiles(
            File(dirPath),
            /* fileFilter = */ FileFilterUtils.and(
                FileFilterUtils.suffixFileFilter(".py"),
                FileFilterUtils.notFileFilter(
                    FileFilterUtils.prefixFileFilter("test")
                )
            ),
            /* dirFilter = */ FileFilterUtils.notFileFilter(
                NameFileFilter("test")
            )
        )

    private fun getModuleName(path: String, fileWithClass: File): String =
        File(path).toURI().relativize(fileWithClass.toURI()).path.removeSuffix(".py").toPath().joinToString(".")

    fun refreshProjectClassesList(
        directoriesForSysPath: List<String>
    ) {
        val processedFiles = mutableSetOf<File>()
        projectClasses = directoriesForSysPath.flatMap { path ->
            getPythonFiles(path).flatMap inner@{ file ->
                if (processedFiles.contains(file))
                    return@inner emptyList()
                processedFiles.add(file)
                val content = IOUtils.toString(FileInputStream(file), StandardCharsets.UTF_8)
                val code = PythonCode.getFromString(content, file.path)
                code.getToplevelClasses().map { pyClass ->
                    val collector = ClassInfoCollector(pyClass)
                    val module = getModuleName(path, file)
                    val initSignature = pyClass.initSignature
                        ?.map {
                            annotationFromProjectToClassId(
                                it.annotation,
                                pythonPath ?: error(PYTHON_NOT_SPECIFIED),
                                module,
                                pyClass.filename!!,
                                directoriesForSysPath
                            )
                        }
                    val fullClassName = module + "." + pyClass.name
                    ProjectClass(pyClass, collector.storage, initSignature, PythonClassId(fullClassName))
                }
            }
        }
    }

    private data class PreprocessedValueFromJSON(
        val name: String,
        val instances: List<String>
    )

    private object TypesFromJSONStorage {
        val preprocessedTypes: List<PreprocessedValueFromJSON>
        init {
            val typesAsString = PythonTypesStorage::class.java.getResource("/preprocessed_values.json")
                ?.readText(Charsets.UTF_8)
                ?: error("Didn't find preprocessed_values.json")
            preprocessedTypes =  Klaxon().parseArray(typesAsString) ?: emptyList()
        }

        val typeNameMap: Map<String, PreprocessedValueFromJSON> by lazy {
            val result = mutableMapOf<String, PreprocessedValueFromJSON>()
            preprocessedTypes.forEach { type ->
                result[type.name] = type
            }
            result
        }
    }
}