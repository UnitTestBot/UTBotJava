package org.utbot.testing

import org.utbot.common.FileUtil
import org.utbot.common.withAccessibility
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.instrumentation.ConcreteExecutor
import kotlin.reflect.KClass
import mu.KotlinLogging
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.fail
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import java.nio.file.Path

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ExtendWith(CodeGenerationIntegrationTest.Companion.ReadRunningTestsNumberBeforeAllTestsCallback::class)
abstract class CodeGenerationIntegrationTest(
    private val testClass: KClass<*>,
    private var testCodeGeneration: Boolean = true,
    private val languagesLastStages: List<TestLastStage> = listOf(
        TestLastStage(CodegenLanguage.JAVA),
        TestLastStage(CodegenLanguage.KOTLIN)
    )
) {
    private val testSets: MutableList<UtMethodTestSet> = arrayListOf()

    data class TestLastStage(
        val language: CodegenLanguage,
        val lastStage: Stage = TestExecution,
        val parameterizedModeLastStage: Stage = lastStage,
    )

    fun processTestCase(testSet: UtMethodTestSet) {
        if (testCodeGeneration) testSets += testSet
    }

    protected fun withEnabledTestingCodeGeneration(testCodeGeneration: Boolean, block: () -> Unit) {
        val prev = this.testCodeGeneration

        try {
            this.testCodeGeneration = testCodeGeneration
            block()
        } finally {
            this.testCodeGeneration = prev
        }
    }

    // save all generated test cases from current class to test code generation
    private fun addTestCase(pkg: Package) {
        if (testCodeGeneration) {
            packageResult.getOrPut(pkg) { mutableListOf() } += CodeGenerationTestCases(
                testClass,
                testSets,
                languagesLastStages
            )
        }
    }

    private fun cleanAfterProcessingPackage(pkg: Package) {
        // clean test cases after cur package processing
        packageResult[pkg]?.clear()

        // clean cur package test classes info
        testClassesAmountByPackage[pkg] = 0
        processedTestClassesAmountByPackage[pkg] = 0
    }

    @Test
    @Order(Int.MAX_VALUE)
    fun processTestCases(testInfo: TestInfo) {
        val pkg = testInfo.testClass.get().`package`
        addTestCase(pkg)

        // check if tests are inside package and current test is not the last one
        if (runningTestsNumber > 1 && !isPackageFullyProcessed(testInfo.testClass.get())) {
            logger.info("Package $pkg is not fully processed yet, code generation will be tested later")
            return
        }
        ConcreteExecutor.defaultPool.close()

        FileUtil.clearTempDirectory(UtSettings.daysLimitForTempFiles)

        val result = packageResult[pkg] ?: return
        try {
            val pipelineErrors = mutableListOf<String?>()

            // TODO: leave kotlin & parameterized mode configuration alone for now
            val pipelineConfigurations = languages
                .flatMap { language -> parameterizationModes.map { mode -> language to mode } }
                .filterNot { it == CodegenLanguage.KOTLIN to ParametrizedTestSource.PARAMETRIZE }

            for ((language, parameterizationMode) in pipelineConfigurations) {
                try {
                    // choose all test cases that should be tested with current language
                    val languageSpecificResults = result.filter { codeGenerationTestCases ->
                        codeGenerationTestCases.lastStages.any { it.language == language }
                    }

                    // for each test class choose code generation pipeline stages
                    val classStages = languageSpecificResults.map { codeGenerationTestCases ->
                        val codeGenerationConfiguration =
                            codeGenerationTestCases.lastStages.single { it.language == language }

                        val lastStage = when (parameterizationMode) {
                            ParametrizedTestSource.DO_NOT_PARAMETRIZE -> {
                                codeGenerationConfiguration.lastStage
                            }

                            ParametrizedTestSource.PARAMETRIZE -> {
                                codeGenerationConfiguration.parameterizedModeLastStage
                            }
                        }

                        ClassStages(
                            codeGenerationTestCases.testClass,
                            StageStatusCheck(
                                firstStage = CodeGeneration,
                                lastStage = lastStage,
                                status = ExecutionStatus.SUCCESS
                            ),
                            codeGenerationTestCases.testSets
                        )
                    }

                    val config =
                        TestCodeGeneratorPipeline.defaultTestFrameworkConfiguration(language, parameterizationMode)
                    TestCodeGeneratorPipeline(config).runClassesCodeGenerationTests(classStages)
                } catch (e: RuntimeException) {
                    logger.warn(e) { "error in test pipeline" }
                    pipelineErrors.add(e.message)
                }
            }

            if (pipelineErrors.isNotEmpty())
                fail { pipelineErrors.joinToString(System.lineSeparator()) }
        } finally {
            cleanAfterProcessingPackage(pkg)
        }
    }

    companion object {

        init {
            // trigger old temporary file deletion
            FileUtil.OldTempFileDeleter
        }

        private val packageResult: MutableMap<Package, MutableList<CodeGenerationTestCases>> = mutableMapOf()

        private var allRunningTestClasses: List<ClassTestDescriptor> = mutableListOf()

        private val languages = listOf(CodegenLanguage.JAVA, CodegenLanguage.KOTLIN)

        private val parameterizationModes = listOf(ParametrizedTestSource.DO_NOT_PARAMETRIZE, ParametrizedTestSource.PARAMETRIZE)

        data class CodeGenerationTestCases(
            val testClass: KClass<*>,
            val testSets: List<UtMethodTestSet>,
            val lastStages: List<TestLastStage>
        )

        class ReadRunningTestsNumberBeforeAllTestsCallback : BeforeAllCallback {
            override fun beforeAll(extensionContext: ExtensionContext) {
                val clazz = Class.forName("org.junit.jupiter.engine.descriptor.AbstractExtensionContext")
                val field = clazz.getDeclaredField("testDescriptor")
                runningTestsNumber = field.withAccessibility {
                    val testDescriptor = field.get(extensionContext.parent.get())
                    // get all running tests and filter disabled
                    allRunningTestClasses = (testDescriptor as JupiterEngineDescriptor).children
                        .map { it as ClassTestDescriptor }
                        .filter { it.testClass.getAnnotation(Disabled::class.java) == null }
                        .toList()
                    allRunningTestClasses.size
                }
            }
        }

        private var processedTestClassesAmountByPackage: MutableMap<Package, Int> = mutableMapOf()
        private var testClassesAmountByPackage: MutableMap<Package, Int> = mutableMapOf()

        private var runningTestsNumber: Int = 0

        internal val logger = KotlinLogging.logger { }

        @JvmStatic
        protected val testCaseGeneratorCache = mutableMapOf<BuildInfo, TestSpecificTestCaseGenerator>()
        data class BuildInfo(val buildDir: Path, val dependencyPath: String?)

        private fun getTestPackageSize(packageName: String): Int =
            // filter all not disabled tests classes
            allRunningTestClasses
                .filter { it.testClass.`package`.name == packageName }
                .distinctBy { it.testClass.name.substringBeforeLast("Kt") }
                .size

        private fun isPackageFullyProcessed(testClass: Class<*>): Boolean {
            val currentPackage = testClass.`package`

            if (currentPackage !in testClassesAmountByPackage)
                testClassesAmountByPackage[currentPackage] = getTestPackageSize(currentPackage.name)

            processedTestClassesAmountByPackage.merge(currentPackage, 1, Int::plus)

            val processed = processedTestClassesAmountByPackage[currentPackage]!!
            val total = testClassesAmountByPackage[currentPackage]!!
            return processed == total
        }
    }
}