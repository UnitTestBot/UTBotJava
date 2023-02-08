package org.utbot.external.api

import org.utbot.common.FileUtil
import org.utbot.engine.StateListener
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.TestCaseGenerator
import org.utbot.framework.plugin.api.testFlow
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.services.JdkInfoDefaultProvider

object UnitTestBotLight {
    @JvmStatic
    @JvmOverloads
    fun run (
        stateListener: StateListener,
        methodForAutomaticGeneration: TestMethodInfo,
        classUnderTest: Class<*>,
        classpath: String,
        dependencyClassPath: String,
        mockStrategyApi: MockStrategyApi = MockStrategyApi.OTHER_PACKAGES,
    ) {

        val buildPath = FileUtil.isolateClassFiles(classUnderTest).toPath()

        TestCaseGenerator(listOf(buildPath), classpath, dependencyClassPath, jdkInfo = JdkInfoDefaultProvider().info)
            .generate(
                listOf(methodForAutomaticGeneration.methodToBeTestedFromUserInput.executableId),
                mockStrategyApi,
                chosenClassesToMockAlways = emptySet(),
                UtSettings.utBotGenerationTimeoutInMillis,
                generate = testFlow {
                    engine.addListener(stateListener)
                    generationTimeout = UtSettings.utBotGenerationTimeoutInMillis
                    isSymbolicEngineEnabled = true
                })
    }
}