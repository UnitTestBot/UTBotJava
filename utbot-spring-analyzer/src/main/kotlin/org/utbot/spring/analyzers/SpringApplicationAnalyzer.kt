package org.utbot.spring.analyzers

import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContextException
import org.utbot.spring.configurators.ApplicationConfigurationType
import org.utbot.spring.configurators.ApplicationConfigurationType.JavaConfiguration
import org.utbot.spring.configurators.ApplicationConfigurationType.XmlConfiguration
import org.utbot.spring.configurators.ApplicationConfigurator
import org.utbot.spring.data.ApplicationData
import org.utbot.spring.utils.FakeFileManager
import org.utbot.spring.postProcessors.UtBotBeanFactoryPostProcessor
import java.io.File

val logger = getLogger<SpringApplicationAnalyzer>()

class SpringApplicationAnalyzer(
    private val applicationData: ApplicationData
) {

    fun analyze(): List<String> {
        logger.info { "Analyzer Java: " + System.getProperty("java.version") }

        val applicationBuilder = SpringApplicationBuilder(SpringApplicationAnalyzer::class.java)
        val applicationConfigurator = ApplicationConfigurator(applicationBuilder, applicationData)

        applicationConfigurator.configureApplication()

        try {
            applicationBuilder.build()
            applicationBuilder.run()
        } catch (e: ApplicationContextException) {
            // UtBotBeanFactoryPostProcessor destroys bean definitions
            // to prevent Spring application from actually starting and
            // that causes it to throw ApplicationContextException
            logger.info { "Bean analysis finished successfully" }
        }
        return UtBotBeanFactoryPostProcessor.beanQualifiedNames
    }
}
