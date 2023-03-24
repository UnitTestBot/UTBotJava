package org.utbot.spring.analyzers

import org.utbot.spring.utils.FakeFileManager
import org.utbot.spring.configurators.PropertiesConfigurator
import org.utbot.spring.configurators.XmlFilesConfigurator
import org.utbot.spring.config.TestApplicationConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContextException
import org.utbot.spring.utils.ConfigurationManager
import java.net.URL
import java.net.URLClassLoader


class SpringApplicationAnalyzer(
    private val applicationUrl: URL,
    private val configurationClassFqn: String,
    private val propertyFilesPaths: List<String>,
    private val xmlConfigurationPaths: List<String>,
) {

    fun analyze() {
        val fakeFileManager = FakeFileManager(propertyFilesPaths + xmlConfigurationPaths)
        fakeFileManager.createTempFiles()

        val classLoader: ClassLoader = URLClassLoader(arrayOf(applicationUrl))
        val userConfigurationClass = classLoader.loadClass(configurationClassFqn)

        val configurationManager = ConfigurationManager(classLoader, userConfigurationClass)
        val propertiesConfigurator = PropertiesConfigurator(propertyFilesPaths, configurationManager)
        val xmlFilesConfigurator = XmlFilesConfigurator(xmlConfigurationPaths, configurationManager)

        propertiesConfigurator.configure()
        xmlFilesConfigurator.configure()

        val app = SpringApplicationBuilder(SpringApplicationAnalyzer::class.java)
        app.sources(TestApplicationConfiguration::class.java, userConfigurationClass)
        for (prop in propertiesConfigurator.readProperties()) {
            app.properties(prop)
        }

        try {
            app.build()
            app.run()
        } catch (e: ApplicationContextException) {
            // UtBotBeanFactoryPostProcessor destroys bean definitions
            // to prevent Spring application from actually starting and
            // that causes it to throw ApplicationContextException
            println("Bean analysis finished successfully")
        } finally {
            fakeFileManager.deleteTempFiles()
        }
    }
}
