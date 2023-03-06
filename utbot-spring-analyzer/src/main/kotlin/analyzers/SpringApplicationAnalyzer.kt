package analyzers

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
    private val propertyFilePath: String,
    private val xmlConfigurationPath: String,
) {

    private val applicationUrl: URL
        get() = Path.of(applicationPath).toUri().toURL()

    fun analyze() {
        val classLoader: ClassLoader = URLClassLoader(arrayOf(applicationUrl))
        val userConfigurationClass = classLoader.loadClass(configurationClassFqn)

        val configurationManager = ConfigurationManager(classLoader, userConfigurationClass)
        val propertiesAnalyzer = PropertiesAnalyzer(propertyFilePath)
        val xmlConfigurationAnalyzer = XmlConfigurationAnalyzer(xmlConfigurationPath)

        xmlConfigurationAnalyzer.fillFakeApplicationXml()

        configurationManager.patchPropertySourceAnnotation()
        configurationManager.patchImportResourceAnnotation()

        val app = SpringApplicationBuilder(SpringApplicationAnalyzer::class.java)
        app.sources(TestApplicationConfiguration::class.java, userConfigurationClass)
        for (prop in propertiesAnalyzer.readProperties()) {
            app.properties(prop)
        }

        try {
            app.build()
            app.run()
        } catch (e: ApplicationContextException) {
            println("Bean analysis finished successfully")
        }
    }
}