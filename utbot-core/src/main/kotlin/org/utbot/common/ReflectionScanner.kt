package org.utbot.common

import org.utbot.common.FileUtil.findAllFilesOnly
import java.io.File
import java.io.InputStream
import java.net.URL
import java.net.URLDecoder
import java.util.jar.JarFile

private fun String.pkg2path(respectOs : Boolean) = replace('.', if (respectOs) File.separatorChar else '/')
private fun String.path2pkg() = replace(File.separatorChar, '.').replace('/','.')
private val classSuffix : String = ".class"

/**
 * Finds all classes from given classloader whose full name starts from *pkgs*
 * @param pkgs
 * @return Sequence of found classes
 */
fun ClassLoader.scanForClasses(vararg pkgs: String): Sequence<Class<*>> {
    return pkgs.asSequence().flatMap { pkg ->
        getResources(pkg.pkg2path(false))
            .asSequence()
            .flatMap { url ->
                url.process(pkg,
                    { file -> file.scanForClasses(pkg, this) },
                    { jar -> jar.scanForClasses(pkg, this) },
                    { emptySequence() }
                )
            }
    }.distinct()
}

fun ClassLoader.scanForResourcesContaining(vararg pkgs: String): Sequence<File> {
    return pkgs.asSequence().flatMap { pkg ->
        getResources(pkg.pkg2path(false))
            .asSequence()
            .flatMap { url ->
                url.process(pkg,
                    { sequenceOf(it) },
                    { jar -> sequenceOf(File(jar.name)) },
                    { emptySequence() }
                )
            }
    }.distinct()
}

/**
 * Finds a resource in the [package] that satisfies [resourcePathPredicate]
 */
fun ClassLoader.firstOrNullResourceIS(`package`: String, resourcePathPredicate: (String) -> Boolean): InputStream? {
    val url = getResource(`package`)
    return url?.process(
        pkg = url.toString(),
        processFile = { directory ->
            directory.listFiles()?.firstOrNull { file ->
                resourcePathPredicate(file.path)
            }?.inputStream()
        },
        processJar = { jar ->
            jar.entries().toList().firstOrNull { jarEntry ->
                resourcePathPredicate(jarEntry.toString())
            }?.let { jarEntry ->
                getResourceAsStream(jarEntry.name)
            }
        },
        onFail = { null }
    )
}

/**
 * Loads all classes whose class files are located in the [classesDirectory].
 *
 * For example, `classesDirectory` may be "./build/classes/java/main/"
 */
fun ClassLoader.loadClassesFromDirectory(classesDirectory: File): List<Class<*>> =
    classesDirectory.findAllFilesOnly().map { classFile ->
        classFile.relativeTo(classesDirectory)
    }.mapNotNull { classFile ->
        val classFqn = classFile.path.removeSuffix(classSuffix).path2pkg()
        this.tryLoadClass(classFqn)
    }

/**
 * Loads the class with the specified fully qualified name.
 * Returns null if the class was not found.
 */
fun ClassLoader.tryLoadClass(fqName: String): Class<*>? =
    try {
        loadClass(fqName)
    } catch (e: Throwable) {
        null
    }


private fun <T> URL.process(pkg: String, processFile: (File) -> T, processJar: (JarFile) -> T, onFail: () -> T) : T {
    return when (protocol) {
        "jar" -> {
            val path = urlDecode(toExternalForm().substringAfter("file:").substringBeforeLast("!"))
            processJar(JarFile(path))
        }
        "file" -> {
            processFile(toPath(pkg))
        }
        else -> onFail()
    }
}

fun URL.toPath(pkg: String = ""): File {
    val path = File(urlDecode(path)).absolutePath.removeSuffix(pkg.pkg2path(true))
    return File(path)
}

private fun File.scanForClasses(pkg: String, classLoader: ClassLoader): Sequence<Class<*>> {
    val root = this
    return walkTopDown()
        .filter { it.isFile && it.name.endsWith(classSuffix) }
        .map {
            val classFileLocation = it.absolutePath
            if (!classFileLocation.contains(pkg.pkg2path(true))) return@map null

            val relativeToRoot = classFileLocation.removePrefix(root.toString()).removePrefix(File.separator)
            val className = relativeToRoot.removeSuffix(classSuffix).path2pkg()
            val clazz = classLoader.tryLoadClass(className)
            clazz
        }.filterNotNull()
}

private fun JarFile.scanForClasses(prefix: String, classLoader: ClassLoader): Sequence<Class<*>> {
    val path = prefix.pkg2path(false) + '/'
    return entries().asSequence()
        .filter {!it.isDirectory && it.name.endsWith(classSuffix) && it.name.startsWith(path)}
        .map {
            val fqName = it.name.removeSuffix(classSuffix).path2pkg()
            classLoader.tryLoadClass(fqName)
        }.filterNotNull()
}

private fun urlDecode(encoded: String): String {
    try {
        return URLDecoder.decode(encoded, "UTF-8")
    } catch(e: Exception) {
        return encoded
    }
}