package org.utbot.python

import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.codegen.model.CodeGenerator
import org.utbot.framework.codegen.model.constructor.CgMethodTestSet
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.python.typing.MypyAnnotations
import org.utbot.python.typing.PythonTypesStorage
import org.utbot.python.typing.StubFileFinder
import org.utbot.python.utils.Cleaner
import org.utbot.python.utils.FileManager
import org.utbot.python.utils.RequirementsUtils.requirementsAreInstalled
import org.utbot.python.utils.getLineOfFunction
import java.io.File

object PythonTestGenerationProcessor {
    fun processTestGeneration(
        pythonPath: String,
        testSourceRoot: String,
        pythonFilePath: String,
        pythonFileContent: String,
        directoriesForSysPath: Set<String>,
        currentPythonModule: String,
        pythonMethods: List<PythonMethod>,
        containingClassName: String?,
        timeout: Long,
        testFramework: TestFramework,
        codegenLanguage: CodegenLanguage,
        outputFilename: String, // without path, just name
        timeoutForRun: Long,
        withMinimization: Boolean = true,
        isCanceled: () -> Boolean = { false },
        checkingRequirementsAction: () -> Unit = {},
        requirementsAreNotInstalledAction: () -> MissingRequirementsActionResult = {
            MissingRequirementsActionResult.NOT_INSTALLED
        },
        startedLoadingPythonTypesAction: () -> Unit = {},
        startedTestGenerationAction: () -> Unit = {},
        notGeneratedTestsAction: (List<String>) -> Unit = {}, // take names of functions without tests
        generatedFileWithTestsAction: (File) -> Unit = {},
        processMypyWarnings: (List<String>) -> Unit = {},
        startedCleaningAction: () -> Unit = {},
        finishedAction: (List<String>) -> Unit = {}  // take names of functions with generated tests
    ) {
        Cleaner.restart()

        try {
            FileManager.assignTestSourceRoot(testSourceRoot)

            checkingRequirementsAction()
            if (!requirementsAreInstalled(pythonPath)) {
                val result = requirementsAreNotInstalledAction()
                if (result == MissingRequirementsActionResult.NOT_INSTALLED)
                    return
            }

            startedLoadingPythonTypesAction()
            PythonTypesStorage.pythonPath = pythonPath
            PythonTypesStorage.refreshProjectClassesAndModulesLists(directoriesForSysPath)
            StubFileFinder

            startedTestGenerationAction()
            val startTime = System.currentTimeMillis()

            val testCaseGenerator = PythonTestCaseGenerator.apply {
                init(
                    directoriesForSysPath,
                    currentPythonModule,
                    pythonPath,
                    pythonFilePath,
                    timeoutForRun,
                    withMinimization
                ) { isCanceled() || (System.currentTimeMillis() - startTime) > timeout }
            }

            val tests = pythonMethods.map { method ->
                testCaseGenerator.generate(method)
            }

            val (notEmptyTests, emptyTestSets) = tests.partition { it.executions.isNotEmpty() }

            if (isCanceled())
                return

            if (emptyTestSets.isNotEmpty()) {
                notGeneratedTestsAction(emptyTestSets.map { it.method.name })
            }

            if (notEmptyTests.isEmpty())
                return

            val classId =
                if (containingClassName == null)
                    PythonClassId("$currentPythonModule.TopLevelFunctions")
                else
                    PythonClassId("$currentPythonModule.$containingClassName")

            val methodIds = notEmptyTests.associate {
                it.method to PythonMethodId(
                    classId,
                    it.method.name,
                    RawPythonAnnotation(it.method.returnAnnotation ?: pythonNoneClassId.name),
                    it.method.arguments.map { argument ->
                        argument.annotation?.let { annotation ->
                            RawPythonAnnotation(annotation)
                        } ?: pythonAnyClassId
                    }
                )
            }

            val paramNames = notEmptyTests.associate { testSet ->
                methodIds[testSet.method] as ExecutableId to testSet.method.arguments.map { it.name }
            }.toMutableMap()

            val context = UtContext(this::class.java.classLoader)
            withUtContext(context) {
                val codegen = CodeGenerator(
                    classId,
                    paramNames = paramNames,
                    testFramework = testFramework,
                    codegenLanguage = codegenLanguage,
                    testClassPackageName = "",
                )
                val testCode = codegen.generateAsStringWithTestReport(
                    notEmptyTests.map { testSet ->
                        CgMethodTestSet(
                            methodIds[testSet.method] as ExecutableId,
                            testSet.executions,
                            directoriesForSysPath,
                        )
                    }
                ).generatedCode
                val testFile = FileManager.createPermanentFile(outputFilename, testCode)
                generatedFileWithTestsAction(testFile)
            }

            val mypyReport = getMypyReport(notEmptyTests, pythonFileContent)
            if (mypyReport.isNotEmpty())
                processMypyWarnings(mypyReport)

            finishedAction(notEmptyTests.map { it.method.name })

        } finally {
            startedCleaningAction()
            Cleaner.doCleaning()
        }
    }

    enum class MissingRequirementsActionResult {
        INSTALLED, NOT_INSTALLED
    }

    private fun getMypyReport(notEmptyTests: List<PythonTestSet>, pythonFileContent: String): List<String> =
        notEmptyTests.flatMap { testSet ->
            val lineOfFunction = getLineOfFunction(pythonFileContent, testSet.method.name)
            val msgLines = testSet.mypyReport.mapNotNull {
                if (it.file != MypyAnnotations.TEMPORARY_MYPY_FILE)
                    null
                else if (lineOfFunction != null && it.line >= 0)
                    ":${it.line + lineOfFunction}: ${it.type}: ${it.message}"
                else
                    "${it.type}: ${it.message}"
            }
            if (msgLines.isNotEmpty()) {
                listOf("MYPY REPORT (function ${testSet.method.name})") + msgLines
            } else {
                emptyList()
            }
        }
}