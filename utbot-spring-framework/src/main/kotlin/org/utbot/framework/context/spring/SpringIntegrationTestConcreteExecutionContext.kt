package org.utbot.framework.context.spring

import mu.KotlinLogging
import org.utbot.framework.context.ConcreteExecutionContext
import org.utbot.framework.context.ConcreteExecutionContext.FuzzingContextParams
import org.utbot.framework.context.JavaFuzzingContext
import org.utbot.framework.plugin.api.ConcreteContextLoadingResult
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.SpringSettings
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.isSuccess
import org.utbot.framework.plugin.api.util.SpringModelUtils
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.getRelevantSpringRepositories
import org.utbot.instrumentation.instrumentation.execution.RemovingConstructFailsUtExecutionInstrumentation
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult
import org.utbot.instrumentation.instrumentation.execution.UtExecutionInstrumentation
import org.utbot.instrumentation.instrumentation.spring.SpringUtExecutionInstrumentation
import org.utbot.instrumentation.tryLoadingSpringContext
import java.io.File

class SpringIntegrationTestConcreteExecutionContext(
    private val delegateContext: ConcreteExecutionContext,
    classpathWithoutDependencies: String,
    private val springApplicationContext: SpringApplicationContext,
) : ConcreteExecutionContext by delegateContext {
    private val springSettings = (springApplicationContext.springSettings as? SpringSettings.PresentSpringSettings) ?:
        error("Integration tests cannot be generated without Spring configuration")

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override val instrumentationFactory: UtExecutionInstrumentation.Factory<*> =
        RemovingConstructFailsUtExecutionInstrumentation.Factory(
            SpringUtExecutionInstrumentation.Factory(
                delegateContext.instrumentationFactory,
                springSettings,
                springApplicationContext.beanDefinitions,
                buildDirs = classpathWithoutDependencies.split(File.pathSeparator)
                    .map { File(it).toURI().toURL() }
                    .toTypedArray(),
            )
        )

    override fun loadContext(
        concreteExecutor: ConcreteExecutor<UtConcreteExecutionResult, UtExecutionInstrumentation>,
    ): ConcreteContextLoadingResult =
        delegateContext.loadContext(concreteExecutor).andThen {
            springApplicationContext.concreteContextLoadingResult ?: concreteExecutor.tryLoadingSpringContext().also {
                springApplicationContext.concreteContextLoadingResult = it
            }
        }

    override fun tryCreateFuzzingContext(params: FuzzingContextParams): JavaFuzzingContext {
        if (springApplicationContext.getBeansAssignableTo(params.classUnderTest).isEmpty())
            error(
                "No beans of type ${params.classUnderTest} are found. " +
                        "Try choosing different Spring configuration or adding beans to " +
                        springSettings.configuration.fullDisplayName
            )

        val relevantRepositories = params.concreteExecutor.getRelevantSpringRepositories(params.classUnderTest)
        logger.info { "Detected relevant repositories for class ${params.classUnderTest}: $relevantRepositories" }

        return SpringIntegrationTestJavaFuzzingContext(
            delegateContext = delegateContext.tryCreateFuzzingContext(params),
            relevantRepositories = relevantRepositories,
            springApplicationContext = springApplicationContext,
            fuzzingStartTimeMillis = params.fuzzingStartTimeMillis,
            fuzzingEndTimeMillis = params.fuzzingEndTimeMillis,
        )
    }

    override fun transformExecutionsBeforeMinimization(
        executions: List<UtExecution>,
        methodUnderTest: ExecutableId
    ): List<UtExecution> {
        val (mockMvcExecutions, regularExecutions) =
            delegateContext.transformExecutionsBeforeMinimization(executions, methodUnderTest)
                .partition { it.executableToCall == SpringModelUtils.mockMvcPerformMethodId }

        val classUnderTestName = methodUnderTest.classId.name
        val methodUnderTestSignature = methodUnderTest.signature

        fun UtExecution.getMethodUnderTestCoverage(): List<Long>? =
            coverage?.coveredInstructions?.filter {
                it.className == classUnderTestName && it.methodSignature == methodUnderTestSignature
            }?.map { it.id }

        fun UtExecution.getKey() = getMethodUnderTestCoverage()?.let { it to result.isSuccess }

        val mockMvcExecutionKeys = mockMvcExecutions.mapNotNullTo(mutableSetOf()) { it.getKey() }

        return mockMvcExecutions + regularExecutions.filter { it.getKey() !in mockMvcExecutionKeys }
    }
}