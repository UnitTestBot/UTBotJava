package org.utbot.instrumentation.instrumentation.execution.context

import org.utbot.framework.plugin.api.SpringSettings.*
import org.utbot.framework.plugin.api.SpringConfiguration.*
import org.utbot.framework.plugin.api.UtConcreteValue
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtSpringContextModel
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.spring.api.SpringApi
import org.utbot.spring.api.instantiator.SpringApiProviderFacade
import org.utbot.spring.api.instantiator.InstantiationSettings

class SpringInstrumentationContext(
    private val springSettings: PresentSpringSettings,
    private val delegateInstrumentationContext: InstrumentationContext,
) : InstrumentationContext by delegateInstrumentationContext {
    // TODO: recreate context/app every time whenever we change method under test
    val springApi: SpringApi by lazy {
        val classLoader = utContext.classLoader
        Thread.currentThread().contextClassLoader = classLoader

        val instantiationSettings = InstantiationSettings(
            configurationClasses = arrayOf(
                // TODO: for now we prohibit generating integration tests with XML configuration supplied,
                //  so we expect JavaConfigurations only.
                //  After fix rewrite the following.
                classLoader.loadClass(
                    (springSettings.configuration as? JavaConfiguration)?.classBinaryName
                        ?: error("JavaConfiguration was expected, but ${springSettings.configuration.javaClass.name} was provided.")
                )
            ),
            profiles = springSettings.profiles,
        )

        SpringApiProviderFacade
            .getInstance(classLoader)
            .provideMostSpecificAvailableApi(instantiationSettings)
    }

    override fun constructContextDependentValue(model: UtModel): UtConcreteValue<*>? = when (model) {
        is UtSpringContextModel -> UtConcreteValue(springApi.getOrLoadSpringApplicationContext())
        else -> delegateInstrumentationContext.constructContextDependentValue(model)
    }
}