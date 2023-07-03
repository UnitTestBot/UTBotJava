package org.utbot.instrumentation.instrumentation.execution.context

import org.utbot.framework.plugin.api.UtConcreteValue
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtSpringContextModel
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.spring.api.context.ContextWrapper
import org.utbot.spring.api.instantiator.ApplicationInstantiatorFacade
import org.utbot.spring.api.instantiator.InstantiationSettings

class SpringInstrumentationContext(
    private val springConfig: String,
    private val delegateInstrumentationContext: InstrumentationContext,
) : InstrumentationContext by delegateInstrumentationContext {
    // TODO: recreate context/app every time whenever we change method under test
    val springContext: ContextWrapper by lazy {
        val classLoader = utContext.classLoader
        Thread.currentThread().contextClassLoader = classLoader

        val instantiationSettings = InstantiationSettings(
            configurationClasses = arrayOf(
                classLoader.loadClass(springConfig),
            ),
            profileExpression = null, // TODO pass profile expression here
        )

        val springFacadeInstance =  classLoader
            .loadClass("org.utbot.spring.instantiator.SpringApplicationInstantiatorFacade")
            .getConstructor()
            .newInstance() as ApplicationInstantiatorFacade

        springFacadeInstance.instantiate(instantiationSettings)
    }

    override fun constructContextDependentValue(model: UtModel): UtConcreteValue<*>? = when (model) {
        is UtSpringContextModel -> UtConcreteValue(springContext.context)
        else -> delegateInstrumentationContext.constructContextDependentValue(model)
    }
}