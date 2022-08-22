package org.utbot.examples

import ClassWithoutPackage
import org.utbot.examples.samples.ClassWithInnerClasses
import org.utbot.examples.samples.ExampleClass
import org.utbot.examples.samples.wrongpackage.ClassWithWrongPackage
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.instrumentation.instrumentation.instrumenter.Instrumenter
import java.nio.file.Paths
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TestGetSourceFileName {
    private lateinit var cookie: AutoCloseable

    @BeforeEach
    fun setup() {
        cookie = UtContext.setUtContext(UtContext(ClassLoader.getSystemClassLoader()))
    }

    @AfterEach
    fun tearDown() {
        cookie.close()
    }

    @Test
    fun testThis() {
        assertEquals("TestGetSourceFileName.kt", Instrumenter.computeSourceFileName(TestGetSourceFileName::class.java))
    }

    @Test
    fun testJavaExample1() {
        assertEquals("ExampleClass.java", Instrumenter.computeSourceFileName(ExampleClass::class.java))
    }

    @Test
    fun testJavaExample2() {
        assertEquals(
            "ClassWithInnerClasses.java",
            Instrumenter.computeSourceFileName(ClassWithInnerClasses::class.java)
        )
    }

    @Test
    fun testInnerClass() {
        assertEquals(
            "ClassWithInnerClasses.java",
            Instrumenter.computeSourceFileName(ClassWithInnerClasses.InnerStaticClass::class.java)
        )
    }

    @Test
    fun testSameNameButDifferentPackages() {
        assertEquals(
            true,
            Instrumenter.computeSourceFileByClass(org.utbot.examples.samples.root.MyClass::class.java)?.toPath()
                ?.endsWith(Paths.get("root", "MyClass.java"))
        )
        assertEquals(
            true,
            Instrumenter.computeSourceFileByClass(org.utbot.examples.samples.root.child.MyClass::class.java)
                ?.toPath()?.endsWith(Paths.get("root", "child", "MyClass.java"))
        )
    }

    @Test
    fun testEmptyPackage() {
        assertEquals(
            true,
            Instrumenter.computeSourceFileByClass(ClassWithoutPackage::class.java)?.toPath()
                ?.endsWith("java/ClassWithoutPackage.java")
        )
    }

    @Test
    fun testPackageDoesNotMatchDir() {
        assertEquals(
            true,
            Instrumenter.computeSourceFileByClass(ClassWithWrongPackage::class.java)?.toPath()
                ?.endsWith("org/utbot/examples/samples/ClassWithWrongPackage.kt")
        )
    }

    @Test
    fun testSearchDir() {
        assertEquals(
            null,
            Instrumenter.computeSourceFileByClass(
                org.utbot.examples.samples.root.MyClass::class.java,
                Paths.get("src/test/kotlin")
            )?.name
        )

        assertEquals(
            "MyClass.java",
            Instrumenter.computeSourceFileByClass(
                org.utbot.examples.samples.root.MyClass::class.java,
                Paths.get("src/test")
            )?.name
        )
    }
}