package org.utbot.python

import org.utbot.framework.plugin.api.PythonClassId
import org.utbot.framework.plugin.api.pythonAnyClassId
import org.utbot.python.code.ArgInfoCollector
import org.utbot.python.typing.AnnotationFinder.findAnnotations
import org.utbot.python.typing.MypyAnnotations
import org.utbot.python.utils.AnnotationNormalizer.annotationFromProjectToClassId

object PythonTestCaseGenerator {
    private lateinit var directoriesForSysPath: List<String>
    private lateinit var moduleToImport: String
    private lateinit var pythonPath: String
    private lateinit var projectRoot: String
    private lateinit var fileOfMethod: String
    private lateinit var isCancelled: () -> Boolean

    private const val maxTestCount = 30

    fun init(
        directoriesForSysPath: List<String>,
        moduleToImport: String,
        pythonPath: String,
        projectRoot: String,
        fileOfMethod: String,
        isCancelled: () -> Boolean
    ) {
        this.directoriesForSysPath = directoriesForSysPath
        this.moduleToImport = moduleToImport
        this.pythonPath = pythonPath
        this.projectRoot = projectRoot
        this.fileOfMethod = fileOfMethod
        this.isCancelled = isCancelled
    }

    private val storageForMypyMessages: MutableList<MypyAnnotations.MypyReportLine> = mutableListOf()

    fun generate(method: PythonMethod): PythonTestSet {
        storageForMypyMessages.clear()

        val initialArgumentTypes = method.arguments.map {
            annotationFromProjectToClassId(
                it.annotation,
                pythonPath,
                projectRoot,
                fileOfMethod,
                directoriesForSysPath
            )
        }

        val argInfoCollector = ArgInfoCollector(method, initialArgumentTypes)
        val annotationSequence = getAnnotations(method, initialArgumentTypes, argInfoCollector, isCancelled)

        val executions = mutableListOf<PythonExecution>()
        val errors = mutableListOf<PythonError>()

        var testsGenerated = 0

        run breaking@ {
            annotationSequence.forEach { annotations ->
                if (isCancelled())
                    return@breaking

                val engine = PythonEngine(
                    method,
                    directoriesForSysPath,
                    moduleToImport,
                    pythonPath,
                    argInfoCollector.getConstants(),
                    annotations
                )

                engine.fuzzing().forEach {
                    if (isCancelled())
                        return@breaking
                    when (it) {
                        is PythonExecution -> executions += it
                        is PythonError -> errors += it
                    }
                    testsGenerated += 1
                    if (testsGenerated >= maxTestCount)
                        return@breaking
                }

                if (testsGenerated >= maxTestCount)
                    return@breaking
            }
        }

        return PythonTestSet(method, executions, errors, storageForMypyMessages)
    }

    fun getAnnotations(
        method: PythonMethod,
        initialArgumentTypes: List<PythonClassId>,
        argInfoCollector: ArgInfoCollector,
        isCancelled: () -> Boolean
    ): Sequence<Map<String, PythonClassId>> {

        val existingAnnotations = mutableMapOf<String, String>()
        initialArgumentTypes.forEachIndexed { index, classId ->
            if (classId != pythonAnyClassId)
                existingAnnotations[method.arguments[index].name] = classId.name
        }

        return if (existingAnnotations.size == method.arguments.size)
            sequenceOf(
                existingAnnotations.mapValues { entry -> PythonClassId(entry.value) }
            )
        else
            findAnnotations(
                argInfoCollector,
                method,
                existingAnnotations,
                moduleToImport,
                directoriesForSysPath,
                pythonPath,
                fileOfMethod,
                isCancelled,
                storageForMypyMessages
            )
    }
}