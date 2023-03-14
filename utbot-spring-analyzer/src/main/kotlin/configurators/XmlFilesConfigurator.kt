package application.configurators

import analyzers.XmlConfigurationAnalyzer
import application.utils.FakeFileManager
import utils.ConfigurationManager
import utils.Paths
import kotlin.io.path.Path

class XmlFilesConfigurator(
    private val userXmlFilePaths: List<String>,
    private val configurationManager: ConfigurationManager,
    private val fakeFileManager: FakeFileManager
) {

    fun configure() {
        configurationManager.clearImportResourceAnnotation()

        for (userXmlFilePath in userXmlFilePaths) {
            if(userXmlFilePath == Paths.EMPTY_PATH)continue

            val xmlConfigurationAnalyzer =
                XmlConfigurationAnalyzer(userXmlFilePath, fakeFileManager.getFakeFilePath(userXmlFilePath))

            xmlConfigurationAnalyzer.fillFakeApplicationXml()
            configurationManager.patchImportResourceAnnotation(Path(userXmlFilePath).fileName)
        }
    }
}