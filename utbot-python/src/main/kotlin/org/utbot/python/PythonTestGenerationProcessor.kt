package org.utbot.python

import org.utbot.common.appendHtmlLine
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
        isCanceled: () -> Boolean = { false },
        startedMypyInstallationAction: () -> Unit = {},
        couldNotInstallMypyAction: () -> Unit = {},
        startedLoadingPythonTypesAction: () -> Unit = {},
        startedTestGenerationAction: () -> Unit = {},
        notGeneratedTestsAction: (List<String>) -> Unit = {}, // take names of functions without tests
        generatedFileWithTestsAction: (File) -> Unit = {},
        processMypyWarnings: (String) -> Unit = {},
        startedCleaningAction: () -> Unit = {}
    ) {
        Cleaner.restart()

        try {
            FileManager.assignTestSourceRoot(testSourceRoot)

            if (!MypyAnnotations.mypyInstalled(pythonPath) && !isCanceled()) {
                startedMypyInstallationAction()
                MypyAnnotations.installMypy(pythonPath)
                if (!MypyAnnotations.mypyInstalled(pythonPath))
                    couldNotInstallMypyAction()
            }

            startedLoadingPythonTypesAction()
            PythonTypesStorage.pythonPath = pythonPath
            PythonTypesStorage.refreshProjectClassesList(directoriesForSysPath)
            StubFileFinder

            startedTestGenerationAction()
            val startTime = System.currentTimeMillis()

            val testCaseGenerator = PythonTestCaseGenerator.apply {
                init(
                    directoriesForSysPath,
                    currentPythonModule,
                    pythonPath,
                    pythonFilePath
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

            val mypyReport = notEmptyTests.fold(StringBuilder()) { acc, testSet ->
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
                    acc.appendHtmlLine("MYPY REPORT (function ${testSet.method.name})")
                    msgLines.forEach { acc.appendHtmlLine(it) }
                }
                acc
            }.toString()

            if (mypyReport != "")
                processMypyWarnings(mypyReport)

        } finally {
            startedCleaningAction()
            Cleaner.doCleaning()
        }
    }
}