package org.utbot.spring.analyzer

import org.utbot.spring.api.instantiator.InstantiationSettings
import org.utbot.spring.api.ApplicationData
import org.utbot.spring.api.instantiator.SpringApiProviderFacade
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

        return SpringApiProviderFacade.getInstance(this::class.java.classLoader)
            .useMostSpecificNonFailingApi(instantiationSettings) { springApi ->
                UtBotSpringShutdownException
                    .catch { springApi.getOrLoadSpringApplicationContext() }
                    .beanDefinitions
                    .toTypedArray()
            }
    }
}