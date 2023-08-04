package org.utbot.testing

import org.junit.jupiter.api.AfterAll
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.plugin.api.*
import org.utbot.taint.TaintConfigurationProvider
import kotlin.reflect.KClass

open class UtValueTestCaseCheckerForTaint(
    testClass: KClass<*>,
    testCodeGeneration: Boolean = true,
    pipelines: List<Configuration> = listOf(
        Configuration(CodegenLanguage.JAVA, ParametrizedTestSource.DO_NOT_PARAMETRIZE, Compilation),
        Configuration(CodegenLanguage.JAVA, ParametrizedTestSource.PARAMETRIZE, Compilation),
        Configuration(CodegenLanguage.KOTLIN, ParametrizedTestSource.DO_NOT_PARAMETRIZE, CodeGeneration),
    ),
    private val taintConfigurationProvider: TaintConfigurationProvider,
) : UtValueTestCaseChecker(testClass, testCodeGeneration, pipelines) {

    init {
        UtSettings.useTaintAnalysis = true
    }

    override fun createTestCaseGenerator(buildInfo: CodeGenerationIntegrationTest.Companion.BuildInfo) =
        TestSpecificTestCaseGenerator(
            buildInfo.buildDir,
            buildInfo.dependencyPath,
            System.getProperty("java.class.path"),
            taintConfigurationProvider = taintConfigurationProvider,
        )

    @AfterAll
    fun reset() {
        UtSettings.useTaintAnalysis = false
    }
}