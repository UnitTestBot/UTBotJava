package org.utbot.spring.analyzers

import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContextException
import org.utbot.spring.configurators.ApplicationConfigurator
import org.utbot.spring.api.ApplicationData
import org.utbot.spring.postProcessors.UtBotBeanFactoryPostProcessor

val logger = getLogger<SpringApplicationAnalyzer>()

class SpringApplicationAnalyzer(private val applicationData: ApplicationData) {

    fun analyze(): List<String> {
        logger.info { "Current Java version is: " + System.getProperty("java.version") }

        val applicationBuilder = SpringApplicationBuilder(SpringApplicationAnalyzer::class.java)
        val applicationConfigurator = ApplicationConfigurator(applicationBuilder, applicationData)

        applicationConfigurator.configureApplication()

        try {
            applicationBuilder.build()
            applicationBuilder.run()
        } catch (e: ApplicationContextException) {
            // UtBotBeanFactoryPostProcessor destroys bean definitions
            // to prevent Spring application from actually starting and
            // that causes it to throw ApplicationContextException.
            logger.info { "Bean analysis finished successfully" }
        }
        return UtBotBeanFactoryPostProcessor.beanQualifiedNames
    }
}
