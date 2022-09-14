package org.utbot.bytecode.versions

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.examples.objects.SimpleDataClass
import org.utbot.framework.util.SootUtils.runSoot
import soot.Scene

@Suppress("UNREACHABLE_CODE")
@Disabled("TODO: https://github.com/UnitTestBot/UTBotJava/issues/891")
class SootTest {
    @Test
    fun `no method isBlank in JDK 8`() {
        runSoot(
            SimpleDataClass::class.java,
            forceReload = true,
            TODO("Get JDK 8")
        )

        val stringClass = Scene.v().getSootClass("java.lang.String")
        assertFalse(stringClass.isPhantomClass)

        val isBlankMethod = stringClass.getMethodByNameUnsafe("isBlank") // no such method in JDK 8
        assertNull(isBlankMethod)
    }

    @Test
    fun `method isBlank exists in JDK 11`() {
        runSoot(
            SimpleDataClass::class.java,
            forceReload = true,
            TODO("Get JDK 11")
        )

        val stringClass = Scene.v().getSootClass("java.lang.String")
        assertFalse(stringClass.isPhantomClass)

        val isBlankMethod = stringClass.getMethodByNameUnsafe("isBlank") // there is such method in JDK 11
        assertNotNull(isBlankMethod)
    }

    @Test
    fun `no records in JDK 11`() {
        runSoot(
            SimpleDataClass::class.java,
            forceReload = true,
            TODO("Get JDK 11")
        )

        val stringClass = Scene.v().getSootClass("java.lang.Record") // must not exist in JDK 11
        assertTrue(stringClass.isPhantomClass)
    }

    @Test
    fun `records exists in JDK 17`() {
        runSoot(
            SimpleDataClass::class.java,
            forceReload = true,
            TODO("Get JDK 17")
        )

        val stringClass = Scene.v().getSootClass("java.lang.Record") // must exist in JDK 17
        assertFalse(stringClass.isPhantomClass)
    }
}