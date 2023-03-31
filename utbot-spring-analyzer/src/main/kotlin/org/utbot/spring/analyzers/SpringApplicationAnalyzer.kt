package org.utbot.spring.analyzers

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

class SpringApplicationAnalyzer(
    private val applicationData: ApplicationData
) {

    fun analyze(): List<String> {
        val fakeFileManager =
            FakeFileManager(applicationData.propertyFilesPaths + applicationData.xmlConfigurationPaths)
        fakeFileManager.createTempFiles()

        val applicationBuilder = SpringApplicationBuilder(SpringApplicationAnalyzer::class.java)
        val applicationConfigurator = ApplicationConfigurator(applicationBuilder, applicationData)

        when (findConfigurationType(applicationData)) {
            XmlConfiguration -> applicationConfigurator.configureXmlBasedApplication()
            else -> applicationConfigurator.configureJavaBasedApplication()
        }

        try {
            applicationBuilder.build()
            applicationBuilder.run()
        } catch (e: ApplicationContextException) {
            // UtBotBeanFactoryPostProcessor destroys bean definitions
            // to prevent Spring application from actually starting and
            // that causes it to throw ApplicationContextException
            println("Bean analysis finished successfully")
        } finally {
            fakeFileManager.deleteTempFiles()
        }
        return UtBotBeanFactoryPostProcessor.beanQualifiedNames
    }

    private fun findConfigurationType(applicationData: ApplicationData): ApplicationConfigurationType {
        //TODO: support Spring Boot Applications here.
        val fileExtension = File(applicationData.configurationFile).extension
        return when (fileExtension) {
            "xml" -> XmlConfiguration
            else -> JavaConfiguration
        }
    }
}
