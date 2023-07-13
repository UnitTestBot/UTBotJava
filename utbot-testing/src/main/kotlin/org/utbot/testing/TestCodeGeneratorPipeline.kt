package org.utbot.testing

import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.assertFalse
import org.utbot.common.FileUtil
import org.utbot.common.measureTime
import org.utbot.common.info
import org.utbot.framework.codegen.generator.CodeGeneratorResult
import org.utbot.framework.codegen.domain.ForceStaticMocking
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.codegen.domain.ProjectType
import org.utbot.framework.codegen.domain.StaticsMocking
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.codegen.generator.CodeGenerator
import org.utbot.framework.codegen.generator.SpringCodeGenerator
import org.utbot.framework.codegen.services.language.CgLanguageAssistant
import org.utbot.framework.codegen.tree.ututils.UtilClassKind
import org.utbot.framework.codegen.tree.ututils.UtilClassKind.Companion.UT_UTILS_INSTANCE_NAME
import org.utbot.framework.context.SpringApplicationContext
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.description
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.withUtContext
import java.io.File
import java.nio.file.Path
import kotlin.reflect.KClass

internal val logger = KotlinLogging.logger {}

class TestCodeGeneratorPipeline(private val testInfrastructureConfiguration: TestInfrastructureConfiguration) {

    fun runClassesCodeGenerationTests(classesStages: ClassStages) {
        val pipeline = with(classesStages) {
            ClassPipeline(StageContext(testClass, testSets, testSets.size), check)
        }

        checkPipelinesResults(pipeline)
    }

    private fun runPipelinesStages(classPipeline: ClassPipeline): CodeGenerationResult {
        val classUnderTest = classPipeline.stageContext.classUnderTest
        val classPipelineName = classUnderTest.qualifiedName
            ?: error("${classUnderTest.simpleName} doesn't have a fqn name")

        logger
            .info()
            .measureTime({ "Executing code generation tests for [$classPipelineName]" }) {
                CodeGeneration.verifyPipeline(classPipeline)?.let {
                    withUtContext(UtContext(classUnderTest.java.classLoader)) {
                        processCodeGenerationStage(it)
                    }
                }

                Compilation.verifyPipeline(classPipeline)?.let {
                    processCompilationStages(it)
                }

                TestExecution.verifyPipeline(classPipeline)?.let {
                    processTestExecutionStages(it)
                }

                return with(classPipeline.stageContext) {
                    CodeGenerationResult(classUnderTest, numberOfTestCases, stages)
                }
            }
    }

    @Suppress("UNCHECKED_CAST")
    private fun processCodeGenerationStage(classPipeline: ClassPipeline) {
        with(classPipeline.stageContext) {
            val information = StageExecutionInformation(CodeGeneration)
            val testSets = data as List<UtMethodTestSet>

            val codegenLanguage = testInfrastructureConfiguration.codegenLanguage
            val parametrizedTestSource = testInfrastructureConfiguration.parametrizedTestSource

            val codeGenerationResult = callToCodeGenerator(testSets, classUnderTest)
            val testClass = codeGenerationResult.generatedCode

            // actual number of the tests in the generated testClass
            val generatedMethodsCount = testClass
                .lines()
                .count {
                    val trimmedLine = it.trimStart()
                    val prefix = when (codegenLanguage) {
                        CodegenLanguage.JAVA ->
                            when (parametrizedTestSource) {
                                ParametrizedTestSource.DO_NOT_PARAMETRIZE -> "@Test"
                                ParametrizedTestSource.PARAMETRIZE -> "public void parameterizedTestsFor"
                            }

                        CodegenLanguage.KOTLIN ->
                            when (parametrizedTestSource) {
                                ParametrizedTestSource.DO_NOT_PARAMETRIZE -> "@Test"
                                ParametrizedTestSource.PARAMETRIZE -> "fun parameterizedTestsFor"
                            }
                    }
                    trimmedLine.startsWith(prefix)
                }
            // expected number of the tests in the generated testClass
            val expectedNumberOfGeneratedMethods =
                when (parametrizedTestSource) {
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

                // for now, we skip a comparing of generated and expected test methods
                // in parametrized test generation mode
                // because there are problems with determining expected number of methods,
                // due to a feature that generates several separated parametrized tests
                // when we have several executions with different result type
                if (parametrizedTestSource != ParametrizedTestSource.PARAMETRIZE) {
                    require(generatedMethodsCount == expectedNumberOfGeneratedMethods) {
                        "Something went wrong during the code generation for ${classUnderTest.simpleName}. " +
                                "Expected to generate $expectedNumberOfGeneratedMethods test methods, " +
                                "but got only $generatedMethodsCount"
                    }
                }
            }.onFailure {
                val classes = listOf(classPipeline).retrieveClasses()
                val buildDirectory = classes.createBuildDirectory()

                val testClassName = classPipeline.retrieveTestClassName("BrokenGeneratedTest")
                val generatedTestFile = writeTest(testClass, testClassName, buildDirectory, codegenLanguage)
                val generatedUtilClassFile = codeGenerationResult.utilClassKind?.writeUtilClassToFile(buildDirectory, codegenLanguage)

                logger.error("Broken test has been written to the file: [$generatedTestFile]")
                if (generatedUtilClassFile != null) {
                    logger.error("Util class for the broken test has been written to the file: [$generatedUtilClassFile]")
                }
                logger.error("Failed configuration: $testInfrastructureConfiguration")

                throw it
            }

            classPipeline.stageContext = copy(data = codeGenerationResult, stages = stages + information.completeStage())
        }
    }

