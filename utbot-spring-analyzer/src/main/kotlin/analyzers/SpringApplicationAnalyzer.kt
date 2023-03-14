package analyzers

import application.utils.FakeFileManager
import application.configurators.PropertiesConfigurator
import application.configurators.XmlFilesConfigurator
import config.TestApplicationConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContextException
import utils.ConfigurationManager
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path


class SpringApplicationAnalyzer(
    private val applicationPath: String,
    private val configurationClassFqn: String,
    private val propertyFilesPaths: List<String>,
    private val xmlConfigurationPaths: List<String>,
) {

    private val applicationUrl: URL
        get() = Path.of(applicationPath).toUri().toURL()

    fun analyze() {
        val fakeFileManager = FakeFileManager(propertyFilesPaths + xmlConfigurationPaths)
        fakeFileManager.createFakeFiles()

        val classLoader: ClassLoader = URLClassLoader(arrayOf(applicationUrl))
        val userConfigurationClass = classLoader.loadClass(configurationClassFqn)

        val configurationManager = ConfigurationManager(classLoader, userConfigurationClass)
        val propertiesConfigurator = PropertiesConfigurator(propertyFilesPaths, configurationManager)
        val xmlFilesConfigurator = XmlFilesConfigurator(xmlConfigurationPaths, configurationManager, fakeFileManager)

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
            println("Bean analysis finished successfully")
        }finally {
            fakeFileManager.deleteFakeFiles()
        }
    }
}