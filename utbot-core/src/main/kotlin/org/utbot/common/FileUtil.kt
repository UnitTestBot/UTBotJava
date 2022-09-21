package org.utbot.common

import org.utbot.common.PathUtil.toPath
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.Files.createTempDirectory
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile
import kotlin.concurrent.thread
import kotlin.streams.asSequence
import mu.KotlinLogging

fun Class<*>.toClassFilePath(): String {
    val name = requireNotNull(name) { "Class is local or anonymous" }

    return "${name.replace('.', '/')}.class"
}

object FileUtil {
    private const val TEMP_DIR_NAME = "utbot-temporary-"

    fun extractArchive(
        archiveFile: Path,
        destPath: Path,
        vararg options: CopyOption = arrayOf(StandardCopyOption.REPLACE_EXISTING)
    ) {
        Files.createDirectories(destPath)

        ZipFile(archiveFile.toFile()).use { archive ->
            val entries = archive.stream().asSequence()
                .sortedBy { it.name }
                .toList()

            for (entry in entries) {
                val entryDest = Paths.get(destPath.toString(), entry.name).normalize()

                if (entry.isDirectory) {
                    Files.createDirectories(entryDest)
                } else {
                    //in case if we have no folders as separate entries in the jar (can be in case if
                    //a jar is assembled with an obfuscator)
                    Files.createDirectories(entryDest.parent)
                    if (!entryDest.toFile().exists()) {
                        repeatWithTimeout { Files.copy(archive.getInputStream(entry), entryDest, *options) }
                        // ^ it is necessary because of asynchronous call in windows filesystem and file is not actually
                        // ready for usage. If we remove this, it can throw an exception.
                    }
                }

            }
        }

    }

    /**
     * Deletes all the files and folders from the java unit-test temp directory that are older than [daysLimit].
     */
    fun clearTempDirectory(daysLimit: Int) {
        val currentTimeInMillis = System.currentTimeMillis()

        val files = utBotTempDirectory.toFile().listFiles() ?: return

        files.filter {
            val creationTime = Files.readAttributes(it.toPath(), BasicFileAttributes::class.java).creationTime()
            TimeUnit.MILLISECONDS.toDays(currentTimeInMillis - creationTime.toMillis()) > daysLimit
        }.forEach { it.deleteRecursively() }
    }

    fun createTempDirectory(prefix: String): Path {
        return createTempDirectory(utBotTempDirectory, prefix)
    }

    /**
     * Copy the class file for given [classes] to temporary folder.
     * It can be used for Soot analysis.
     */
    fun isolateClassFiles(vararg classes: Class<*>): File {
        val tempDir = createTempDirectory("generated-").toFile()

        for (clazz in classes) {
            val path = clazz.toClassFilePath()
            val resource = clazz.classLoader.getResource(path) ?: error("No such file: $path")

            if (resource.toURI().scheme == "jar") {
                val jarLocation = resource.toURI().extractJarName()
                extractClassFromArchive(Paths.get(jarLocation), clazz, tempDir.toPath())
            } else {
                resource.openStream().buffered().use { input ->
                    File(tempDir, path)
                        .apply { parentFile.mkdirs() }
                        .outputStream()
                        .use { output -> input.copyTo(output) }
                }
            }
        }
        return tempDir
    }

    private fun URI.extractJarName(): URI = URI(this.schemeSpecificPart.substringBefore("!").replace(" ", "%20"))