    private fun UtilClassKind.writeUtilClassToFile(buildDirectory: Path, language: CodegenLanguage): File {
        val utilClassFile = File(buildDirectory.toFile(), "$UT_UTILS_INSTANCE_NAME${language.extension}")
        val utilClassText = getUtilClassText(language)
        return writeFile(utilClassText, utilClassFile)
    }

    private data class GeneratedTestClassInfo(
        val testClassName: String,
        val generatedTestFile: File,
        val generatedUtilClassFile: File?
    )

    @Suppress("UNCHECKED_CAST")
    private fun processCompilationStages(classesPipeline: ClassPipeline) {
        val information = StageExecutionInformation(Compilation)
        val classes = listOf(classesPipeline).retrieveClasses()
        val buildDirectory = classes.createBuildDirectory()

        val codegenLanguage = testInfrastructureConfiguration.codegenLanguage

            val codeGeneratorResult = classesPipeline.stageContext.data as CodeGeneratorResult//String
            val testClass = codeGeneratorResult.generatedCode

            val testClassName = classesPipeline.retrieveTestClassName("GeneratedTest")
            val generatedTestFile = writeTest(testClass, testClassName, buildDirectory, codegenLanguage)
            val generatedUtilClassFile = codeGeneratorResult.utilClassKind?.writeUtilClassToFile(buildDirectory, codegenLanguage)

            logger.info("Test has been written to the file: [$generatedTestFile]")
            if (generatedUtilClassFile != null) {
                logger.info("Util class for the test has been written to the file: [$generatedUtilClassFile]")
            }

            val testClassesNamesToTestGeneratedTests = GeneratedTestClassInfo(testClassName, generatedTestFile, generatedUtilClassFile)


        val sourceFiles = mutableListOf<String>().apply {
            testClassesNamesToTestGeneratedTests.generatedTestFile.absolutePath?.let { this += it }
            testClassesNamesToTestGeneratedTests.generatedUtilClassFile?.absolutePath?.let { this += it }
        }
        compileTests(
            "$buildDirectory",
            sourceFiles,
            codegenLanguage
        )

        classesPipeline.stageContext = classesPipeline.stageContext.copy(
            data = CompilationResult("$buildDirectory", testClassesNamesToTestGeneratedTests.testClassName),
            stages = classesPipeline.stageContext.stages + information.completeStage()
        )
    }

    /**
     * For simple CUT equals to its fqn + [testSuffix] suffix,
     * for nested CUT is its package + dot + its simple name + [testSuffix] suffix (to avoid outer class mention).
     */
    private fun ClassPipeline.retrieveTestClassName(testSuffix: String): String =
        stageContext.classUnderTest.let { "${it.java.`package`.name}.${it.simpleName}" } + testSuffix

    private fun List<Class<*>>.createBuildDirectory() =
        FileUtil.isolateClassFiles(*toTypedArray()).toPath()

    private fun List<ClassPipeline>.retrieveClasses() = map { it.stageContext.classUnderTest.java }

    @Suppress("UNCHECKED_CAST")
    private fun processTestExecutionStages(classesPipeline: ClassPipeline) {
        val information = StageExecutionInformation(TestExecution)
        val compilationResult = classesPipeline.stageContext.data as CompilationResult
        val buildDirectory = compilationResult.buildDirectory
        val testClassesNames = listOf(compilationResult.testClassName)

        with(testInfrastructureConfiguration) {
            runTests(buildDirectory, testClassesNames, testFramework, codegenLanguage)
        }

        classesPipeline.stageContext = classesPipeline.stageContext.copy(
                data = Unit,
                stages = classesPipeline.stageContext.stages + information.completeStage()
            )
    }

