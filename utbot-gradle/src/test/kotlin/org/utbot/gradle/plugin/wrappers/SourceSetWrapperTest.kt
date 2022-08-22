package org.utbot.gradle.plugin.wrappers

import org.gradle.api.Project
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.utbot.common.FileUtil.createNewFileWithParentDirectories
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.util.Snippet
import org.utbot.framework.util.compileClassFile
import org.utbot.gradle.plugin.buildProject
import org.utbot.gradle.plugin.extension.SarifGradleExtensionProvider
import org.utbot.gradle.plugin.mainSourceSet
import java.nio.file.Paths

class SourceSetWrapperTest {

    @Nested
    @DisplayName("targetClasses")
    inner class TargetClassesTest {
        @Test
        fun `should provide sarifProperties targetClasses`() {
            val project = buildProject()
            val classNames = listOf("com.TestClassA", "org.TestClassB")
            classNames.forEach { project.addEmptyClass(it) }

            Mockito.`when`(sarifPropertiesMock.targetClasses).thenReturn(classNames)
            Mockito.`when`(sarifPropertiesMock.codegenLanguage).thenReturn(CodegenLanguage.JAVA)
            Mockito.`when`(sarifPropertiesMock.generatedTestsRelativeRoot).thenReturn("test")
            Mockito.`when`(sarifPropertiesMock.sarifReportsRelativeRoot).thenReturn("sarif")
            Mockito.`when`(sarifPropertiesMock.testPrivateMethods).thenReturn(true)

            val gradleProject = GradleProjectWrapper(project, sarifPropertiesMock)
            val sourceSetWrapper = SourceSetWrapper(project.mainSourceSet, gradleProject)
            val classNamesActual = sourceSetWrapper.targetClasses.map { it.qualifiedName }.toSet()
            assertEquals(classNames.toSet(), classNamesActual)
        }

        @Test
        fun `should find all declared classes if sarifProperties targetClasses is empty`() {
            val project = buildProject()
            val classNames = listOf("com.TestClassA", "org.TestClassB")
            classNames.forEach { project.addEmptyClass(it) }

            Mockito.`when`(sarifPropertiesMock.targetClasses).thenReturn(listOf()) // empty
            Mockito.`when`(sarifPropertiesMock.codegenLanguage).thenReturn(CodegenLanguage.JAVA)
            Mockito.`when`(sarifPropertiesMock.generatedTestsRelativeRoot).thenReturn("test")
            Mockito.`when`(sarifPropertiesMock.sarifReportsRelativeRoot).thenReturn("sarif")
            Mockito.`when`(sarifPropertiesMock.testPrivateMethods).thenReturn(true)

            val gradleProject = GradleProjectWrapper(project, sarifPropertiesMock)
            val sourceSetWrapper = SourceSetWrapper(project.mainSourceSet, gradleProject)
            val classNamesActual = sourceSetWrapper.targetClasses.map { it.qualifiedName }.toSet()
            assertEquals(classNames.toSet(), classNamesActual)
        }
    }

    @Nested
    @DisplayName("findSourceCodeFile")
    inner class FindSourceCodeFileTest {
        @Test
        fun `should find source code file by given fqn`() {
            val project = buildProject()
            val className = "com.TestClassA"
            project.addEmptyClass(className, classFileNeeded = false)

            Mockito.`when`(sarifPropertiesMock.generatedTestsRelativeRoot).thenReturn("test")
            Mockito.`when`(sarifPropertiesMock.sarifReportsRelativeRoot).thenReturn("sarif")
            Mockito.`when`(sarifPropertiesMock.testPrivateMethods).thenReturn(true)

            val gradleProject = GradleProjectWrapper(project, sarifPropertiesMock)
            val sourceSetWrapper = SourceSetWrapper(project.mainSourceSet, gradleProject)
            assertNotNull(sourceSetWrapper.findSourceCodeFile(className))
        }

        @Test
        fun `should return null if class is not found`() {
            val project = buildProject()
            project.addEmptyClass(classFqn = "TestClass", classFileNeeded = false)

            Mockito.`when`(sarifPropertiesMock.generatedTestsRelativeRoot).thenReturn("test")
            Mockito.`when`(sarifPropertiesMock.sarifReportsRelativeRoot).thenReturn("sarif")
            Mockito.`when`(sarifPropertiesMock.testPrivateMethods).thenReturn(true)

            val gradleProject = GradleProjectWrapper(project, sarifPropertiesMock)
            val sourceSetWrapper = SourceSetWrapper(project.mainSourceSet, gradleProject)
            assertNull(sourceSetWrapper.findSourceCodeFile(classFqn = "AnotherClass"))
        }
    }

    // internal

    private val sarifPropertiesMock = Mockito.mock(SarifGradleExtensionProvider::class.java)

    private fun Project.addEmptyClass(classFqn: String, classFileNeeded: Boolean = true) {
        val className = classFqn.substringAfterLast('.')
        val classPackage = classFqn.substringBeforeLast('.').split('.').toTypedArray()
        val sourceCode = """
            package ${classPackage.joinToString(".")};

            public class $className {
                public void function() {}
            }
        """.trimIndent()
        this.addClass(classPackage, className, sourceCode, classFileNeeded)
    }

    /**
     * Creates a [className].java file with [sourceCode] in a special directory.
     * Compiles [sourceCode] and creates a [className].class file if [classFileNeeded] is true.
     */
    private fun Project.addClass(
        classPackage: Array<String>,
        className: String,
        sourceCode: String,
        classFileNeeded: Boolean
    ) {
        if (classFileNeeded) {
            // writing bytes of the compiled class to the needed file
            val classesDir = mainSourceSet.output.classesDirs.asPath
            val compiledClass = compileClassFile(className, Snippet(CodegenLanguage.JAVA, sourceCode)).readBytes()
            Paths.get(classesDir, *classPackage, "$className.class").toFile().apply {
                createNewFileWithParentDirectories()
                writeBytes(compiledClass)
            }
        }

        // writing the source code to the needed file
        val sourcesDir = mainSourceSet.java.srcDirs.first().path
        Paths.get(sourcesDir, *classPackage, "$className.java").toFile().apply {
            createNewFileWithParentDirectories()
            writeText(sourceCode)
        }
    }
}
