package org.utbot.framework.context.spring

import mu.KotlinLogging
import org.utbot.common.tryLoadClass
import org.utbot.framework.context.JavaFuzzingContext
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.SpringRepositoryId
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtSpringMockMvcResultActionsModel
import org.utbot.framework.plugin.api.util.SpringModelUtils
import org.utbot.framework.plugin.api.util.SpringModelUtils.allControllerParametersAreSupported
import org.utbot.framework.plugin.api.util.allDeclaredFieldIds
import org.utbot.framework.plugin.api.util.jField
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.providers.AnyDepthNullValueProvider
import org.utbot.fuzzing.spring.decorators.ModifyingWithMethodsProviderWrapper
import org.utbot.fuzzing.providers.ObjectValueProvider
import org.utbot.fuzzing.spring.GeneratedFieldValueProvider
import org.utbot.fuzzing.spring.SpringBeanValueProvider
import org.utbot.fuzzing.spring.decorators.preserveProperties
import org.utbot.fuzzing.spring.valid.EmailValueProvider
import org.utbot.fuzzing.spring.valid.NotBlankStringValueProvider
import org.utbot.fuzzing.spring.valid.NotEmptyStringValueProvider
import org.utbot.fuzzing.spring.valid.ValidEntityValueProvider
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult

class SpringIntegrationTestJavaFuzzingContext(
    private val delegateContext: JavaFuzzingContext,
    relevantRepositories: Set<SpringRepositoryId>,
    springApplicationContext: SpringApplicationContext,
    private val fuzzingStartTimeMillis: Long,
    private val fuzzingEndTimeMillis: Long,
) : JavaFuzzingContext by delegateContext {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val springBeanValueProvider: JavaValueProvider =
        SpringBeanValueProvider(
            idGenerator,
            beanNameProvider = { classId ->
                springApplicationContext.getBeansAssignableTo(classId).map { it.beanName }
            },
            relevantRepositories = relevantRepositories
        )

    override val valueProvider: JavaValueProvider =
        springBeanValueProvider.withModifyingMethodsBuddy()
            .withFallback(ValidEntityValueProvider(idGenerator, onlyAcceptWhenValidIsRequired = true).withModifyingMethodsBuddy())
            .withFallback(EmailValueProvider())
            .withFallback(NotBlankStringValueProvider())
            .withFallback(NotEmptyStringValueProvider())
            .withFallback(
                delegateContext.valueProvider
                    .with(ObjectValueProvider(idGenerator).withModifyingMethodsBuddy())
                    .with(ValidEntityValueProvider(idGenerator, onlyAcceptWhenValidIsRequired = false).withModifyingMethodsBuddy())
                    .with(createGeneratedFieldValueProviders(relevantRepositories, idGenerator))
                    .withFallback(AnyDepthNullValueProvider)
            )
            .preserveProperties()

    private fun JavaValueProvider.withModifyingMethodsBuddy(): JavaValueProvider =
        with(modifyingMethodsBuddy(this))

    private fun modifyingMethodsBuddy(provider: JavaValueProvider): JavaValueProvider =
        ModifyingWithMethodsProviderWrapper(classUnderTest, provider)


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

    private val methodsSuccessfullyCalledViaMockMvc = mutableSetOf<ExecutableId>()

    override fun createStateBefore(
        thisInstance: UtModel?,
        parameters: List<UtModel>,
        statics: Map<FieldId, UtModel>,
        executableToCall: ExecutableId
    ): EnvironmentModels {
        val delegateStateBefore = delegateContext.createStateBefore(thisInstance, parameters, statics, executableToCall)
        return when (executableToCall) {
            is ConstructorId -> delegateStateBefore
            is MethodId -> {
                val requestBuilderModel = SpringModelUtils.createRequestBuilderModelOrNull(
                    methodId = executableToCall,
                    arguments = parameters,
                    idGenerator = { idGenerator.createId() }
                )?.takeIf {
                    val halfOfFuzzingTimePassed = System.currentTimeMillis() > (fuzzingStartTimeMillis + fuzzingEndTimeMillis) / 2
                    val allParamsSupported = allControllerParametersAreSupported(executableToCall)
                    val successfullyCalledViaMockMvc = executableToCall in methodsSuccessfullyCalledViaMockMvc
                    !halfOfFuzzingTimePassed || allParamsSupported && successfullyCalledViaMockMvc
                } ?: return delegateStateBefore

                delegateStateBefore.copy(
                    thisInstance = SpringModelUtils.createMockMvcModel(controller = thisInstance) { idGenerator.createId() },
                    parameters = listOf(requestBuilderModel),
                    executableToCall = SpringModelUtils.mockMvcPerformMethodId,
                )
            }
        }
    }

    override fun handleFuzzedConcreteExecutionResult(
        methodUnderTest: ExecutableId,
        concreteExecutionResult: UtConcreteExecutionResult
    ) {
        delegateContext.handleFuzzedConcreteExecutionResult(methodUnderTest, concreteExecutionResult)
        ((concreteExecutionResult.result as? UtExecutionSuccess)?.model as? UtSpringMockMvcResultActionsModel)?.let {
            if (it.status < 400)
                methodsSuccessfullyCalledViaMockMvc.add(methodUnderTest)
        }
    }
}