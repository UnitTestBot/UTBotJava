package org.utbot.framework.process

import mu.KotlinLogging
import org.utbot.framework.plugin.api.BeanAdditionalData
import org.utbot.framework.plugin.api.BeanDefinitionData
import org.utbot.framework.plugin.api.SpringSettings.PresentSpringSettings
import org.utbot.rd.use
import org.utbot.spring.process.SpringAnalyzerProcess

class SpringAnalyzerTask(
    private val classpath: List<String>,
    private val settings: PresentSpringSettings,
) : EngineProcessTask<List<BeanDefinitionData>> {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override fun perform(): List<BeanDefinitionData> = try {
        SpringAnalyzerProcess.createBlocking(classpath).use {
            it.getBeanDefinitions(settings)
        }.beanDefinitions
            .map { data ->
                // mapping RD models to API models
                val additionalData = data.additionalData?.let {
                    BeanAdditionalData(
                        it.factoryMethodName,
                        it.parameterTypes,
                        it.configClassFqn
                    )
                }
                BeanDefinitionData(
                    data.beanName,
                    data.beanTypeFqn,
                    additionalData
                )
            }
    } catch (e: Exception) {
        logger.error(e) { "Spring Analyzer crashed, resorting to using empty bean list" }
        emptyList()
    }
}