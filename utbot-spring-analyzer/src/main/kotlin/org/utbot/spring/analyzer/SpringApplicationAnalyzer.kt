package org.utbot.spring.analyzer

import org.utbot.spring.context.InstantiationContext
import org.utbot.spring.instantiator.SpringApplicationInstantiatorFacade
import org.utbot.spring.api.ApplicationData
import org.utbot.spring.exception.UtBotSpringShutdownException
import org.utbot.spring.generated.BeanDefinitionData
import org.utbot.spring.utils.SourceFinder

class SpringApplicationAnalyzer {

    fun getBeanDefinitions(applicationData: ApplicationData): Array<BeanDefinitionData> {
        val configurationClasses = SourceFinder(applicationData).findSources()
        val instantiationContext = InstantiationContext(
            configurationClasses,
            applicationData.profileExpression,
        )

        return UtBotSpringShutdownException
            .catch { SpringApplicationInstantiatorFacade(instantiationContext).instantiate() }
            .beanDefinitions
            .toTypedArray()
    }
}