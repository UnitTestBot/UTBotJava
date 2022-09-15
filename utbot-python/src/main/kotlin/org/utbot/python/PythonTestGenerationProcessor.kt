package org.utbot.python

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.utbot.framework.codegen.*
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
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString

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
        doNotCheckRequirements: Boolean = false,
        visitOnlySpecifiedSource: Boolean = false,
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
        processCoverageInfo: (String) -> Unit = {},
        startedCleaningAction: () -> Unit = {},
        finishedAction: (List<String>) -> Unit = {},  // take names of functions with generated tests
        pythonRunRoot: Path? = null
    ) {
        Cleaner.restart()

        try {
            FileManager.assignTestSourceRoot(testSourceRoot)

            if (!doNotCheckRequirements) {
                checkingRequirementsAction()
                if (!requirementsAreInstalled(pythonPath)) {
                    val result = requirementsAreNotInstalledAction()
                    if (result == MissingRequirementsActionResult.NOT_INSTALLED)
                        return
                }
            }

            startedLoadingPythonTypesAction()
            PythonTypesStorage.pythonPath = pythonPath

            val onlySpecifiedFile = if (!visitOnlySpecifiedSource) null else File(pythonFilePath)
            PythonTypesStorage.refreshProjectClassesAndModulesLists(directoriesForSysPath, onlySpecifiedFile)
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
                    withMinimization,
                    pythonRunRoot = pythonRunRoot ?: Path(testSourceRoot)
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

            val importParamModules = notEmptyTests.flatMap { testSet ->
                testSet.executions.flatMap { execution ->
                    execution.stateBefore.parameters.flatMap { utModel ->
                        (utModel as PythonModel).let {
                            it.allContainingClassIds.map { classId ->
                                PythonUserImport(classId.moduleName)
                            }
                        }
                    }
                }
            }
            val testRootModules = notEmptyTests.mapNotNull { testSet ->
                methodIds[testSet.method]?.rootModuleName?.let { PythonUserImport(it) }
            }
            val testFrameworkModule = PythonSystemImport(
                (testFramework.testSuperClass as PythonClassId).rootModuleName
            )
            val sysImport = PythonSystemImport("sys")
            val sysPathImports = relativizePaths(pythonRunRoot, directoriesForSysPath).map { PythonSysPathImport(it) }

            val allImports = importParamModules + testRootModules + sysPathImports + listOf(testFrameworkModule, sysImport)

            val context = UtContext(this::class.java.classLoader)
            withUtContext(context) {
                val codegen = CodeGenerator(
                    classId,
                    paramNames = paramNames,
                    testFramework = testFramework,
                    codegenLanguage = codegenLanguage,
                    testClassPackageName = "",
                )
                val testCode = codegen.pythonGenerateAsStringWithTestReport(
                    notEmptyTests.map { testSet ->
                        CgMethodTestSet(
                            methodIds[testSet.method] as ExecutableId,
                            testSet.executions,
                        )
                    },
                    allImports.toSet()
                ).generatedCode
                val testFile = FileManager.createPermanentFile(outputFilename, testCode)
                generatedFileWithTestsAction(testFile)
            }

            val coverageInfo = getCoverageInfo(notEmptyTests)
            processCoverageInfo(coverageInfo)

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

    data class InstructionSet(
        val start: Int,
        val end: Int
    )

    data class CoverageInfo(
        val covered: List<InstructionSet>,
        val notCovered: List<InstructionSet>
    )

    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val jsonAdapter = moshi.adapter(CoverageInfo::class.java)

    private fun getInstructionSetList(instructions: Collection<Int>): List<InstructionSet> =
        instructions.sorted().fold(emptyList()) { acc, lineNumber ->
            if (acc.isEmpty())
                return@fold listOf(InstructionSet(lineNumber, lineNumber))
            val elem = acc.last()
            if (elem.end + 1 == lineNumber)
                acc.dropLast(1) + listOf(InstructionSet(elem.start, lineNumber))
            else
                acc + listOf(InstructionSet(lineNumber, lineNumber))
        }

    private fun getCoverageInfo(testSets: List<PythonTestSet>): String {
        val covered = mutableSetOf<Int>()
        val missed = mutableSetOf<Set<Int>>()
        testSets.forEach { testSet ->
            testSet.executions.forEach inner@{ execution ->
                val coverage = execution.coverage as? PythonCoverage ?: return@inner
                coverage.coveredInstructions.forEach { covered.add(it.lineNumber) }
                missed.add(coverage.missedInstructions.map { it.lineNumber } .toSet())
            }
        }
        val coveredInstructionSets = getInstructionSetList(covered)
        val missedInstructionSets =
            if (missed.isEmpty())
                emptyList()
            else
                getInstructionSetList(missed.reduce { a, b -> a intersect b })

        return jsonAdapter.toJson(CoverageInfo(coveredInstructionSets, missedInstructionSets))
    }

    private fun relativizePaths(rootPath: Path?, paths: Set<String>): Set<String> =
        if (rootPath != null) {
            paths.map { path ->
                rootPath.relativize(Path(path)).pathString
            }.toSet()
        } else {
            paths
        }
}