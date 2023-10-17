package org.utbot.python

import mu.KotlinLogging
import org.parsers.python.PythonParser
import org.utbot.framework.codegen.domain.HangingTestsTimeout
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.UtClusterInfo
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.python.code.PythonCode
import org.utbot.python.evaluation.coverage.CoverageOutputFormat
import org.utbot.python.evaluation.coverage.PyInstruction
import org.utbot.python.evaluation.coverage.toPair
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonMethodId
import org.utbot.python.framework.api.python.PythonModel
import org.utbot.python.framework.api.python.PythonUtExecution
import org.utbot.python.framework.api.python.RawPythonAnnotation
import org.utbot.python.framework.api.python.pythonBuiltinsModuleName
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.framework.api.python.util.pythonNoneClassId
import org.utbot.python.framework.codegen.model.PythonCodeGenerator
import org.utbot.python.framework.codegen.model.PythonImport
import org.utbot.python.framework.codegen.model.PythonSysPathImport
import org.utbot.python.framework.codegen.model.PythonSystemImport
import org.utbot.python.framework.codegen.model.PythonUserImport
import org.utbot.python.newtyping.PythonFunctionDefinition
import org.utbot.python.newtyping.general.CompositeType
import org.utbot.python.newtyping.getPythonAttributes
import org.utbot.python.newtyping.mypy.MypyBuildDirectory
import org.utbot.python.newtyping.mypy.MypyInfoBuild
import org.utbot.python.newtyping.mypy.MypyReportLine
import org.utbot.python.newtyping.mypy.readMypyAnnotationStorageAndInitialErrors
import org.utbot.python.newtyping.pythonName
import org.utbot.python.utils.TemporaryFileManager
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString

private val logger = KotlinLogging.logger {}

// TODO: add asserts that one or less of containing classes and only one file
abstract class PythonTestGenerationProcessor {
    abstract val configuration: PythonTestGenerationConfig
    private val mypyBuildRoot = TemporaryFileManager.assignTemporaryFile(tag = "mypyBuildRoot")

    fun sourceCodeAnalyze(): Pair<MypyInfoBuild, List<MypyReportLine>> {
        return readMypyAnnotationStorageAndInitialErrors(
            configuration.pythonPath,
            configuration.testFileInformation.testedFilePath,
            configuration.testFileInformation.moduleName,
            MypyBuildDirectory(mypyBuildRoot, configuration.sysPathDirectories)
        )
    }

    fun testGenerate(mypyStorage: MypyInfoBuild): List<PythonTestSet> {
        val startTime = System.currentTimeMillis()

        val testCaseGenerator = PythonTestCaseGenerator(
            withMinimization = configuration.withMinimization,
            directoriesForSysPath = configuration.sysPathDirectories,
            curModule = configuration.testFileInformation.moduleName,
            pythonPath = configuration.pythonPath,
            fileOfMethod = configuration.testFileInformation.testedFilePath,
            isCancelled = configuration.isCanceled,
            timeoutForRun = configuration.timeoutForRun,
            sourceFileContent = configuration.testFileInformation.testedFileContent,
            mypyStorage = mypyStorage,
            mypyReportLine = emptyList(),
            coverageMode = configuration.coverageMeasureMode,
            sendCoverageContinuously = configuration.sendCoverageContinuously,
        )

        val until = startTime + configuration.timeout
        val tests = configuration.testedMethods.mapIndexedNotNull { index, methodHeader ->
            val methodsLeft = configuration.testedMethods.size - index
            val localUntil = (until - System.currentTimeMillis()) / methodsLeft + System.currentTimeMillis()
            try {
                val method = findMethodByHeader(
                    mypyStorage,
                    methodHeader,
                    configuration.testFileInformation.moduleName,
                    configuration.testFileInformation.testedFileContent
                )
                testCaseGenerator.generate(method, localUntil)
            } catch (e: SelectedMethodIsNotAFunctionDefinition) {
                logger.warn { "Skipping method ${e.methodName}: did not find its function definition" }
                null
            }
        }
        val (notEmptyTests, emptyTestSets) = tests.partition { it.executions.isNotEmpty() }

        if (emptyTestSets.isNotEmpty()) {
            notGeneratedTestsAction(emptyTestSets.map { it.method.name })
        }

        return notEmptyTests
    }

