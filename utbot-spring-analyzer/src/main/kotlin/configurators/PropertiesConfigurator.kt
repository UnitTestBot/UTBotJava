package application.configurators

import utils.ConfigurationManager
import utils.PathsUtils
import java.io.BufferedReader
import java.io.FileReader
import kotlin.io.path.Path

class PropertiesConfigurator(
    private val propertiesFilesPaths: List<String>,
    private val configurationManager: ConfigurationManager
) {

    fun configure() {
        configurationManager.clearPropertySourceAnnotation()

        propertiesFilesPaths
            .map { Path(it).fileName }
            .forEach { fileName -> configurationManager.patchPropertySourceAnnotation(fileName) }
    }

    fun readProperties(): ArrayList<String> {
        val props = ArrayList<String>()

        for (propertiesFilePath in propertiesFilesPaths) {
            if (propertiesFilePath == PathsUtils.EMPTY_PATH) continue

            val reader = BufferedReader(FileReader(propertiesFilePath))
            var line = reader.readLine()
            while (line != null) {
                props.add(line)
                line = reader.readLine()
            }

            reader.close()
        }

        return props
    }
}