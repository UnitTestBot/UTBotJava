package org.utbot.spring.analyzers

import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContextException
import org.utbot.spring.configurators.ApplicationConfigurator
import org.utbot.spring.data.ApplicationData
import org.utbot.spring.utils.ExtensionUtils
import org.utbot.spring.utils.FakeFileManager
import java.io.File

class SpringApplicationAnalyzer(
    private val applicationData: ApplicationData
) {

    fun analyze() {
        val fakeFileManager =
            FakeFileManager(applicationData.propertyFilesPaths + applicationData.xmlConfigurationPaths)
        fakeFileManager.createTempFiles()

        val app = SpringApplicationBuilder(SpringApplicationAnalyzer::class.java)

        val applicationConfigurator = ApplicationConfigurator(app, applicationData)
        when (File(applicationData.configurationFile).extension) {
            ExtensionUtils.XML -> {
                applicationConfigurator.configureXmlBasedApplication()
            }
            else -> {
                applicationConfigurator.configureJavaBasedApplication()
            }
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
