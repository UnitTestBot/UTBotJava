package application.configurators

import utils.ConfigurationManager
import utils.Paths
import java.io.BufferedReader
import java.io.FileReader
import kotlin.io.path.Path

class PropertiesConfigurator(
    private val propertiesFilesPaths: List<String>,
    private val configurationManager: ConfigurationManager
) {

    fun configure(){
        configurationManager.clearPropertySourceAnnotation()

        for(propertiesFilePath in propertiesFilesPaths) {
            if(propertiesFilePath == Paths.EMPTY_PATH)continue

            configurationManager.patchPropertySourceAnnotation(Path(propertiesFilePath).fileName)
        }
    }

    fun readProperties(): ArrayList<String> {
        val props = ArrayList<String>()

        for(propertiesFilePath in propertiesFilesPaths) {
            if(propertiesFilePath == Paths.EMPTY_PATH)continue

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