package utils

import kotlin.io.path.Path

object PathsUtils {
    const val EMPTY_PATH = ""

    fun createFakeFilePath(fileName: String): String =
        Path(buildResourcesPath, "fake_${Path(fileName).fileName}").toString()

    //TODO: it is better to do it without marker files
    private val buildResourcesPath: String
        get() {
            val resourcesMarker =
                this.javaClass.classLoader.getResource("resources_marker.txt")
                    ?: error("Resources marker file is not found")

            return Path(resourcesMarker.path).parent.toString()
        }

}