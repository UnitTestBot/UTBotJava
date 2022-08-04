package org.utbot.python.typing

import com.beust.klaxon.Klaxon
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.io.filefilter.DirectoryFileFilter
import org.apache.commons.io.filefilter.RegexFileFilter
import org.utbot.framework.plugin.api.ClassId
import org.utbot.python.utils.annotationToClassId
import org.utbot.python.code.ClassInfoCollector
import org.utbot.python.code.PythonClass
import org.utbot.python.code.PythonCode
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets

class PythonType(
    val name: String, // include module name (like 'ast.Assign')
    val initSignature: List<ClassId>?,
    val sourceFile: String?,
    val preprocessedInstances: List<String>?,
    val methods: List<String>,
    val fields: List<String>,
    val returnRenderType: ReturnRenderType = ReturnRenderType.REPR
) {
    val module: String?
        get() {
            val lastIndex = name.lastIndexOf('.')
            return if (lastIndex == -1) null else name.substring(0, lastIndex)
        }
}

enum class ReturnRenderType {
    REPR, PICKLE, NONE
}

object PythonTypesStorage {
    private var projectClasses: List<ProjectClass> = emptyList()

    fun findTypeWithMethod(
        methodName: String
    ): Set<String> {
        val fromStubs = StubFileFinder.findTypeWithMethod(methodName)
        val fromProject = projectClasses.mapNotNull {
            if (it.info.methods.contains(methodName)) it.pythonClass.name else null
        }
        return fromStubs union fromProject.toSet()
    }

    fun findTypeWithField(
        fieldName: String
    ): Set<String> {
        val fromStubs = StubFileFinder.findTypeWithField(fieldName)
        val fromProject = projectClasses.mapNotNull {
            if (it.info.fields.contains(fieldName)) it.pythonClass.name else null
        }
        return fromStubs union fromProject.toSet()
    }

    fun isFromProject(typeName: String): Boolean {
        return projectClasses.any { it.pythonClass.name == typeName }
    }

    fun getTypeByName(classId: ClassId): PythonType? {
        val fromStub = StubFileFinder.nameToClassMap[classId.name]
        if (fromStub != null) {
            val fromPreprocessed = TypesFromJSONStorage.typeNameMap[classId.name]
            return PythonType(
                classId.name,
                fromStub.methods.find { it.name == "__init__" }
                    ?.args
                    ?.drop(1) // drop 'self' parameter
                    ?.map { ClassId(it.annotation) },
                null,
                fromPreprocessed?.instances,
                fromStub.methods.map { it.name },
                fromStub.fields.map { it.name },
                if (fromPreprocessed?.useAsReturn == false) ReturnRenderType.NONE else ReturnRenderType.REPR
            )
        }

        return projectClasses.find { it.pythonClass.name == classId.name } ?.let { projectClass ->
            PythonType(
                classId.name,
                projectClass.initAnnotation,
                projectClass.pythonClass.filename,
                null,
                projectClass.info.methods,
                projectClass.info.fields
            )
        }
    }

    val builtinTypes: List<String>
        get() = TypesFromJSONStorage.preprocessedTypes.mapNotNull {
            if (it.name.startsWith("builtins.")) it.name.removePrefix("builtins.") else null
        }

    private data class ProjectClass(
        val pythonClass: PythonClass,
        val info: ClassInfoCollector.Storage,
        val initAnnotation: List<ClassId>?
    )

    private fun getPythonFiles(dirPath: String): Collection<File> =
        FileUtils.listFiles(
            File(dirPath),
            RegexFileFilter("^.*[.]py"),
            DirectoryFileFilter.DIRECTORY
        )

    fun refreshProjectClassesList(
        path: String,
        pythonPath: String,
        projectRoot: String,
        directoriesForSysPath: List<String>
    ) {
        val pythonFiles = if (path.endsWith(".py")) listOf(File(path)) else getPythonFiles(path)
        projectClasses = pythonFiles.flatMap { file ->
            val content = IOUtils.toString(FileInputStream(file), StandardCharsets.UTF_8)
            val code = PythonCode.getFromString(content, file.path)
            code.getToplevelClasses().map { pyClass ->
                val collector = ClassInfoCollector(pyClass)
                val initSignature = pyClass.initFunction
                    ?.arguments
                    ?.drop(1) // drop 'self' parameter
                    ?.map {
                        annotationToClassId(
                            it.annotation,
                            pythonPath,
                            projectRoot,
                            pyClass.filename!!,
                            directoriesForSysPath
                        )
                    }
                ProjectClass(pyClass, collector.storage, initSignature)
            }
        }
    }

    private data class PreprocessedValueFromJSON(
        val name: String,
        val instances: List<String>,
        val useAsReturn: Boolean
    )

    private object TypesFromJSONStorage {
        val preprocessedTypes: List<PreprocessedValueFromJSON>
        init {
            val typesAsString = PythonTypesStorage::class.java.getResource("/preprocessed_values.json")?.readText(Charsets.UTF_8)
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