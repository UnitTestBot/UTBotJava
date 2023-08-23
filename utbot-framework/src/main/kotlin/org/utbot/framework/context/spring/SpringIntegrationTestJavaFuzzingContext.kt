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
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.SpringModelUtils
import org.utbot.framework.plugin.api.util.allDeclaredFieldIds
import org.utbot.framework.plugin.api.util.jField
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.providers.AnyDepthNullValueProvider
import org.utbot.fuzzing.providers.ModifyingWithMethodsProviderWrapper
import org.utbot.fuzzing.providers.anyObjectValueProvider
import org.utbot.fuzzing.spring.GeneratedFieldValueProvider
import org.utbot.fuzzing.spring.SpringBeanValueProvider
import org.utbot.fuzzing.spring.preserveProperties
import org.utbot.fuzzing.spring.valid.EmailValueProvider
import org.utbot.fuzzing.spring.valid.NotBlankStringValueProvider
import org.utbot.fuzzing.spring.valid.NotEmptyStringValueProvider
import org.utbot.fuzzing.spring.valid.ValidEntityValueProvider

class SpringIntegrationTestJavaFuzzingContext(
    val delegateContext: JavaFuzzingContext,
    relevantRepositories: Set<SpringRepositoryId>,
    springApplicationContext: SpringApplicationContext,
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
        springBeanValueProvider
            .with(ModifyingWithMethodsProviderWrapper(classUnderTest, springBeanValueProvider))
            .withFallback(ValidEntityValueProvider(idGenerator, onlyAcceptWhenValidIsRequired = true))
            .withFallback(EmailValueProvider())
            .withFallback(NotBlankStringValueProvider())
            .withFallback(NotEmptyStringValueProvider())
            .withFallback(
                delegateContext.valueProvider
                    .withProviderAndModifyingMethodsBuddy(anyObjectValueProvider(idGenerator))
                    .withProviderAndModifyingMethodsBuddy(ValidEntityValueProvider(idGenerator, onlyAcceptWhenValidIsRequired = false))
                    .with(createGeneratedFieldValueProviders(relevantRepositories, idGenerator))
                    .withFallback(AnyDepthNullValueProvider)
            )
            .preserveProperties()

    private fun JavaValueProvider.withProviderAndModifyingMethodsBuddy(provider: JavaValueProvider): JavaValueProvider =
        this.with(provider)
            .with(ModifyingWithMethodsProviderWrapper(classUnderTest, this))

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
    ): EnvironmentModels {
        val delegateStateBefore = delegateContext.createStateBefore(thisInstance, parameters, statics, executableToCall)
        return when (executableToCall) {
            is ConstructorId -> delegateStateBefore
            is MethodId -> {
                val requestBuilderModel = SpringModelUtils.createRequestBuilderModelOrNull(
                    methodId = executableToCall,
                    arguments = parameters,
                    idGenerator = { idGenerator.createId() }
                ) ?: return delegateStateBefore
                delegateStateBefore.copy(
                    thisInstance = SpringModelUtils.createMockMvcModel { idGenerator.createId() },
                    parameters = listOf(requestBuilderModel),
                    executableToCall = SpringModelUtils.mockMvcPerformMethodId,
                )
            }
        }
    }
}