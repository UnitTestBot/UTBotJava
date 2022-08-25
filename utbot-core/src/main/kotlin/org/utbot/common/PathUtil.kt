package org.utbot.common

import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

object PathUtil {

    /**
     * Creates a Path from the String.
     */
    fun String.toPath(): Path = Paths.get(this)

    /**
     * Finds a path for the [other] relative to the [root] if it is possible.
     *
     * Example: safeRelativize("C:/project/", "C:/project/src/Main.java") = "src/Main.java".
     */
    fun safeRelativize(root: String?, other: String?): String? {
        if (root == null || other == null)
            return null
        val rootPath = root.toPath()
        val otherPath = other.toPath()
        return if (otherPath.startsWith(rootPath))
            replaceSeparator(rootPath.relativize(otherPath).toString())
        else
            null
    }

    /**
     * Removes class fully qualified name from absolute path to the file if it is possible.
     *
     * Example: removeClassFqnFromPath("C:/project/src/com/Main.java", "com.Main") = "C:/project/src/".
     */
    fun removeClassFqnFromPath(sourceAbsolutePath: String?, classFqn: String?): String? {
        if (sourceAbsolutePath == null || classFqn == null)
            return null

        val normalizedPath = replaceSeparator(
            sourceAbsolutePath.substringBeforeLast('.') // remove extension
        )
        val classFqnPath = classFqnToPath(classFqn)

        return if (normalizedPath.endsWith(classFqnPath))
            normalizedPath.removeSuffix(classFqnPath)
        else
            null
    }

    /**
     * Resolves [toResolve] against [absolute] and checks if a resolved path exists.
     *
     * Example: resolveIfExists("C:/project/src/", "Main.java") = "C:/project/src/Main.java".
     */
    fun resolveIfExists(absolute: String, toResolve: String): String? {
        val absolutePath = absolute.toPath()
        val toResolvePath = toResolve.toPath()
        val resolvedPath = absolutePath.resolve(toResolvePath)

        return if (resolvedPath.toFile().exists())
            resolvedPath.toString()
        else
            null
    }

    /**
     * Replaces '\\' in the [path] with '/'.
     */
    fun replaceSeparator(path: String): String =
        path.replace('\\', '/')

    /**
     * Replaces '.' in the [classFqn] with '/'.
     */
    fun classFqnToPath(classFqn: String): String =
        classFqn.replace('.', '/')

    /**
     * Returns a URL to represent this path.
     */
    fun Path.toURL(): URL =
        this.toUri().toURL()

    /**
     * Returns html link tag of the file.
     */
    fun toHtmlLinkTag(filePath: String, fileName: String = filePath.toPath().fileName.toString()): String =
        """<a href="file:///$filePath">${fileName}</a>"""

    /**
     * Returns the extension of this file (including the dot).
     */
    val Path.fileExtension: String
        get() = "." + this.toFile().extension

    @JvmStatic
    fun getUrlsFromClassLoader(contextClassLoader: ClassLoader): Array<URL> {
        return if (contextClassLoader is URLClassLoader) {
            contextClassLoader.urLs
        } else {
            System.getProperty("java.class.path").split(File.pathSeparator).dropLastWhile { it.isEmpty() }
                .map { File(it).toURL() }.toTypedArray()
        }
    }
}