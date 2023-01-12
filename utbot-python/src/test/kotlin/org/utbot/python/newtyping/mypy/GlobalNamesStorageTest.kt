package org.utbot.python.newtyping.mypy

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.general.TypeMetaDataWithName
import org.utbot.python.newtyping.pythonTypeRepresentation

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GlobalNamesStorageTest {
    lateinit var namesStorage: GlobalNamesStorage
    @BeforeAll
    fun setup() {
        val sample = AnnotationFromMypyKtTest::class.java.getResource("/imports_sample.json")!!.readText()
        namesStorage = GlobalNamesStorage(readMypyAnnotationStorage(sample))
    }

    @Test
    fun testImportlib() {
        val pathFinderClass = namesStorage.resolveTypeName("import_test", "im.PathFinder")
        assertTrue(pathFinderClass is Type && (pathFinderClass.meta as TypeMetaDataWithName).name.name == "PathFinder")
    }

    @Test
    fun testSimpleAsImport() {
        val deque = namesStorage.resolveTypeName("import_test", "c.deque")
        assertTrue(deque is Type && deque.pythonTypeRepresentation().startsWith("collections.deque"))
    }

    @Test
    fun testImportFrom() {
        val deque = namesStorage.resolveTypeName("import_test", "deque")
        assertTrue(deque is Type && deque.pythonTypeRepresentation().startsWith("collections.deque"))
    }
}