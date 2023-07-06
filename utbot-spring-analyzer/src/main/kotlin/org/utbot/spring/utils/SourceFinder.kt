package org.utbot.spring.utils

import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.springframework.context.annotation.ImportResource
import org.utbot.common.patchAnnotation
import org.utbot.spring.api.ApplicationData
import org.utbot.spring.config.TestApplicationConfiguration
import org.utbot.spring.configurators.ApplicationConfigurationType
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path

private val logger = getLogger<SourceFinder>()

class SourceFinder(
    private val applicationData: ApplicationData
) {
    private val classLoader: ClassLoader = this::class.java.classLoader

    fun findSources(): Array<Class<*>> = when (configurationType) {
        ApplicationConfigurationType.XmlConfiguration -> {
            logger.info { "Using xml Spring configuration" }

            // Put `applicationData.configurationFile` in `@ImportResource` of `TestApplicationConfiguration`
            patchImportResourceAnnotation(Path(applicationData.configurationFile).fileName)

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

    private fun patchImportResourceAnnotation(userXmlFilePath: Path) =
        patchAnnotation(
            annotation = TestApplicationConfiguration::class.java.getAnnotation(ImportResource::class.java),
            property = "value",
            newValue = arrayOf(String.format("classpath:%s", "$userXmlFilePath"))
        )
}