package org.utbot.python.typing

import com.beust.klaxon.Klaxon
import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.io.filefilter.FileFilterUtils
import org.apache.commons.io.filefilter.NameFileFilter
import org.utbot.python.code.ClassInfoCollector
import org.utbot.python.code.PythonClass
import org.utbot.python.code.PythonCode
import org.utbot.python.code.PythonModule
import org.utbot.python.framework.api.python.NormalizedPythonAnnotation
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.utils.AnnotationNormalizer
import org.utbot.python.utils.AnnotationNormalizer.annotationFromProjectToClassId
import org.utbot.python.utils.checkIfFileLiesInPath
import org.utbot.python.utils.getModuleNameWithoutCheck
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets

private val logger = KotlinLogging.logger {}

class PythonClassIdInfo(
    val pythonClassId: PythonClassId,
    val initSignature: List<NormalizedPythonAnnotation>?,
    val preprocessedInstances: List<String>?,
    val methods: Set<String>,
    val fields: Set<String>
)

object PythonTypesStorage {
    private const val PYTHON_NOT_SPECIFIED = "PythonPath in PythonTypeCollector not specified"
    private var projectClasses: List<ProjectClass> = emptyList()
    private var projectModules: List<PythonModule> = emptyList()
    var pythonPath: String? = null

    fun findTypeWithMethod(
        methodName: String
    ): Set<NormalizedPythonAnnotation> {
        val fromStubs = StubFileFinder.findTypeWithMethod(methodName).map {
            AnnotationNormalizer.pythonClassIdToNormalizedAnnotation(it)
        }
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
        val fromStubs = StubFileFinder.findTypeWithField(fieldName).map {
            AnnotationNormalizer.pythonClassIdToNormalizedAnnotation(it)
        }
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
        StubFileFinder.findAnnotationByFunctionWithArgumentPosition(functionName, argumentName, argumentPosition)

    fun findTypeByFunctionReturnValue(functionName: String): Set<NormalizedPythonAnnotation> =
        StubFileFinder.findAnnotationByFunctionReturnValue(functionName).toSet()

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
                        ?.map { NormalizedPythonAnnotation(it.annotation) },
                    fromPreprocessed?.instances,
                    fromStub.methods.map { it.name }.toSet(),
                    fromStub.fields.map { it.name }.toSet()
                )
            } else {
                projectClasses.find { it.name.name == classIdName }?.let { projectClass ->
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

    data class ProjectClass(
        val pythonClass: PythonClass,
        val info: ClassInfoCollector.Storage,
        val initAnnotation: List<NormalizedPythonAnnotation>?,
        val name: PythonClassId
    )

    private fun getPythonFiles(directory: File): Collection<File> =
        FileUtils.listFiles(
            directory,
            /* fileFilter = */ FileFilterUtils.and(
                FileFilterUtils.suffixFileFilter(".py"),
                FileFilterUtils.notFileFilter(
                    FileFilterUtils.prefixFileFilter("test")
                ),
            ),
            /* dirFilter = */ FileFilterUtils.and(
                FileFilterUtils.notFileFilter(
                    NameFileFilter("test")
                ),
                FileFilterUtils.notFileFilter(
                    FileFilterUtils.suffixFileFilter("venv")
                ),
                FileFilterUtils.notFileFilter(
                    FileFilterUtils.prefixFileFilter(".")
                )
            )
        )

    fun refreshProjectClassesAndModulesLists(
        directoriesForSysPath: Set<String>,
        onlyFromSpecifiedFile: File? = null
    ) {
        val projectClassesSet = mutableSetOf<ProjectClass>()
        val projectModulesSet = mutableSetOf(PythonModule("builtins"))

        val filesToVisit = directoriesForSysPath.flatMap { path ->
            if (onlyFromSpecifiedFile != null && !checkIfFileLiesInPath(path, onlyFromSpecifiedFile.path))
                return@flatMap emptyList()

            val pathFile = File(path)
            if (onlyFromSpecifiedFile != null)
                return@flatMap listOf(
                    Pair(getModuleNameWithoutCheck(pathFile, onlyFromSpecifiedFile), onlyFromSpecifiedFile)
                )

            getPythonFiles(pathFile).map { Pair(getModuleNameWithoutCheck(pathFile, it), it) }
        }.distinctBy { it.second }

        filesToVisit.forEach { (module, file) ->
            val content = IOUtils.toString(FileInputStream(file), StandardCharsets.UTF_8)
            val code = PythonCode.getFromString(content, file.path) ?: return@forEach
            projectClassesSet += code.getToplevelClasses().map { pyClass ->
                val collector = ClassInfoCollector(pyClass)
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
            projectModulesSet += code.getToplevelModules()
        }
        projectClasses = projectClassesSet.toList()

        val newModules = projectModulesSet - projectModules.toSet()

        logger.debug("Updating info from stub files")

        updateStubFiles(newModules.map { it.name }.toList())
        projectModules = projectModulesSet.toList()
    }

    private fun updateStubFiles(newModules: List<String>) {
        if (newModules.isNotEmpty()) {
            val jsonData = StubFileReader.getStubInfo(
                newModules,
                pythonPath ?: error(PYTHON_NOT_SPECIFIED),
            )
            StubFileFinder.updateStubs(jsonData)
        }
    }


    data class PreprocessedValueFromJSON(
        val name: String,
        val instances: List<String>
    )

    private object TypesFromJSONStorage {
        val preprocessedTypes: List<PreprocessedValueFromJSON>

        init {
            val typesAsString = PythonTypesStorage::class.java.getResource("/preprocessed_values.json")
                ?.readText(Charsets.UTF_8)
                ?: error("Didn't find preprocessed_values.json")
            preprocessedTypes = Klaxon().parseArray(typesAsString) ?: emptyList()
        }

        val typeNameMap: Map<String, PreprocessedValueFromJSON> by lazy {
            val result = mutableMapOf<String, PreprocessedValueFromJSON>()
            preprocessedTypes.forEach { type ->
                result[type.name] = type
            }
            result
        }
    }

    fun getTypesFromJsonStorage(): Map<String, PreprocessedValueFromJSON> {
        return TypesFromJSONStorage.typeNameMap
    }
}