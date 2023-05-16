package org.utbot.spring.analyzers

import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.springframework.boot.SpringBootVersion
import org.springframework.core.SpringVersion
import org.utbot.spring.api.ApplicationData
import org.utbot.spring.exception.UtBotSpringShutdownException
import org.utbot.spring.generated.BeanDefinitionData
import org.utbot.spring.utils.EnvironmentFactory
import org.utbot.spring.utils.SourceFinder

private val logger = getLogger<SpringApplicationAnalyzerFacade>()

class SpringApplicationAnalyzerFacade(private val applicationData: ApplicationData) {

    fun analyze(): List<BeanDefinitionData> {
        logger.info { "Current Java version is: " + System.getProperty("java.version") }
        logger.info { "Current Spring version is: " + runCatching { SpringVersion.getVersion() }.getOrNull() }
        logger.info { "Current Spring Boot version is: " + runCatching { SpringBootVersion.getVersion() }.getOrNull() }

        val sources = SourceFinder(applicationData).findSources()
        val environmentFactory = EnvironmentFactory(applicationData)

        for (analyzer in listOf(SpringBootApplicationAnalyzer(), PureSpringApplicationAnalyzer())) {
            if (analyzer.canAnalyze()) {
                logger.info { "Analyzing with $analyzer" }
                try {
                    return UtBotSpringShutdownException
                        .catch { analyzer.analyze(sources, environmentFactory.createEnvironment()) }
                        .beanDefinitions
                } catch (e: Throwable) {
                    logger.error("Analyzer $analyzer failed", e)
                }
            }
        }
        logger.error { "All Spring analyzers failed, using empty bean list" }
        return emptyList()
    }
}
