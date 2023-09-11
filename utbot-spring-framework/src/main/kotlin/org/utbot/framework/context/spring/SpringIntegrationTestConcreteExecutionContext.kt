package org.utbot.framework.context.spring

import mu.KotlinLogging
import org.utbot.framework.context.ConcreteExecutionContext
import org.utbot.framework.context.JavaFuzzingContext
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConcreteContextLoadingResult
import org.utbot.framework.plugin.api.SpringSettings
import org.utbot.fuzzer.IdentityPreservingIdGenerator
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

    override fun tryCreateFuzzingContext(
        concreteExecutor: ConcreteExecutor<UtConcreteExecutionResult, UtExecutionInstrumentation>,
        classUnderTest: ClassId,
        idGenerator: IdentityPreservingIdGenerator<Int>
    ): JavaFuzzingContext {
        if (springApplicationContext.getBeansAssignableTo(classUnderTest).isEmpty())
            error(
                "No beans of type ${classUnderTest.name} are found. " +
                        "Try choosing different Spring configuration or adding beans to " +
                        springSettings.configuration.fullDisplayName
            )

        val relevantRepositories = concreteExecutor.getRelevantSpringRepositories(classUnderTest)
        logger.info { "Detected relevant repositories for class $classUnderTest: $relevantRepositories" }

        return SpringIntegrationTestJavaFuzzingContext(
            delegateContext = delegateContext.tryCreateFuzzingContext(concreteExecutor, classUnderTest, idGenerator),
            relevantRepositories = relevantRepositories,
            springApplicationContext = springApplicationContext,
        )
    }
}