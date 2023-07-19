package org.utbot.spring.analyzer

import org.utbot.spring.api.provider.InstantiationSettings
import org.utbot.spring.api.ApplicationData
import org.utbot.spring.api.provider.SpringApiProviderFacade
import org.utbot.spring.exception.UtBotSpringShutdownException
import org.utbot.spring.generated.BeanDefinitionData
import org.utbot.spring.utils.SourceFinder

class SpringApplicationAnalyzer {

    fun getBeanDefinitions(applicationData: ApplicationData): Array<BeanDefinitionData> {
        // TODO: get rid of SourceFinder
        val configurationClasses = SourceFinder(applicationData).findSources()
        val instantiationSettings = InstantiationSettings(
            configurationClasses,
            applicationData.springSettings.profiles.toTypedArray(),
        )

        return SpringApiProviderFacade.getInstance(this::class.java.classLoader)
            .useMostSpecificNonFailingApi(instantiationSettings) { springApi ->
                UtBotSpringShutdownException
                    .catch { springApi.getOrLoadSpringApplicationContext() }
                    .beanDefinitions
                    .toTypedArray()
            }.result.getOrThrow()
    }
}