    fun testCodeGenerate(testSets: List<PythonTestSet>): String {
        val containingClassName = getContainingClassName(testSets)
        val classId = PythonClassId(configuration.testFileInformation.moduleName, containingClassName)

        val methodIds = testSets.associate { testSet ->
            testSet.method to PythonMethodId(
                classId,
                testSet.method.name,
                RawPythonAnnotation(pythonAnyClassId.name),
                testSet.method.arguments.map { argument ->
                    argument.annotation?.let { annotation ->
                        RawPythonAnnotation(annotation)
                    } ?: pythonAnyClassId
                },
            )
        }

        val paramNames = testSets.associate { testSet ->
            var params = testSet.method.arguments.map { it.name }
            if (testSet.method.hasThisArgument) {
                params = params.drop(1)
            }
            methodIds[testSet.method] as ExecutableId to params
        }.toMutableMap()

        val allImports = collectImports(testSets)

        val context = UtContext(this::class.java.classLoader)
        withUtContext(context) {
            val codegen = PythonCodeGenerator(
                classId,
                paramNames = paramNames,
                testFramework = configuration.testFramework,
                testClassPackageName = "",
                hangingTestsTimeout = HangingTestsTimeout(configuration.timeoutForRun),
                runtimeExceptionTestsBehaviour = configuration.runtimeExceptionTestsBehaviour,
            )
            val testCode = codegen.pythonGenerateAsStringWithTestReport(
                testSets.map { testSet ->
                    val intRange = testSet.executions.indices
                    val clusterInfo = listOf(Pair(UtClusterInfo("FUZZER"), intRange))
                    CgMethodTestSet(
                        executableId = methodIds[testSet.method] as ExecutableId,
                        executions = testSet.executions,
                        clustersInfo = clusterInfo,
                    )
                },
                allImports
            ).generatedCode
            return testCode
        }
    }

    abstract fun saveTests(testsCode: String)

    abstract fun notGeneratedTestsAction(testedFunctions: List<String>)

    abstract fun processCoverageInfo(testSets: List<PythonTestSet>)

    private fun getContainingClassName(testSets: List<PythonTestSet>): String {
        val containingClasses = testSets.map { it.method.containingPythonClass?.pythonName() ?: "TopLevelFunctions" }
        return containingClasses.toSet().first()
    }

    private fun collectImports(notEmptyTests: List<PythonTestSet>): Set<PythonImport> {
        val importParamModules = notEmptyTests.flatMap { testSet ->
            testSet.executions.flatMap { execution ->
                val params = (execution.stateAfter.parameters + execution.stateBefore.parameters).toMutableList()
                val self = mutableListOf(execution.stateBefore.thisInstance, execution.stateAfter.thisInstance)
                if (execution is PythonUtExecution) {
                    params.addAll(execution.stateInit.parameters)
                    self.add(execution.stateInit.thisInstance)
                }
                (params + self)
                    .filterNotNull()
                    .flatMap { utModel ->
                        (utModel as PythonModel).let {
                            it.allContainingClassIds
                                .filterNot { classId -> classId == pythonNoneClassId }
                                .map { classId -> PythonUserImport(importName_ = classId.moduleName) }
                        }
                    }
            }
        }.toSet()
        val importResultModules = notEmptyTests.flatMap { testSet ->
            testSet.executions.mapNotNull { execution ->
                if (execution.result is UtExecutionSuccess) {
                    (execution.result as UtExecutionSuccess).let { result ->
                        (result.model as PythonModel).let {
                            it.allContainingClassIds
                                .filterNot { classId -> classId == pythonNoneClassId }
                                .map { classId -> PythonUserImport(importName_ = classId.moduleName) }
                        }
                    }
                } else null
            }.flatten()
        }.toSet()
        val rootModule = configuration.testFileInformation.moduleName.split(".").first()
        val testRootModule = PythonUserImport(importName_ = rootModule)
        val sysImport = PythonSystemImport("sys")
        val osImport = PythonSystemImport("os")
        val sysPathImports = relativizePaths(
            configuration.testSourceRootPath,
            configuration.sysPathDirectories
        ).map { PythonSysPathImport(it) }

        val testFrameworkModule =
            configuration.testFramework.testSuperClass?.let { PythonUserImport(importName_ = (it as PythonClassId).rootModuleName) }

        return (importParamModules + importResultModules + testRootModule + sysPathImports + listOf(
            testFrameworkModule, osImport, sysImport
        ))
            .filterNotNull()
            .filterNot { it.rootModuleName == pythonBuiltinsModuleName }
            .toSet()
    }

