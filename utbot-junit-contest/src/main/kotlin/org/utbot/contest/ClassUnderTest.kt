package org.utbot.contest

import org.utbot.common.FileUtil
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.utContext
import java.io.File
import java.nio.file.Paths

class ClassUnderTest(
    val classId: ClassId,
    private val generatedTestsSourcesDir: File,
    classfileDirSecondaryLocation: File? = null
) {

    val fqn: String
        get() = classId.name
    val classLoader: ClassLoader
        get() = utContext.classLoader
    val clazz
        get() = classLoader.loadClass(fqn)

    /**
     * Directory where this .class file is located without package folder structure
     * E.g. for gradle projects it's `project/build/classes`.
     */
    val classfileDir: File by lazy {
        (FileUtil.locateClassPath(clazz)
            ?: classfileDirSecondaryLocation
            ?: FileUtil.isolateClassFiles(clazz)
            ).absoluteFile
    }
//    val classpathDir : File get() = FileUtil.locateClassPath(kotlinClass)?.absoluteFile !!


    val packageName: String get() = fqn.substringBeforeLast('.', "")
    val simpleName: String get() = createTestClassName(fqn)

    val testClassSimpleName: String get() = simpleName + "Test"

    val generatedTestFile: File
        get() = Paths.get(
            generatedTestsSourcesDir.canonicalPath,
            *(packageName.split('.') + "$testClassSimpleName.java").toTypedArray()
        ).toFile().absoluteFile

    override fun toString(): String {
        return "ClassUnderTest[ FQN: $fqn" +
            "\n    classfileDir: $classfileDir" +
            "\n    testClassSimpleName: $testClassSimpleName" +
            "\n    generatedTestFile: $generatedTestFile" +
            "\n    generatedTestsSourcesDir: $generatedTestsSourcesDir" +
            "\n]"
    }

    /**
     * Creates a name of test class.
     * We need the name in code and the name of test class file be similar.
     * On this way we need to avoid symbols like '$'.
     */
    private fun createTestClassName(name: String): String = name
        .substringAfterLast('.')
        .replace('\$', '_')
}