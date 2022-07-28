package org.utbot.framework.codegen

import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.assertTrue
import org.utbot.common.FileUtil
import org.utbot.common.bracket
import org.utbot.common.info
import org.utbot.common.packageName
import org.utbot.examples.TestFrameworkConfiguration
import org.utbot.examples.conflictTriggers
import org.utbot.framework.codegen.ExecutionStatus.SUCCESS
import org.utbot.framework.codegen.model.CodeGenerator
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.description
import org.utbot.framework.plugin.api.util.withUtContext
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

class TestCodeGeneratorPipeline(private val testFrameworkConfiguration: TestFrameworkConfiguration) {

    fun runClassesCodeGenerationTests(classesStages: List<ClassStages>) {
        val pipelines = classesStages.map {
            with(it) { ClassPipeline(StageContext(testClass, testSets, testSets.size), check) }
        }

        if (pipelines.isEmpty()) return

        checkPipelinesResults(pipelines)
    }

    private fun runPipelinesStages(classesPipelines: List<ClassPipeline>): List<CodeGenerationResult> {
        val classesPipelinesNames = classesPipelines.joinToString(" ") {
            val classUnderTest = it.stageContext.classUnderTest
            classUnderTest.qualifiedName ?: error("${classUnderTest.simpleName} doesn't have a fqn name")
        }

        logger
            .info()
            .bracket("Executing code generation tests for [$classesPipelinesNames]") {
                CodeGeneration.filterPipelines(classesPipelines).forEach {
                    withUtContext(UtContext(it.stageContext.classUnderTest.java.classLoader)) {
                        processCodeGenerationStage(it)
                    }
                }

                Compilation.filterPipelines(classesPipelines).let {
                    if (it.isNotEmpty()) processCompilationStages(it)
                }

                TestExecution.filterPipelines(classesPipelines).let {
                    if (it.isNotEmpty()) processTestExecutionStages(it)
                }

                return classesPipelines.map {
                    with(it.stageContext) {
                        CodeGenerationResult(classUnderTest, numberOfTestCases, stages)
                    }
                }
            }
    }