    private fun callToCodeGenerator(
        testSets: List<UtMethodTestSet>,
        classUnderTest: KClass<*>
    ): CodeGeneratorResult {
        val params = mutableMapOf<ExecutableId, List<String>>()

        withUtContext(UtContext(classUnderTest.java.classLoader)) {
            val codeGenerator = with(testInfrastructureConfiguration) {
                when (projectType) {
                    ProjectType.Spring -> SpringCodeGenerator(
                        classUnderTest.id,
                        projectType = ProjectType.Spring,
                        generateUtilClassFile = generateUtilClassFile,
                        paramNames = params,
                        testFramework = testFramework,
                        staticsMocking = staticsMocking,
                        forceStaticMocking = forceStaticMocking,
                        generateWarningsForStaticMocking = false,
                        codegenLanguage = codegenLanguage,
                        cgLanguageAssistant = CgLanguageAssistant.getByCodegenLanguage(codegenLanguage),
                        parameterizedTestSource = parametrizedTestSource,
                        runtimeExceptionTestsBehaviour = runtimeExceptionTestsBehaviour,
                        enableTestsTimeout = enableTestsTimeout,
                        springCodeGenerationContext = defaultSpringApplicationContext,
                    )
                    ProjectType.PureJvm -> CodeGenerator(
                        classUnderTest.id,
                        projectType = ProjectType.PureJvm,
                        generateUtilClassFile = generateUtilClassFile,
                        paramNames = params,
                        testFramework = testFramework,
                        staticsMocking = staticsMocking,
                        forceStaticMocking = forceStaticMocking,
                        generateWarningsForStaticMocking = false,
                        codegenLanguage = codegenLanguage,
                        cgLanguageAssistant = CgLanguageAssistant.getByCodegenLanguage(codegenLanguage),
                        parameterizedTestSource = parametrizedTestSource,
                        runtimeExceptionTestsBehaviour = runtimeExceptionTestsBehaviour,
                        enableTestsTimeout = enableTestsTimeout
                    )
                    else -> error("Unsupported project type $projectType in code generator instantiation")
                }
            }
            val testClassCustomName = "${classUnderTest.java.simpleName}GeneratedTest"

            return codeGenerator.generateAsStringWithTestReport(testSets, testClassCustomName)
        }
    }

    private fun checkPipelinesResults(classesPipeline: ClassPipeline) {
        val result = runPipelinesStages(classesPipeline)
        val classChecks = classesPipeline.stageContext.classUnderTest to listOf(classesPipeline.check)
        processResults(result, classChecks)
    }

    private fun processResults(
        result: CodeGenerationResult,
        classChecks: Pair<KClass<*>, List<StageStatusCheck>>
    ) {
        val stageStatusChecks = classChecks.second.mapNotNull { check ->
            runCatching { check(result) }
                .fold(
                    onSuccess = { if (it) null else check.description },
                    onFailure = { it.description }
                )
        }
        val transformedResult: Pair<KClass<*>, List<String>> =  result.classUnderTest to stageStatusChecks
        val failedResultExists = transformedResult.second.isNotEmpty()

        assertFalse(failedResultExists) {
            "There are failed checks: ${transformedResult.second}"
        }
    }

    companion object {
        private val defaultConfiguration =
            Configuration(CodegenLanguage.JAVA, ParametrizedTestSource.DO_NOT_PARAMETRIZE, TestExecution)

        //TODO: this variable must be lateinit, without strange default initializer
        var currentTestInfrastructureConfiguration: TestInfrastructureConfiguration = configurePipeline(defaultConfiguration)

        fun configurePipeline(configuration: AbstractConfiguration) =
            TestInfrastructureConfiguration(
                projectType = configuration.projectType,
                testFramework = TestFramework.defaultItem,
                codegenLanguage = configuration.language,
                mockFramework = MockFramework.defaultItem,
                mockStrategy = when (configuration.projectType) {
                    ProjectType.Spring -> MockStrategyApi.springDefaultItem
                    else -> MockStrategyApi.defaultItem
                },
                staticsMocking = StaticsMocking.defaultItem,
                parametrizedTestSource = configuration.parametrizedTestSource,
                forceStaticMocking = ForceStaticMocking.defaultItem,
                generateUtilClassFile = false
            ).also {
                currentTestInfrastructureConfiguration = it
            }

        private const val ERROR_REGION_BEGINNING = "///region Errors"
        private const val ERROR_REGION_END = "///endregion"
    }
}

enum class ExecutionStatus {
    IN_PROCESS, FAILED, SUCCESS
}

sealed class Stage(private val name: String, val nextStage: Stage?) {
    override fun toString() = name

    fun verifyPipeline(classesPipeline: ClassPipeline): ClassPipeline? =
        if (classesPipeline.check.firstStage <= this && classesPipeline.check.lastStage >= this) classesPipeline else null

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
    fun completeStage(status: ExecutionStatus = ExecutionStatus.SUCCESS) = copy(status = status)
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
            .firstOrNull { statuses[it] != ExecutionStatus.SUCCESS }

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
    val status: ExecutionStatus = ExecutionStatus.SUCCESS
)

data class ClassStages(
    val testClass: KClass<*>,
    val check: StageStatusCheck,
    val testSets: List<UtMethodTestSet> = emptyList()
)

data class ClassPipeline(var stageContext: StageContext, val check: StageStatusCheck)