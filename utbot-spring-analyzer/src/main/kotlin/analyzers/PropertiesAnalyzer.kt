package analyzers

import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

class PropertiesAnalyzer(private val propertiesFilePath: String) {

    @Throws(IOException::class)
    fun readProperties(): ArrayList<String> {
        val props = ArrayList<String>()

        val reader = BufferedReader(FileReader(propertiesFilePath))
        var line = reader.readLine()
        while (line != null) {
            props.add(line)
            line = reader.readLine()
        }

        reader.close()

        return props
    }
}