    @Suppress("UNCHECKED_CAST")
    private fun processCodeGenerationStage(classPipeline: ClassPipeline) {
        with(classPipeline.stageContext) {
            val information = StageExecutionInformation(CodeGeneration)
            val testSets = data as List<UtMethodTestSet>

            val codegenLanguage = testFrameworkConfiguration.codegenLanguage
            val parametrizedTestSource = testFrameworkConfiguration.parametrizedTestSource
            val isParametrizedAndMocked = testFrameworkConfiguration.isParametrizedAndMocked

            val testClass = callToCodeGenerator(testSets, classUnderTest)

            // clear triggered flags from the current launch in order to get ready for the next possible run
            conflictTriggers.clear()

            // actual number of the tests in the generated testClass
            val generatedMethodsCount = testClass
                .lines()
                .count {
                    val trimmedLine = it.trimStart()
                    val prefix = when (codegenLanguage) {
                        CodegenLanguage.JAVA ->
                            when (parametrizedTestSource) {
                                ParametrizedTestSource.DO_NOT_PARAMETRIZE -> "public void "
                                ParametrizedTestSource.PARAMETRIZE -> "public void parameterizedTestsFor"
                            }

                        CodegenLanguage.KOTLIN ->
                            when (parametrizedTestSource) {
                                ParametrizedTestSource.DO_NOT_PARAMETRIZE -> "fun "
                                ParametrizedTestSource.PARAMETRIZE -> "fun parameterizedTestsFor"
                            }
                    }
                    trimmedLine.startsWith(prefix)
                }
            // expected number of the tests in the generated testClass
            // if force mocking took place in parametrized test generation,
            // we don't generate tests at all
            val expectedNumberOfGeneratedMethods =
                if (isParametrizedAndMocked) 0
                else when (parametrizedTestSource) {
                    ParametrizedTestSource.DO_NOT_PARAMETRIZE -> testSets.sumOf { it.executions.size }
                    ParametrizedTestSource.PARAMETRIZE -> testSets.filter { it.executions.isNotEmpty() }.size
                }

            // check for error in the generated file
            runCatching {
                val separator = System.lineSeparator()
                require(ERROR_REGION_BEGINNING !in testClass) {
                    val lines = testClass.lines().withIndex().toList()

                    val errorsRegionsBeginningIndices = lines
                        .filter { it.value.trimStart().startsWith(ERROR_REGION_BEGINNING) }

                    val errorsRegionsEndIndices = lines
                        .filter { it.value.trimStart().startsWith(ERROR_REGION_END) }

                    val errorRegions = errorsRegionsBeginningIndices.map { beginning ->
                        val endIndex = errorsRegionsEndIndices.indexOfFirst { it.index > beginning.index }
                        lines.subList(beginning.index + 1, errorsRegionsEndIndices[endIndex].index).map { it.value }
                    }

                    val errorText = errorRegions.joinToString(separator, separator, separator) { errorRegion ->
                        val text = errorRegion.joinToString(separator = separator)
                        "Error region in ${classUnderTest.simpleName}: $text"
                    }

                    logger.error(errorText)

                    "Errors regions has been generated: $errorText"
                }

                require(generatedMethodsCount == expectedNumberOfGeneratedMethods) {
                    "Something went wrong during the code generation for ${classUnderTest.simpleName}. " +
                            "Expected to generate $expectedNumberOfGeneratedMethods test methods, " +
                            "but got only $generatedMethodsCount"
                }
            }.onFailure {
                val classes = listOf(classPipeline).retrieveClasses()
                val buildDirectory = classes.createBuildDirectory()

                val testClassName = classPipeline.retrieveTestClassName("BrokenGeneratedTest")
                val generatedTestFile = writeTest(testClass, testClassName, buildDirectory, codegenLanguage)

                logger.error("Broken test has been written to the file: [$generatedTestFile]")
                logger.error("Failed configuration: $testFrameworkConfiguration")

                throw it
            }

            classPipeline.stageContext = copy(data = testClass, stages = stages + information.completeStage())
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun processCompilationStages(classesPipelines: List<ClassPipeline>) {
        val information = StageExecutionInformation(Compilation)
        val classes = classesPipelines.retrieveClasses()
        val buildDirectory = classes.createBuildDirectory()

        val codegenLanguage = testFrameworkConfiguration.codegenLanguage

        val testClassesNamesToTestGeneratedTests = classesPipelines.map { classPipeline ->
            val testClass = classPipeline.stageContext.data as String
            val testClassName = classPipeline.retrieveTestClassName("GeneratedTest")
            val generatedTestFile = writeTest(testClass, testClassName, buildDirectory, codegenLanguage)

            logger.info("Test has been written to the file: [$generatedTestFile]")

            testClassName to generatedTestFile
        }

        compileTests(
            "$buildDirectory",
            testClassesNamesToTestGeneratedTests.map { it.second.absolutePath },
            codegenLanguage
        )

        testClassesNamesToTestGeneratedTests.zip(classesPipelines) { testClassNameToTest, classPipeline ->
            classPipeline.stageContext = classPipeline.stageContext.copy(
                data = CompilationResult("$buildDirectory", testClassNameToTest.first),
                stages = classPipeline.stageContext.stages + information.completeStage()
            )
        }
    }

    /**
     * For simple CUT equals to its fqn + [testSuffix] suffix,
     * for nested CUT is its package + dot + its simple name + [testSuffix] suffix (to avoid outer class mention).
     */
    private fun ClassPipeline.retrieveTestClassName(testSuffix: String): String =
        stageContext.classUnderTest.let { "${it.java.packageName}.${it.simpleName}" } + testSuffix

    private fun List<KClass<*>>.createBuildDirectory() = FileUtil.isolateClassFiles(*toTypedArray()).toPath()

    private fun List<ClassPipeline>.retrieveClasses() = map { it.stageContext.classUnderTest }

    @Suppress("UNCHECKED_CAST")
    private fun processTestExecutionStages(classesPipelines: List<ClassPipeline>) {
        val information = StageExecutionInformation(TestExecution)
        val buildDirectory = (classesPipelines.first().stageContext.data as CompilationResult).buildDirectory
        val testClassesNames = classesPipelines.map { classPipeline ->
            (classPipeline.stageContext.data as CompilationResult).testClassName
        }
        with(testFrameworkConfiguration) {
            runTests(buildDirectory, testClassesNames, testFramework, codegenLanguage)
        }
        classesPipelines.forEach {
            it.stageContext = it.stageContext.copy(
                data = Unit,
                stages = it.stageContext.stages + information.completeStage()
            )
        }
    }

    private fun callToCodeGenerator(
        testSets: List<UtMethodTestSet>,
        classUnderTest: KClass<*>
    ): String {
        val params = mutableMapOf<UtMethod<*>, List<String>>()

        val codeGenerator = with(testFrameworkConfiguration) {
            CodeGenerator(
                classUnderTest.java,
                params = params,
                testFramework = testFramework,
                staticsMocking = staticsMocking,
                forceStaticMocking = forceStaticMocking,
                generateWarningsForStaticMocking = false,
                codegenLanguage = codegenLanguage,
                parameterizedTestSource = parametrizedTestSource,
                runtimeExceptionTestsBehaviour = runtimeExceptionTestsBehaviour,
                enableTestsTimeout = enableTestsTimeout
            )
        }
        val testClassCustomName = "${classUnderTest.java.simpleName}GeneratedTest"

        // if force mocking took place in parametrized test generation,
        // we don't generate tests at all by passing empty list instead of test sets
        return codeGenerator.generateAsString(
            if (testFrameworkConfiguration.isParametrizedAndMocked) listOf() else testSets,
            testClassCustomName
        )
    }

    private fun checkPipelinesResults(classesPipelines: List<ClassPipeline>) {
        val results = runPipelinesStages(classesPipelines)
        val classesChecks = classesPipelines.map { it.stageContext.classUnderTest to listOf(it.check) }
        processResults(results, classesChecks)
    }

    private fun processResults(
        results: List<CodeGenerationResult>,
        classesChecks: List<Pair<KClass<*>, List<StageStatusCheck>>>
    ) {
        val transformedResults: List<Pair<KClass<*>, List<String>>> =
            results.zip(classesChecks) { result, classChecks ->
                val stageStatusChecks = classChecks.second.mapNotNull { check ->
                    runCatching { check(result) }
                        .fold(
                            onSuccess = { if (it) null else check.description },
                            onFailure = { it.description }
                        )
                }
                result.classUnderTest to stageStatusChecks
            }
        val failedResults = transformedResults.filter { it.second.isNotEmpty() }

        assertTrue(failedResults.isEmpty()) {
            val separator = "\n\t\t"
            val failedResultsConcatenated = failedResults.joinToString(separator, prefix = separator) {
                "${it.first.simpleName} : ${it.second.joinToString()}"
            }

            "There are failed checks: $failedResultsConcatenated"
        }
    }

    companion object {
        val CodegenLanguage.defaultCodegenPipeline: TestCodeGeneratorPipeline
            get() = TestCodeGeneratorPipeline(
                TestFrameworkConfiguration(
                    testFramework = TestFramework.defaultItem,
                    codegenLanguage = this,
                    mockFramework = MockFramework.defaultItem,
                    mockStrategy = MockStrategyApi.defaultItem,
                    staticsMocking = StaticsMocking.defaultItem,
                    parametrizedTestSource = ParametrizedTestSource.defaultItem,
                    forceStaticMocking = ForceStaticMocking.defaultItem,
                )
            )

        private const val ERROR_REGION_BEGINNING = "///region Errors"
        private const val ERROR_REGION_END = "///endregion"
    }
}

enum class ExecutionStatus {
    IN_PROCESS, FAILED, SUCCESS
}

sealed class Stage(private val name: String, val nextStage: Stage?) {
    override fun toString() = name

    fun filterPipelines(classesPipelines: List<ClassPipeline>): List<ClassPipeline> =
        classesPipelines.filter {
            it.check.firstStage <= this && it.check.lastStage >= this
        }

    abstract operator fun compareTo(stage: Stage): Int
}

object CodeGeneration : Stage("Code Generation", Compilation) {
    override fun compareTo(stage: Stage): Int = if (stage is CodeGeneration) 0 else -1
}

object Compilation : Stage("Compilation", TestExecution) {
    override fun compareTo(stage: Stage): Int =
        when (stage) {
            is CodeGeneration -> 1
            is Compilation -> 0
            else -> -1
        }
}

object TestExecution : Stage("Test Execution", null) {
    override fun compareTo(stage: Stage): Int = if (stage is TestExecution) 0 else 1
}

private fun pipeline(firstStage: Stage = CodeGeneration, lastStage: Stage = TestExecution): Sequence<Stage> =
    generateSequence(firstStage) { if (it == lastStage) null else it.nextStage }

data class StageExecutionInformation(
    val stage: Stage,
    val status: ExecutionStatus = ExecutionStatus.IN_PROCESS
) {
    fun completeStage(status: ExecutionStatus = SUCCESS) = copy(status = status)
}

data class CodeGenerationResult(
    val classUnderTest: KClass<*>,
    val numberOfTestCases: Int,
    val stageStatisticInformation: List<StageExecutionInformation>
)

sealed class PipelineResultCheck(
    val description: String,
    private val check: (CodeGenerationResult) -> Boolean
) {
    open operator fun invoke(codeGenerationResult: CodeGenerationResult) = check(codeGenerationResult)
}

/**
 * Checks that stage failed and all previous stages are successfully processed.
 */
class StageStatusCheck(
    val firstStage: Stage = CodeGeneration,
    val lastStage: Stage,
    status: ExecutionStatus,
) : PipelineResultCheck(
    description = "Expect [$lastStage] to be in [$status] status",
    check = constructPipelineResultCheck(firstStage, lastStage, status)
)

private fun constructPipelineResultCheck(
    firstStage: Stage,
    lastStage: Stage,
    status: ExecutionStatus
): (CodeGenerationResult) -> Boolean =
    { result ->
        val statuses = result.stageStatisticInformation.associate { it.stage to it.status }
        val failedPrevStage = pipeline(firstStage, lastStage)
            .takeWhile { it != lastStage }
            .firstOrNull { statuses[it] != SUCCESS }

        if (failedPrevStage != null) error("[$lastStage] is not started cause $failedPrevStage has failed")

        statuses[lastStage] == status
    }

data class CompilationResult(val buildDirectory: String, val testClassName: String)

/**
 * Context to run Stage. Contains class under test, data (input of current stage), number of analyzed test cases and
 * stage execution information.
 */
data class StageContext(
    val classUnderTest: KClass<*>,
    val data: Any = Unit,
    val numberOfTestCases: Int = 0,
    val stages: List<StageExecutionInformation> = emptyList(),
    val status: ExecutionStatus = SUCCESS
)

data class ClassStages(
    val testClass: KClass<*>,
    val check: StageStatusCheck,
    val testSets: List<UtMethodTestSet> = emptyList()
)

data class ClassPipeline(var stageContext: StageContext, val check: StageStatusCheck)