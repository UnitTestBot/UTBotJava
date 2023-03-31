package org.utbot.spring.configurators

import org.springframework.boot.builder.SpringApplicationBuilder
import org.utbot.spring.config.TestApplicationConfiguration
import org.utbot.spring.data.ApplicationData
import org.utbot.spring.utils.ConfigurationManager
import java.net.URLClassLoader

open class ApplicationConfigurator(
    private val app: SpringApplicationBuilder,
    private val applicationData: ApplicationData
) {
    private val classLoader: ClassLoader = URLClassLoader(applicationData.applicationUrlArray)

    fun configureJavaBasedApplication() {
        configureApplication(
            configurationClass = classLoader.loadClass(applicationData.configurationFile),
            xmlConfigurationPaths = applicationData.xmlConfigurationPaths
        )
    }

    fun configureXmlBasedApplication() {
        configureApplication(
            configurationClass = TestApplicationConfiguration::class.java,
            xmlConfigurationPaths = listOf(applicationData.configurationFile)
        )
    }

    private fun configureApplication(configurationClass: Class<*>, xmlConfigurationPaths: List<String>) {

        val configurationManager = ConfigurationManager(classLoader, configurationClass)

        val xmlFilesConfigurator = XmlFilesConfigurator(xmlConfigurationPaths, configurationManager)
        val propertiesConfigurator = PropertiesConfigurator(applicationData.propertyFilesPaths, configurationManager)

        propertiesConfigurator.configure()
        xmlFilesConfigurator.configure()

        for (prop in propertiesConfigurator.readProperties()) {
            app.properties(prop)
        }
        app.sources(TestApplicationConfiguration::class.java, configurationClass)
    }
}
