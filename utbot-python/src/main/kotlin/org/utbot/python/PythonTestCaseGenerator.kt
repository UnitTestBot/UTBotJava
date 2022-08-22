package org.utbot.python

import mu.KotlinLogging
import org.utbot.framework.minimization.minimizeExecutions
import org.utbot.framework.plugin.api.NormalizedPythonAnnotation
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.pythonAnyClassId
import org.utbot.python.code.ArgInfoCollector
import org.utbot.python.typing.AnnotationFinder.findAnnotations
import org.utbot.python.typing.MypyAnnotations
import org.utbot.python.utils.AnnotationNormalizer.annotationFromProjectToClassId

private val logger = KotlinLogging.logger {}

object PythonTestCaseGenerator {
    private var withMinimization: Boolean = true
    private lateinit var directoriesForSysPath: Set<String>
    private lateinit var curModule: String
    private lateinit var pythonPath: String
    private lateinit var fileOfMethod: String
    private lateinit var isCancelled: () -> Boolean
    private var timeoutForRun: Long = 0

    private const val maxTestCount = 30

    fun init(
        directoriesForSysPath: Set<String>,
        moduleToImport: String,
        pythonPath: String,
        fileOfMethod: String,
        timeoutForRun: Long,
        withMinimization: Boolean = true,
        isCancelled: () -> Boolean
    ) {
        this.directoriesForSysPath = directoriesForSysPath
        this.curModule = moduleToImport
        this.pythonPath = pythonPath
        this.fileOfMethod = fileOfMethod
        this.withMinimization = withMinimization
        this.isCancelled = isCancelled
        this.timeoutForRun = timeoutForRun
    }

    private val storageForMypyMessages: MutableList<MypyAnnotations.MypyReportLine> = mutableListOf()

    fun generate(method: PythonMethod): PythonTestSet {
        storageForMypyMessages.clear()

        val initialArgumentTypes = method.arguments.map {
            annotationFromProjectToClassId(
                it.annotation,
                pythonPath,
                curModule,
                fileOfMethod,
                directoriesForSysPath
            )
        }.toMutableList()

        // TODO: consider static and class methods
        if (method.containingPythonClassId != null) {
            initialArgumentTypes[0] = NormalizedPythonAnnotation(method.containingPythonClassId!!.name)
        }

        val argInfoCollector = ArgInfoCollector(method, initialArgumentTypes)
        val annotationSequence = getAnnotations(method, initialArgumentTypes, argInfoCollector, isCancelled)

        val executions = mutableListOf<UtExecution>()
        val errors = mutableListOf<UtError>()

        var testsGenerated = 0

        run breaking@ {
            annotationSequence.forEach { annotations ->
                if (isCancelled())
                    return@breaking

                val engine = PythonEngine(
                    method,
                    directoriesForSysPath,
                    curModule,
                    pythonPath,
                    argInfoCollector.getConstants(),
                    annotations,
                    timeoutForRun
                )

                engine.fuzzing().forEach {
                    if (isCancelled())
                        return@breaking
                    when (it) {
                        is UtExecution -> {
                            logger.debug("Added execution")
                            executions += it
                        }
                        is UtError -> {
                            logger.debug("Failed evaluation")
                            errors += it
                        }
                    }
                    testsGenerated += 1
                    if (testsGenerated >= maxTestCount)
                        return@breaking
                }

                if (testsGenerated >= maxTestCount)
                    return@breaking
            }
        }

        return PythonTestSet(
            method,
            if (withMinimization) minimizeExecutions(executions) else executions,
            errors,
            storageForMypyMessages
        )
    }

    private fun getAnnotations(
        method: PythonMethod,
        initialArgumentTypes: List<NormalizedPythonAnnotation>,
        argInfoCollector: ArgInfoCollector,
        isCancelled: () -> Boolean
    ): Sequence<Map<String, NormalizedPythonAnnotation>> {

        val existingAnnotations = mutableMapOf<String, NormalizedPythonAnnotation>()
        initialArgumentTypes.forEachIndexed { index, classId ->
            if (classId != pythonAnyClassId)
                existingAnnotations[method.arguments[index].name] = classId
        }

        return findAnnotations(
            argInfoCollector,
            method,
            existingAnnotations,
            curModule,
            directoriesForSysPath,
            pythonPath,
            fileOfMethod,
            isCancelled,
            storageForMypyMessages
        )
    }
}