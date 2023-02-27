package org.utbot.python

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.utbot.python.framework.codegen.model.PythonSysPathImport
import org.utbot.python.framework.codegen.model.PythonSystemImport
import org.utbot.python.framework.codegen.model.PythonUserImport
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonMethodId
import org.utbot.python.framework.api.python.PythonModel
import org.utbot.python.framework.api.python.RawPythonAnnotation
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.framework.codegen.model.PythonCodeGenerator
import org.utbot.python.newtyping.mypy.readMypyAnnotationStorageAndInitialErrors
import org.utbot.python.newtyping.mypy.setConfigFile
import org.utbot.python.typing.MypyAnnotations
import org.utbot.python.utils.Cleaner
import org.utbot.python.utils.RequirementsUtils.installRequirements
import org.utbot.python.utils.RequirementsUtils.requirementsAreInstalled
import org.utbot.python.utils.getLineOfFunction
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString

object PythonTestGenerationProcessor {
    fun processTestGeneration(
        pythonPath: String,
        pythonFilePath: String,
        pythonFileContent: String,
        directoriesForSysPath: Set<String>,
        currentPythonModule: String,
        pythonMethods: List<PythonMethodHeader>,
        containingClassName: String?,
        timeout: Long,
        testFramework: TestFramework,
        timeoutForRun: Long,
        writeTestTextToFile: (String) -> Unit,
        pythonRunRoot: Path,
        doNotCheckRequirements: Boolean = false,
        withMinimization: Boolean = true,
        isCanceled: () -> Boolean = { false },
        checkingRequirementsAction: () -> Unit = {},
        installingRequirementsAction: () -> Unit = {},
        testFrameworkInstallationAction: () -> Unit = {},
        requirementsAreNotInstalledAction: () -> MissingRequirementsActionResult = {
            MissingRequirementsActionResult.NOT_INSTALLED
        },
        startedLoadingPythonTypesAction: () -> Unit = {},
        startedTestGenerationAction: () -> Unit = {},
        notGeneratedTestsAction: (List<String>) -> Unit = {}, // take names of functions without tests
        processMypyWarnings: (List<String>) -> Unit = {},
        processCoverageInfo: (String) -> Unit = {},
        startedCleaningAction: () -> Unit = {},
        finishedAction: (List<String>) -> Unit = {},  // take names of functions with generated tests
    ) {
        Cleaner.restart()

        try {
            if (!testFramework.isInstalled) {
                testFrameworkInstallationAction()
                installRequirements(pythonPath, listOf(testFramework.mainPackage))
            }
            if (!doNotCheckRequirements) {
                checkingRequirementsAction()
                if (!requirementsAreInstalled(pythonPath)) {
                    installingRequirementsAction()
                    val result = requirementsAreNotInstalledAction()
                    if (result == MissingRequirementsActionResult.NOT_INSTALLED)
                        return
                }
            }

            startedLoadingPythonTypesAction()

            val mypyConfigFile = setConfigFile(directoriesForSysPath)
            val (mypyStorage, report) = readMypyAnnotationStorageAndInitialErrors(
                pythonPath,
                pythonFilePath,
                currentPythonModule,
                mypyConfigFile
            )

            startedTestGenerationAction()

            val startTime = System.currentTimeMillis()

            val testCaseGenerator = PythonTestCaseGenerator(
                withMinimization = withMinimization,
                directoriesForSysPath = directoriesForSysPath,
                curModule = currentPythonModule,
                pythonPath = pythonPath,
                fileOfMethod = pythonFilePath,
                isCancelled = isCanceled,
                timeoutForRun = timeoutForRun,
                sourceFileContent = pythonFileContent,
                mypyStorage = mypyStorage,
                mypyReportLine = report,
                mypyConfigFile = mypyConfigFile,
            )

            val until = startTime + timeout
            val tests = pythonMethods.mapIndexed { index, method ->
                val methodsLeft = pythonMethods.size - index
                val localUntil = (until - System.currentTimeMillis()) / methodsLeft + System.currentTimeMillis()
                testCaseGenerator.generate(method, localUntil)
            }

            val (notEmptyTests, emptyTestSets) = tests.partition { it.executions.isNotEmpty() }

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
                    RawPythonAnnotation(pythonAnyClassId.name),
                    it.method.arguments.map { argument ->
                        argument.annotation?.let { annotation ->
                            RawPythonAnnotation(annotation)
                        } ?: pythonAnyClassId
                    }
                )
            }

            val paramNames = notEmptyTests.associate { testSet ->
                var params = testSet.method.arguments.map { it.name }
                if (testSet.method.hasThisArgument) {
                    params = params.drop(1)
                }
                methodIds[testSet.method] as ExecutableId to params
            }.toMutableMap()

            val importParamModules = notEmptyTests.flatMap { testSet ->
                testSet.executions.flatMap { execution ->
                    (execution.stateBefore.parameters + execution.stateAfter.parameters +
                            listOf(execution.stateBefore.thisInstance, execution.stateAfter.thisInstance))
                        .filterNotNull()
                        .flatMap { utModel ->
                        (utModel as PythonModel).let {
                            it.allContainingClassIds.map { classId ->
                                PythonUserImport(importName_ = classId.moduleName)
                            }
                        }
                    }
                }
            }
            val importResultModules = notEmptyTests.flatMap { testSet ->
                testSet.executions.mapNotNull { execution ->
                    if (execution.result is UtExecutionSuccess) {
                        (execution.result as UtExecutionSuccess).let { result ->
                            (result.model as PythonModel).let {
                                it.allContainingClassIds.map { classId ->
                                    PythonUserImport(importName_ = classId.moduleName)
                                }
                            }
                        }
                    } else null
                }.flatten()
            }
            val testRootModules = notEmptyTests.mapNotNull { testSet ->
                methodIds[testSet.method]?.rootModuleName?.let { PythonUserImport(importName_ = it) }
            }
            val sysImport = PythonSystemImport("sys")
            val sysPathImports = relativizePaths(pythonRunRoot, directoriesForSysPath).map { PythonSysPathImport(it) }

            val testFrameworkModule =
                testFramework.testSuperClass?.let { PythonUserImport(importName_ = (it as PythonClassId).rootModuleName) }

            val allImports = (
                    importParamModules + importResultModules + testRootModules + sysPathImports + listOf(
                        testFrameworkModule,
                        sysImport
                    )
                ).filterNotNull().toSet()

            val context = UtContext(this::class.java.classLoader)
            withUtContext(context) {
                val codegen = PythonCodeGenerator(
                    classId,
                    paramNames = paramNames,
                    testFramework = testFramework,
                    testClassPackageName = "",
                )
                val testCode = codegen.pythonGenerateAsStringWithTestReport(
                    notEmptyTests.map { testSet ->
                        CgMethodTestSet(
                            executableId = methodIds[testSet.method] as ExecutableId,
                            executions = testSet.executions
                        )
                    },
                    allImports
                ).generatedCode
                writeTestTextToFile(testCode)
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
                val coverage = execution.coverage ?: return@inner
                coverage.coveredInstructions.forEach { covered.add(it.lineNumber) }
                missed.add(coverage.missedInstructions.map { it.lineNumber }.toSet())
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