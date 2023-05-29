package org.utbot.spring.analyzer

import org.utbot.framework.plugin.api.util.utContext
import org.utbot.spring.api.instantiator.InstantiationSettings
import org.utbot.spring.api.ApplicationData
import org.utbot.spring.api.instantiator.ApplicationInstantiatorFacade
import org.utbot.spring.exception.UtBotSpringShutdownException
import org.utbot.spring.generated.BeanDefinitionData
import org.utbot.spring.utils.SourceFinder

class SpringApplicationAnalyzer {

    fun getBeanDefinitions(applicationData: ApplicationData): Array<BeanDefinitionData> {
        val configurationClasses = SourceFinder(applicationData).findSources()
        val instantiationSettings = InstantiationSettings(
            configurationClasses,
            applicationData.profileExpression,
        )

        val springFacadeInstance =  utContext.classLoader
            .loadClass("org.utbot.spring.api.instantiator.SpringApplicationInstantiatorFacade")
            .getDeclaredConstructor()
            .newInstance()
        springFacadeInstance as ApplicationInstantiatorFacade

        return UtBotSpringShutdownException
            .catch { springFacadeInstance.instantiate(instantiationSettings) }
            .beanDefinitions
            .toTypedArray()
    }
}