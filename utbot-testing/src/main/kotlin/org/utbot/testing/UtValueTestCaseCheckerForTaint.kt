package org.utbot.testing

import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.*
import org.utbot.taint.TaintConfigurationProvider
import kotlin.reflect.KClass

open class UtValueTestCaseCheckerForTaint(
    testClass: KClass<*>,
    testCodeGeneration: Boolean = true,
    pipelines: List<TestLastStage> = listOf(
        TestLastStage(CodegenLanguage.JAVA),
        TestLastStage(CodegenLanguage.KOTLIN)
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
}