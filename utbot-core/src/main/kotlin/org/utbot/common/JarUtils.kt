package org.utbot.common

import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object JarUtils {
    private const val UNKNOWN_MODIFICATION_TIME = 0L

    fun extractJarFileFromResources(jarFileName: String, jarResourcePath: String, targetDirectoryName: String): File {
        val targetDirectory =
            Files.createDirectories(utBotTempDirectory.toFile().resolve(targetDirectoryName).toPath()).toFile()
        return targetDirectory.resolve(jarFileName).also { jarFile ->
            val resource = this::class.java.classLoader.getResource(jarResourcePath)
                ?: error("Unable to find \"$jarResourcePath\" in resources, make sure it's on the classpath")
            updateJarIfRequired(jarFile, resource)
        }
    }

    private fun updateJarIfRequired(jarFile: File, resource: URL) {
        val resourceConnection = resource.openConnection()
        resourceConnection.getInputStream().use { inputStream ->
            val lastResourceModification = resourceConnection.lastModified
            if (
                !jarFile.exists() ||
                jarFile.lastModified() == UNKNOWN_MODIFICATION_TIME ||
                lastResourceModification == UNKNOWN_MODIFICATION_TIME ||
                jarFile.lastModified() < lastResourceModification
            ) {
                Files.copy(inputStream, jarFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}