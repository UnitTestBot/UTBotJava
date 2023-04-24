package org.utbot.spring.analyzers

import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.springframework.boot.SpringBootVersion
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.SpringVersion
import org.utbot.spring.utils.SourceFinder
import org.utbot.spring.api.ApplicationData
import org.utbot.spring.exception.UtBotSpringShutdownException
import org.utbot.spring.postProcessors.UtBotBeanFactoryPostProcessor
import org.utbot.spring.utils.EnvironmentFactory

private val logger = getLogger<SpringApplicationAnalyzerFacade>()

class SpringApplicationAnalyzerFacade(private val applicationData: ApplicationData) {

    fun analyze(): List<String> {
        logger.info { "Current Java version is: " + System.getProperty("java.version") }
        logger.info {
            "Current Spring version is: " + runCatching { SpringVersion.getVersion() }.getOrElse(logger::error)
        }

        val analyzer = try {
            this::class.java.classLoader.loadClass("org.springframework.boot.SpringApplication")
            logger.info {
                "Current Spring Boot version is: " + runCatching { SpringBootVersion.getVersion() }.getOrElse(logger::error)
            }
            SpringBootApplicationAnalyzer()
        } catch (e: ClassNotFoundException) {
            logger.info { "Spring Boot is not detected" }
            PureSpringApplicationAnalyzer()
        }

        logger.info { "Configuration file is: ${applicationData.configurationFile}" }

        return analyzer.analyze(
            SpringApplicationAnalysisContext(
                sources = SourceFinder(applicationData).findSources(),
                environmentFactory = EnvironmentFactory(applicationData)
            )
        )
    }
}
