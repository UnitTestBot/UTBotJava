package org.utbot.spring.utils

import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.utbot.spring.api.ApplicationData
import org.utbot.spring.config.TestApplicationConfiguration
import org.utbot.spring.configurators.ApplicationConfigurationType
import java.io.File
import kotlin.io.path.Path

private val logger = getLogger<SourceFinder>()

class SourceFinder(
    private val applicationData: ApplicationData
) {
    private val classLoader: ClassLoader = this::class.java.classLoader

    fun findSources(): Array<Class<*>> = when (configurationType) {
        ApplicationConfigurationType.XmlConfiguration -> {
            logger.info { "Using xml Spring configuration" }
            val configurationManager = ConfigurationManager(classLoader, TestApplicationConfiguration::class.java)
            // Put `applicationData.configurationFile` in `@ImportResource` of `TestApplicationConfiguration`
            configurationManager.patchImportResourceAnnotation(Path(applicationData.configurationFile).fileName)
            arrayOf(TestApplicationConfiguration::class.java)
        }
        ApplicationConfigurationType.JavaConfiguration -> {
            logger.info { "Using java Spring configuration" }
            arrayOf(
                TestApplicationConfiguration::class.java,
                classLoader.loadClass(applicationData.configurationFile)
            )
        }
    }

    private val configurationType: ApplicationConfigurationType
        get() = when (File(applicationData.configurationFile).extension) {
            "xml" -> ApplicationConfigurationType.XmlConfiguration
            else -> ApplicationConfigurationType.JavaConfiguration
        }
}