    private fun findMethodByHeader(
        mypyStorage: MypyInfoBuild,
        method: PythonMethodHeader,
        curModule: String,
        sourceFileContent: String
    ): PythonMethod {
        var containingClass: CompositeType? = null
        val containingClassName = method.containingPythonClassId?.simpleName
        val functionDef = if (containingClassName == null) {
            mypyStorage.definitions[curModule]!![method.name]!!.getUtBotDefinition()!!
        } else {
            containingClass =
                mypyStorage.definitions[curModule]!![containingClassName]!!.getUtBotType() as CompositeType
            mypyStorage.definitions[curModule]!![containingClassName]!!.type.asUtBotType.getPythonAttributes().first {
                it.meta.name == method.name
            }
        } as? PythonFunctionDefinition ?: throw SelectedMethodIsNotAFunctionDefinition(method.name)

        val parsedFile = PythonParser(sourceFileContent).Module()
        val funcDef = PythonCode.findFunctionDefinition(parsedFile, method)

        return PythonMethod(
            name = method.name,
            moduleFilename = method.moduleFilename,
            containingPythonClass = containingClass,
            codeAsString = funcDef.body.source,
            definition = functionDef,
            ast = funcDef.body
        )
    }

    private fun relativizePaths(rootPath: Path?, paths: Set<String>): Set<String> =
        if (rootPath != null) {
            paths.map { path ->
                rootPath.relativize(Path(path)).pathString
            }.toSet()
        } else {
            paths
        }

    sealed class CoverageFormat
    data class LineCoverage(val start: Int, val end: Int) : CoverageFormat() {
        override fun equals(other: Any?): Boolean {
            if (other is LineCoverage) {
                return start == other.start && end == other.end
            }
            return false
        }

        override fun hashCode(): Int {
            var result = start
            result = 31 * result + end
            return result
        }
    }
    data class InstructionCoverage(val line: Int, val offset: Long) : CoverageFormat() {
        override fun equals(other: Any?): Boolean {
            if (other is InstructionCoverage) {
                return line == other.line && offset == other.offset
            }
            return false
        }

        override fun hashCode(): Int {
            var result = line
            result = 31 * result + offset.hashCode()
            return result
        }
    }

    data class InstructionIdCoverage(val line: Int, val offset: Long, val globalOffset: Long) : CoverageFormat() {
        override fun equals(other: Any?): Boolean {
            if (other is InstructionIdCoverage) {
                return line == other.line && offset == other.offset && globalOffset == other.globalOffset
            }
            return false
        }

        override fun hashCode(): Int {
            var result = line
            result = 31 * result + offset.hashCode()
            result = 31 * result + globalOffset.hashCode()
            return result
        }

    }

    data class CoverageInfo<T: CoverageFormat>(
        val covered: List<T>,
        val notCovered: List<T>,
    )

