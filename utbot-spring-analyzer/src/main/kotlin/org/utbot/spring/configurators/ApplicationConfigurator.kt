package org.utbot.spring.configurators

import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry
import org.springframework.boot.SpringApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.context.annotation.ComponentScanBeanDefinitionParser
import org.springframework.core.io.DefaultResourceLoader
import org.utbot.spring.config.TestApplicationConfiguration
import org.utbot.spring.data.ApplicationData
import org.utbot.spring.utils.ConfigurationManager
import java.io.File
import java.net.URLClassLoader
import kotlin.io.path.Path

private val logger = getLogger<ApplicationConfigurator>()

open class ApplicationConfigurator(
    private val applicationBuilder: SpringApplicationBuilder,
    private val applicationData: ApplicationData
) {
//    private val classLoader: ClassLoader = URLClassLoader(applicationData.classpath)
    private val classLoader: ClassLoader = this::class.java.classLoader

    fun configureApplication() {
        //applicationBuilder.resourceLoader(DefaultResourceLoader().also { it.classLoader = classLoader })
        when (findConfigurationType(applicationData)) {
            ApplicationConfigurationType.XmlConfiguration -> {
                logger.info { "Using xml Spring configuration" }
                val configurationManager = ConfigurationManager(classLoader, TestApplicationConfiguration::class.java)
                // Put `applicationData.configurationFile` in `@ImportResource` of `TestApplicationConfiguration`
                configurationManager.patchImportResourceAnnotation(Path(applicationData.configurationFile).fileName)
                applicationBuilder.sources(TestApplicationConfiguration::class.java)
            }
            else -> {
                logger.info { "Using java Spring configuration" }
                applicationBuilder.sources(
                    TestApplicationConfiguration::class.java,
                    classLoader.loadClass(applicationData.configurationFile)
                )
            }
        }
    }

    private fun findConfigurationType(applicationData: ApplicationData): ApplicationConfigurationType {
        //TODO: support Spring Boot Applications here.
        val fileExtension = File(applicationData.configurationFile).extension
        return when (fileExtension) {
            "xml" -> ApplicationConfigurationType.XmlConfiguration
            else -> ApplicationConfigurationType.JavaConfiguration
        }
    }
}
