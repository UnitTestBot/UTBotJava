package org.utbot.instrumentation.instrumentation.execution.mock

import org.utbot.framework.plugin.api.util.utContext
import org.utbot.spring.api.context.ContextWrapper
import org.utbot.spring.api.instantiator.ApplicationInstantiatorFacade
import org.utbot.spring.api.instantiator.InstantiationSettings

class SpringInstrumentationContext(private val springConfig: String) : InstrumentationContext() {
    // TODO: recreate context/app every time whenever we change method under test
    val springContext: ContextWrapper by lazy {
        val classLoader = utContext.classLoader
        Thread.currentThread().contextClassLoader = classLoader

        val instantiationSettings = InstantiationSettings(
            configurationClasses = arrayOf(
                classLoader.loadClass(springConfig),
                classLoader.loadClass("org.utbot.spring.repositoryWrapper.RepositoryWrapperConfiguration")
            ),
            profileExpression = null, // TODO pass profile expression here
        )

        val springFacadeInstance =  classLoader
            .loadClass("org.utbot.spring.instantiator.SpringApplicationInstantiatorFacade")
            .getConstructor()
            .newInstance() as ApplicationInstantiatorFacade

        springFacadeInstance.instantiate(instantiationSettings)
    }
}