    private fun getLinesList(instructions: Collection<PyInstruction>): List<LineCoverage> =
        instructions
            .map { it.lineNumber }
            .sorted()
            .fold(emptyList()) { acc, lineNumber ->
                if (acc.isEmpty())
                    return@fold listOf(LineCoverage(lineNumber, lineNumber))
                val elem = acc.last()
                if (elem.end + 1 == lineNumber || elem.end == lineNumber )
                    acc.dropLast(1) + listOf(LineCoverage(elem.start, lineNumber))
                else
                    acc + listOf(LineCoverage(lineNumber, lineNumber))
            }

    private fun filterMissedLines(covered: Collection<LineCoverage>, missed: Collection<PyInstruction>): List<PyInstruction> =
        missed.filterNot { missedInstruction -> covered.any { it.start <= missedInstruction.lineNumber && missedInstruction.lineNumber <= it.end } }

    private fun getInstructionsListWithId(instructions: Collection<PyInstruction>): List<CoverageFormat> =
        instructions.map { InstructionIdCoverage(it.lineNumber, it.offset, it.globalOffset) }.toSet().toList()

    private fun getInstructionsListWithOffset(instructions: Collection<PyInstruction>): List<CoverageFormat> =
        instructions.map { InstructionCoverage(it.lineNumber, it.offset) }.toSet().toList()

    private fun getCoverageInfo(testSets: List<PythonTestSet>): CoverageInfo<CoverageFormat> {
        val covered = mutableSetOf<PyInstruction>()
        val missed = mutableSetOf<PyInstruction>()
        testSets.forEach { testSet ->
            testSet.executions.forEach inner@{ execution ->
                val coverage = execution.coverage ?: return@inner
                covered.addAll(coverage.coveredInstructions.map { PyInstruction(it.lineNumber, it.id) })
                missed.addAll(coverage.missedInstructions.map { PyInstruction(it.lineNumber, it.id) })
            }
        }
        missed -= covered
        val info = when (this.configuration.coverageOutputFormat) {
            CoverageOutputFormat.Lines -> {
                val coveredLines = getLinesList(covered)
                val filteredMissed = filterMissedLines(coveredLines, missed)
                val missedLines = getLinesList(filteredMissed)
                CoverageInfo(coveredLines, missedLines)
            }
            CoverageOutputFormat.Instructions -> CoverageInfo(getInstructionsListWithId(covered), getInstructionsListWithId(missed))
            CoverageOutputFormat.TopFrameInstructions -> {
                val filteredCovered = covered.filter { it.id.toPair().first == it.id.toPair().second }
                val filteredMissed = missed.filter { it.id.toPair().first == it.id.toPair().second }

                val coveredInstructions = getInstructionsListWithOffset(filteredCovered)
                val missedInstructions = getInstructionsListWithOffset(filteredMissed)

                CoverageInfo(coveredInstructions, (missedInstructions.toSet() - coveredInstructions.toSet()).toList())
            }
        }
        return CoverageInfo(info.covered.toSet().toList(), info.notCovered.toSet().toList())
    }

    private fun toJson(coverageInfo: CoverageInfo<CoverageFormat>): String {
        val covered = coverageInfo.covered.map { toJson(it) }
        val notCovered = coverageInfo.notCovered.map { toJson(it) }
        return "{\"covered\": [${covered.joinToString(", ")}], \"notCovered\": [${notCovered.joinToString(", ")}]}"
    }

    private fun toJson(coverageFormat: CoverageFormat): String {
        return when (coverageFormat) {
            is LineCoverage -> "{\"start\": ${coverageFormat.start}, \"end\": ${coverageFormat.end}}"
            is InstructionCoverage -> "{\"line\": ${coverageFormat.line}, \"offset\": ${coverageFormat.offset}}"
            is InstructionIdCoverage -> "{\"line\": ${coverageFormat.line}, \"offset\": ${coverageFormat.offset}, \"globalOffset\": ${coverageFormat.globalOffset}}"
        }
    }

    protected fun getStringCoverageInfo(testSets: List<PythonTestSet>): String {
        val value = getCoverageInfo(testSets)
        return toJson(value)
    }

}

data class SelectedMethodIsNotAFunctionDefinition(val methodName: String): Exception()