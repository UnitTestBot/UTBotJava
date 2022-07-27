package org.utbot.contest

import org.utbot.common.FileUtil
import org.utbot.framework.codegen.model.util.createTestClassName
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.jcdb.api.ClassId
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
    val kotlinClass
        get() = classLoader.loadClass(fqn).kotlin

    /**
     * Directory where this .class file is located without package folder structure
     * E.g. for gradle projects it's `project/build/classes`.
     */
    val classfileDir: File by lazy {
        (
                FileUtil.locateClassPath(kotlinClass)
                    ?: classfileDirSecondaryLocation
                    ?: FileUtil.isolateClassFiles(kotlinClass)
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


}