    private fun extractClassFromArchive(archiveFile: Path, clazz: Class<*>, destPath: Path) {
        val classFilePath = clazz.toClassFilePath()
        ZipFile(archiveFile.toFile()).use { archive ->
            val entry = archive.stream().asSequence().filter { it.name.normalizePath() == classFilePath }.single()
            val entryDest = Paths.get(destPath.toString(), entry.name).normalize()
            Files.createDirectories(entryDest.parent)
            Files.copy(archive.getInputStream(entry), entryDest, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    /**
     * Locates class path by class.
     * It can be used for Soot analysis.
     */
    fun locateClassPath(clazz: Class<*>): File? {
        val path = clazz.toClassFilePath()
        val resource = requireNotNull(clazz.classLoader.getResource(path)) { "No such file: $path" }
        if (resource.toURI().scheme == "jar") return null
        val fullPath = resource.path.removeSuffix(path)
        return File(fullPath)
    }

    /**
     * Locates class and returns class location. Could be directory or jar based.
     */
    fun locateClass(clazz: Class<*>): ClassLocation {
        val path = clazz.toClassFilePath()
        val resource = requireNotNull(clazz.classLoader.getResource(path)) { "No such file: $path" }
        return if (resource.toURI().scheme == "jar") {
            val jarLocation = resource.toURI().extractJarName()
            JarClassLocation(Paths.get(jarLocation))
        } else {
            val fullPath = resource.path.removeSuffix(path)
            DirClassLocation(File(fullPath).toPath())
        }
    }

    /**
     * This class clean old temporary directories.
     */
    object OldTempFileDeleter {
        private val maxDeltaTime = Duration.ofHours(2).toMillis()
        private val logger = KotlinLogging.logger("OldTempFileDeleter")

        init {
            val currentTime = System.currentTimeMillis()
            thread(priority = Thread.MIN_PRIORITY) {
                runCleanOldTemp(currentTime)
            }
        }

        private fun runCleanOldTemp(currentTime: Long) {
            tempDirectoryPath.toPath().toFile().listFiles { file ->
                file.isDirectory &&
                file.name.startsWith(TEMP_DIR_NAME) &&
                (currentTime - file.lastModified()) > maxDeltaTime
            }?.forEach { dir ->
                logger.info("Start deleting old directory $dir")
                dir.deleteRecursively()
            }
        }
    }

    /**
     * Extracts archive to temp directory and returns path to directory.
     */
    fun extractArchive(archiveFile: Path): Path {
        val tempDir = createTempDirectory(TEMP_DIR_NAME).toFile().apply { deleteOnExit() }
        extractArchive(archiveFile, tempDir.toPath())
        return tempDir.toPath()
    }

    /**
     * Returns the path to the class files for the given ClassLocation.
     */
    fun findPathToClassFiles(classLocation: ClassLocation) = when (classLocation) {
        is DirClassLocation -> classLocation.path
        is JarClassLocation -> extractArchive(classLocation.jarPath)
    }

    /**
     * Calculates latest timestamp for list of files and directories.
     * Returns zero for empty list.
     */
    fun maxLastModifiedRecursivelyMillis(paths: List<File>): Long =
        paths.maxOfOrNull { maxLastModifiedRecursivelyMillis(it) } ?: 0

    private fun maxLastModifiedRecursivelyMillis(file: File): Long {
        if (file.exists()) {
            return if (file.isFile) {
                file.lastModified()
            } else {
                try {
                    Files.walk(file.toPath()).mapToLong { Files.getLastModifiedTime(it).toMillis() }.max().orElse(0)
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
        }
        return 0
    }

    /**
     * Finds all files (not directories) in the current directory and in all its subdirectories.
     */
    fun File.findAllFilesOnly(): List<File> =
        this.walk().filter { it.isFile }.toList()

    /**
     * Creates a new file with all parent directories.
     * Does nothing if the file already exists.
     */
    fun File.createNewFileWithParentDirectories() {
        this.parentFile.mkdirs()
        this.createNewFile()
    }

    // https://stackoverflow.com/a/68822715
    fun byteCountToDisplaySize(bytes: Long): String {
        val bytesInDouble = bytes.toDouble()

        return when {
            bytesInDouble >= 1 shl 30 -> "%.1f GB".format(bytesInDouble / (1 shl 30))
            bytesInDouble >= 1 shl 20 -> "%.1f MB".format(bytesInDouble / (1 shl 20))
            bytesInDouble >= 1 shl 10 -> "%.0f kB".format(bytesInDouble / (1 shl 10))
            else -> "$bytesInDouble bytes"
        }
    }
}

/**
 * Executes the [block]. If an exception occurs, repeats it after [timeout] ms.
 *
 * [timeout] = 50 is the empirically selected constant.
 */
inline fun <reified T> repeatWithTimeout(timeout: Long = 50, block: () -> T): T {
    return try {
        block()
    } catch (e: Exception) {
        Thread.sleep(timeout)
        block()
    }
}

private fun String.normalizePath(): String = this.replace('\\', '/')

fun String.asPathToFile() = File(this).path

sealed class ClassLocation

data class DirClassLocation(val path: Path) : ClassLocation()

data class JarClassLocation(val jarPath: Path) : ClassLocation()

val utBotTempDirectory: Path
    get() = Paths.get(tempDirectoryPath + File.separator + utBotTempFolderPrefix).also { it.toFile().mkdirs() }

private val utBotTempFolderPrefix = "UTBot${File.separator}"

private val tempDirectoryPath = System.getProperty("java.io.tmpdir")