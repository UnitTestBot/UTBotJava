package org.utbot.instrumentation.instrumentation.spring

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.SpringSettings.*
import org.utbot.framework.plugin.api.SpringConfiguration.*
import org.utbot.framework.plugin.api.UtConcreteValue
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtSpringContextModel
import org.utbot.framework.plugin.api.UtSpringEntityManagerModel
import org.utbot.framework.plugin.api.util.SpringModelUtils.resultActionsClassId
import org.utbot.framework.plugin.api.util.isSubtypeOf
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.instrumentation.instrumentation.execution.constructors.UtModelWithCompositeOriginConstructor
import org.utbot.instrumentation.instrumentation.execution.context.InstrumentationContext
import org.utbot.spring.api.SpringApi
import org.utbot.spring.api.provider.SpringApiProviderFacade
import org.utbot.spring.api.provider.InstantiationSettings

class SpringInstrumentationContext(
    private val springSettings: PresentSpringSettings,
    private val delegateInstrumentationContext: InstrumentationContext,
) : InstrumentationContext by delegateInstrumentationContext {
    // TODO: recreate context/app every time whenever we change method under test
    val springApiProviderResult: SpringApiProviderFacade.ProviderResult<SpringApi> by lazy {
        val classLoader = utContext.classLoader
        Thread.currentThread().contextClassLoader = classLoader

        val instantiationSettings = InstantiationSettings(
            configurationClasses = arrayOf(
                // TODO: for now we prohibit generating integration tests with XML configuration supplied,
                //  so we expect JavaConfigurations only.
                //  After fix rewrite the following.
                classLoader.loadClass(
                    (springSettings.configuration as? JavaBasedConfiguration)?.configBinaryName
                        ?: error("JavaConfiguration was expected, but ${springSettings.configuration.javaClass.name} was provided.")
                )
            ),
            profiles = springSettings.profiles.toTypedArray(),
        )

        SpringApiProviderFacade
            .getInstance(classLoader)
            .provideMostSpecificAvailableApi(instantiationSettings)
    }

    val springApi get() = springApiProviderResult.result.getOrThrow()

    override fun constructContextDependentValue(model: UtModel): UtConcreteValue<*>? = when (model) {
        is UtSpringContextModel -> UtConcreteValue(springApi.getOrLoadSpringApplicationContext())
        is UtSpringEntityManagerModel -> UtConcreteValue(springApi.getEntityManager())
        else -> delegateInstrumentationContext.constructContextDependentValue(model)
    }

    override fun findUtModelWithCompositeOriginConstructor(classId: ClassId): UtModelWithCompositeOriginConstructor? =
        if (classId.isSubtypeOf(resultActionsClassId)) UtMockMvcResultActionsModelConstructor()
        else delegateInstrumentationContext.findUtModelWithCompositeOriginConstructor(classId)
}