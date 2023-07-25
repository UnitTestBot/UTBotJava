package org.utbot.external.api

import org.utbot.engine.EngineController
import org.utbot.engine.ExecutionStateListener
import org.utbot.engine.MockStrategy
import org.utbot.engine.UtBotSymbolicEngine
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.ApplicationContext
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.framework.plugin.services.JdkInfoDefaultProvider
import org.utbot.framework.util.SootUtils.runSoot
import org.utbot.instrumentation.instrumentation.execution.UtExecutionInstrumentation
import java.io.File
import java.nio.file.Paths


@Suppress("unused")
object UnitTestBotLight {

    /**
     * Creates an instance of symbolic engine with default values
     * w/o specific objects like
     * - [ApplicationContext]
     * - [UtExecutionInstrumentation]
     * - [EngineController]
     * - etc.
     *
     * @return symbolic engine instance
     */
    @JvmStatic
    fun javaUtBotSymbolicEngine(
        methodUnderTest: ExecutableId,
        classpath: String,
        mockStrategy: MockStrategy,
        chosenClassesToMockAlways: Set<ClassId>,
    ) = UtBotSymbolicEngine(
        controller = EngineController(),
        methodUnderTest = methodUnderTest,
        classpath = classpath,
        dependencyPaths = "",
        mockStrategy = mockStrategy,
        chosenClassesToMockAlways = chosenClassesToMockAlways,
        applicationContext = ApplicationContext(),
        executionInstrumentation = UtExecutionInstrumentation,
        solverTimeoutInMillis = UtSettings.checkSolverTimeoutMillis
    )

    @JvmStatic
    @JvmOverloads
    fun run (
        stateListener: ExecutionStateListener,
        methodForAutomaticGeneration: TestMethodInfo,
        classpath: String,
        dependencyClassPath: String,
        mockStrategyApi: MockStrategyApi = MockStrategyApi.OTHER_PACKAGES,
    ) {
        // init Soot if it is not yet
        runSoot(classpath.split(File.pathSeparator).map(Paths::get), classpath, false, JdkInfoDefaultProvider().info)
        val classLoader = Thread.currentThread().contextClassLoader
        val utContext = UtContext(classLoader)
        withUtContext(utContext) {
            UtBotSymbolicEngine(
                EngineController(),
                methodForAutomaticGeneration.methodToBeTestedFromUserInput.executableId,
                classpath,
                dependencyClassPath,
                when (mockStrategyApi) {
                    MockStrategyApi.NO_MOCKS -> MockStrategy.NO_MOCKS
                    MockStrategyApi.OTHER_PACKAGES -> MockStrategy.OTHER_PACKAGES
                    MockStrategyApi.OTHER_CLASSES -> MockStrategy.OTHER_CLASSES
                },
                HashSet(),
                ApplicationContext(),
                UtExecutionInstrumentation
            ).addListener(stateListener).traverseAll()
        }
    }
}