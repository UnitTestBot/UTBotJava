package org.utbot.common

import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

object JarUtils {
    private const val UNKNOWN_MODIFICATION_TIME = 0L

    fun extractJarFileFromResources(jarFileName: String, jarResourcePath: String, targetDirectoryName: String): File {
        val resource = this::class.java.classLoader.getResource(jarResourcePath)
            ?: error("Unable to find \"$jarResourcePath\" in resources, make sure it's on the classpath")

        val targetDirectory = utBotTempDirectory.toFile().resolve(targetDirectoryName).toPath()
        fun extractToSubDir(subDir: String) =
            Files.createDirectories(targetDirectory.resolve(subDir)).toFile().resolve(jarFileName).also { jarFile ->
                updateJarIfRequired(jarFile, resource)
            }

        // We attempt to always extract jars to same locations, to avoid eating up drive space with
        // every UtBot launch, but we may fail to do so if multiple processes are running in parallel.
        repeat(10) { i ->
            runCatching {
                return extractToSubDir(i.toString())
            }
        }
        return extractToSubDir(UUID.randomUUID().toString())
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