package org.utbot.spring.configurators

import org.utbot.spring.utils.ConfigurationManager
import org.utbot.spring.utils.PathsUtils
import kotlin.io.path.Path

class XmlFilesConfigurator(
    private val userXmlFilePaths: List<String>,
    private val configurationManager: ConfigurationManager,
) {

    fun configure() {
        configurationManager.clearImportResourceAnnotation()

        for (userXmlFilePath in userXmlFilePaths) {
            if(userXmlFilePath == PathsUtils.EMPTY_PATH)continue

            val xmlConfigurationParser =
                XmlConfigurationParser(userXmlFilePath, PathsUtils.createFakeFilePath(userXmlFilePath))

            xmlConfigurationParser.fillFakeApplicationXml()
            configurationManager.patchImportResourceAnnotation(Path(userXmlFilePath).fileName)
        }
    }
}