package org.utbot.spring.utils

import java.io.File
import kotlin.io.path.Path

object PathsUtils {
    const val EMPTY_PATH = ""

    fun createFakeFilePath(fileName: String): String =
        Path(buildResourcesPath, "fake_${Path(fileName).fileName}").toString()

    private val buildResourcesPath: String
        get() {
            val resourcesMarker =
                this.javaClass.classLoader.getResource("resources_marker.txt")
                    ?: error("Resources marker file is not found")

            return Path(File(resourcesMarker.toURI()).path).parent.toString()
        }

}