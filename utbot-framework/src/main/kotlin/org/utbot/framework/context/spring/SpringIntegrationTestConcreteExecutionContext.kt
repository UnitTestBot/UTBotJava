package org.utbot.framework.context.spring

import mu.KotlinLogging
import org.utbot.common.tryLoadClass
import org.utbot.framework.context.ConcreteExecutionContext
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConcreteContextLoadingResult
import org.utbot.framework.plugin.api.SpringRepositoryId
import org.utbot.framework.plugin.api.SpringSettings
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.util.SpringModelUtils
import org.utbot.framework.plugin.api.util.allDeclaredFieldIds
import org.utbot.framework.plugin.api.util.jField
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.providers.AnyDepthNullValueProvider
import org.utbot.fuzzing.providers.FieldValueProvider
import org.utbot.fuzzing.providers.ObjectValueProvider
import org.utbot.fuzzing.providers.anyObjectValueProvider
import org.utbot.fuzzing.spring.SavedEntityValueProvider
import org.utbot.fuzzing.spring.SpringBeanValueProvider
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.getRelevantSpringRepositories
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult
import org.utbot.instrumentation.instrumentation.execution.UtExecutionInstrumentation
import org.utbot.instrumentation.instrumentation.spring.SpringUtExecutionInstrumentation
import org.utbot.instrumentation.tryLoadingSpringContext
import java.io.File

class SpringIntegrationTestConcreteExecutionContext(
    private val delegateContext: ConcreteExecutionContext,
    classpathWithoutDependencies: String,
    private val springApplicationContext: SpringApplicationContext,
) : ConcreteExecutionContext {
    private val springSettings = (springApplicationContext.springSettings as? SpringSettings.PresentSpringSettings) ?:
        error("Integration tests cannot be generated without Spring configuration")

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override val instrumentationFactory: UtExecutionInstrumentation.Factory<*> =
        SpringUtExecutionInstrumentation.Factory(
            delegateContext.instrumentationFactory,
            springSettings,
            springApplicationContext.beanDefinitions,
            buildDirs = classpathWithoutDependencies.split(File.pathSeparator)
                .map { File(it).toURI().toURL() }
                .toTypedArray(),
        )

    override fun loadContext(
        concreteExecutor: ConcreteExecutor<UtConcreteExecutionResult, UtExecutionInstrumentation>,
    ): ConcreteContextLoadingResult =
        delegateContext.loadContext(concreteExecutor).andThen {
            springApplicationContext.concreteContextLoadingResult ?: concreteExecutor.tryLoadingSpringContext().also {
                springApplicationContext.concreteContextLoadingResult = it
            }
        }

    override fun transformExecutionsBeforeMinimization(
        executions: List<UtExecution>,
        classUnderTestId: ClassId
    ): List<UtExecution> = delegateContext.transformExecutionsBeforeMinimization(executions, classUnderTestId)

    override fun tryCreateValueProvider(
        concreteExecutor: ConcreteExecutor<UtConcreteExecutionResult, UtExecutionInstrumentation>,
        classUnderTest: ClassId,
        idGenerator: IdentityPreservingIdGenerator<Int>
    ): JavaValueProvider {
        if (springApplicationContext.getBeansAssignableTo(classUnderTest).isEmpty())
            error(
                "No beans of type ${classUnderTest.name} are found. " +
                        "Try choosing different Spring configuration or adding beans to " +
                        springSettings.configuration.fullDisplayName
            )

        val relevantRepositories = concreteExecutor.getRelevantSpringRepositories(classUnderTest)
        logger.info { "Detected relevant repositories for class $classUnderTest: $relevantRepositories" }

        // spring should try to generate bean values, but if it fails, then object value provider is used for it
        val springBeanValueProvider = SpringBeanValueProvider(
            idGenerator,
            beanNameProvider = { classId ->
                springApplicationContext.getBeansAssignableTo(classId).map { it.beanName }
            },
            relevantRepositories = relevantRepositories
        ).withFallback(anyObjectValueProvider(idGenerator))

        return delegateContext.tryCreateValueProvider(concreteExecutor, classUnderTest, idGenerator)
            .except { p -> p is ObjectValueProvider }
            .with(springBeanValueProvider)
            .with(createSavedEntityValueProviders(relevantRepositories, idGenerator))
            .with(createFieldValueProviders(relevantRepositories, idGenerator))
            .withFallback(AnyDepthNullValueProvider)
    }

    private fun createSavedEntityValueProviders(
        relevantRepositories: Set<SpringRepositoryId>,
        idGenerator: IdentityPreservingIdGenerator<Int>
    ) = ValueProvider.of(relevantRepositories.map { SavedEntityValueProvider(idGenerator, it) })

    private fun createFieldValueProviders(
        relevantRepositories: Set<SpringRepositoryId>,
        idGenerator: IdentityPreservingIdGenerator<Int>
    ): JavaValueProvider {
        val generatedValueAnnotationClasses = SpringModelUtils.generatedValueClassIds.mapNotNull {
            @Suppress("UNCHECKED_CAST") // type system fails to understand that @GeneratedValue is indeed an annotation
            utContext.classLoader.tryLoadClass(it.name) as Class<out Annotation>?
        }

        val generatedValueFieldIds =
            relevantRepositories
                .flatMap { it.entityClassId.allDeclaredFieldIds }
                .filter { fieldId -> generatedValueAnnotationClasses.any { fieldId.jField.isAnnotationPresent(it) } }
        logger.info { "Detected @GeneratedValue fields: $generatedValueFieldIds" }

        return ValueProvider.of(generatedValueFieldIds.map { FieldValueProvider(idGenerator, it) })
    }
}