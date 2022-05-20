package org.utbot.cli

import java.io.File
import kotlin.reflect.KClass

internal fun loadClassesFromPath(classLoader: ClassLoader, pathToClassPathRoot: String): List<KClass<*>> {
    val classFiles = mutableListOf<KClass<*>>()
    // Create a java file from the path
    val root = File(pathToClassPathRoot)
    if (!root.exists()) {
        return emptyList()
    }
    //Scan and find all class files
    root.walkTopDown().forEach { file ->
        if (file.extension == "class") {
            // construct relative path
            val relativePathForClass = file.normalize().relativeTo(root)
            val fqnClassName = relativePathForClass.parent.replace('/', '.')
                .replace('\\', '.') + '.' + relativePathForClass.nameWithoutExtension
            classFiles.add(classLoader.loadClass(fqnClassName).kotlin)
        }
    }
    return classFiles
}