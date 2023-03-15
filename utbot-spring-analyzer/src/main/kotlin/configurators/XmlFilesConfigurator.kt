package application.configurators

import analyzers.XmlConfigurationAnalyzer
import application.utils.FakeFileManager
import utils.ConfigurationManager
import utils.PathsUtils
import kotlin.io.path.Path

class XmlFilesConfigurator(
    private val userXmlFilePaths: List<String>,
    private val configurationManager: ConfigurationManager,
) {

    fun configure() {
        configurationManager.clearImportResourceAnnotation()

        for (userXmlFilePath in userXmlFilePaths) {
            if(userXmlFilePath == PathsUtils.EMPTY_PATH)continue

            val xmlConfigurationAnalyzer =
                XmlConfigurationAnalyzer(userXmlFilePath, PathsUtils.createFakeFilePath(userXmlFilePath))

            xmlConfigurationAnalyzer.fillFakeApplicationXml()
            configurationManager.patchImportResourceAnnotation(Path(userXmlFilePath).fileName)
        }
    }
}