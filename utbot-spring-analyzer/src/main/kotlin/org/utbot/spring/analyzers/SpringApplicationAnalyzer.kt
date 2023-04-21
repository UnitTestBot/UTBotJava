package org.utbot.spring.analyzers

import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.springframework.boot.SpringApplication
import org.springframework.boot.SpringBootVersion
import org.springframework.context.ApplicationContextException
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.SpringVersion
import org.utbot.common.silent
import org.utbot.spring.utils.SourceFinder
import org.utbot.spring.api.ApplicationData
import org.utbot.spring.exception.UtBotSpringShutdownException
import org.utbot.spring.postProcessors.UtBotBeanFactoryPostProcessor

val logger = getLogger<SpringApplicationAnalyzer>()

class SpringApplicationAnalyzer(private val applicationData: ApplicationData) {

    fun analyze(): List<String> {
        logger.info { "Current Java version is: " + System.getProperty("java.version") }
        logger.info {
            "Current Spring version is: " + runCatching { SpringVersion.getVersion() }.getOrElse(logger::error)
        }

        val isSpringBoot = try {
            this::class.java.classLoader.loadClass("org.springframework.boot.SpringApplication")
            logger.info {
                "Current Spring Boot version is: " + runCatching { SpringBootVersion.getVersion() }.getOrElse(logger::error)
            }
            true
        } catch (e: ClassNotFoundException) {
            logger.info { "Spring Boot is not detected" }
            false
        }

        logger.info { "Configuration file is: ${applicationData.configurationFile}" }
        val sources = SourceFinder(applicationData).findSources()

        if (isSpringBoot) analyzeSpringBootApplication(sources)
        else analyzePureSpringApplication(sources)

        return UtBotBeanFactoryPostProcessor.beanQualifiedNames
    }

    private fun analyzePureSpringApplication(sources: Array<Class<*>>) {
        logger.info { "Analyzing pure Spring application" }
        runExpectingUtBotSpringShutdownException {
            AnnotationConfigApplicationContext(*sources)
        }
    }

    private fun analyzeSpringBootApplication(sources: Array<Class<*>>) {
        logger.info { "Analyzing Spring Boot application" }
        try {
            runExpectingUtBotSpringShutdownException {
                SpringApplication(*sources).run()
            }
        } catch (e: Throwable) {
            logger.error("Failed to analyze Spring Boot application, falling back to using pure Spring", e)
            analyzePureSpringApplication(sources)
        }
    }

    private fun runExpectingUtBotSpringShutdownException(block: () -> Unit) {
        try {
            block()
            throw IllegalStateException("Failed to shutdown Spring application with UtBotSpringShutdownException")
        } catch (e: Throwable) {
            if (generateSequence(e) { it.cause }.any { it is UtBotSpringShutdownException })
                logger.info { "Spring application has been successfully shutdown with UtBotSpringShutdownException" }
            else
                throw e
        }
    }
}
