package org.utbot.framework.context.spring

import mu.KotlinLogging
import org.utbot.common.tryLoadClass
import org.utbot.framework.context.ConcreteExecutionContext
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConcreteContextLoadingResult
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.SpringRepositoryId
import org.utbot.framework.plugin.api.SpringSettings
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.SpringModelUtils
import org.utbot.framework.plugin.api.util.SpringModelUtils.createMockMvcModel
import org.utbot.framework.plugin.api.util.SpringModelUtils.createRequestBuilderModelOrNull
import org.utbot.framework.plugin.api.util.SpringModelUtils.mockMvcPerformMethodId
import org.utbot.framework.plugin.api.util.allDeclaredFieldIds
import org.utbot.framework.plugin.api.util.jField
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.providers.AnyDepthNullValueProvider
import org.utbot.fuzzing.spring.GeneratedFieldValueProvider
import org.utbot.fuzzing.spring.SpringBeanValueProvider
import org.utbot.fuzzing.spring.preserveProperties
import org.utbot.fuzzing.spring.valid.EmailValueProvider
import org.utbot.fuzzing.spring.valid.NotBlankStringValueProvider
import org.utbot.fuzzing.spring.valid.NotEmptyStringValueProvider
import org.utbot.fuzzing.spring.valid.ValidEntityValueProvider
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

        val springBeanValueProvider = SpringBeanValueProvider(
            idGenerator,
            beanNameProvider = { classId ->
                springApplicationContext.getBeansAssignableTo(classId).map { it.beanName }
            },
            relevantRepositories = relevantRepositories
        )

        return springBeanValueProvider
            .withFallback(ValidEntityValueProvider(idGenerator, onlyAcceptWhenValidIsRequired = true))
            .withFallback(EmailValueProvider())
            .withFallback(NotBlankStringValueProvider())
            .withFallback(NotEmptyStringValueProvider())
            .withFallback(
                delegateContext.tryCreateValueProvider(concreteExecutor, classUnderTest, idGenerator)
                    .with(ValidEntityValueProvider(idGenerator, onlyAcceptWhenValidIsRequired = false))
                    .with(createGeneratedFieldValueProviders(relevantRepositories, idGenerator))
                    .withFallback(AnyDepthNullValueProvider)
            )
            .preserveProperties()
    }

    private fun createGeneratedFieldValueProviders(
        relevantRepositories: Set<SpringRepositoryId>,
        idGenerator: IdentityPreservingIdGenerator<Int>
    ): JavaValueProvider {
        val generatedValueAnnotationClasses = SpringModelUtils.generatedValueClassIds.mapNotNull {
            @Suppress("UNCHECKED_CAST") // type system fails to understand that @GeneratedValue is indeed an annotation
            utContext.classLoader.tryLoadClass(it.name) as Class<out Annotation>?
        }

        val generatedValueFields =
            relevantRepositories
                .flatMap { springRepositoryId ->
                    val entityClassId = springRepositoryId.entityClassId
                    entityClassId.allDeclaredFieldIds
                        .filter { fieldId -> generatedValueAnnotationClasses.any { fieldId.jField.isAnnotationPresent(it) } }
                        .map { entityClassId to it }
                }

        logger.info { "Detected @GeneratedValue fields: $generatedValueFields" }

        return ValueProvider.of(generatedValueFields.map { (entityClassId, fieldId) ->
            GeneratedFieldValueProvider(idGenerator, entityClassId, fieldId)
        })
    }

    override fun createStateBefore(
        thisInstance: UtModel?,
        parameters: List<UtModel>,
        statics: Map<FieldId, UtModel>,
        executableToCall: ExecutableId,
        idGenerator: IdGenerator<Int>
    ): EnvironmentModels {
        val delegateStateBefore = delegateContext.createStateBefore(thisInstance, parameters, statics, executableToCall, idGenerator)
        return when (executableToCall) {
            is ConstructorId -> delegateStateBefore
            is MethodId -> {
                val requestBuilderModel = createRequestBuilderModelOrNull(
                    methodId = executableToCall,
                    arguments = parameters,
                    idGenerator = { idGenerator.createId() }
                ) ?: return delegateStateBefore
                delegateStateBefore.copy(
                    thisInstance = createMockMvcModel { idGenerator.createId() },
                    parameters = listOf(requestBuilderModel),
                    executableToCall = mockMvcPerformMethodId,
                )
            }
        }
    }
}