package org.utbot.python.newtyping.mypy

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.newtyping.general.TypeMetaDataWithName
import org.utbot.python.newtyping.pythonTypeRepresentation

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GlobalNamesStorageTest {
    lateinit var namesStorage: GlobalNamesStorage
    @BeforeAll
    fun setup() {
        val sample = MypyBuildKtTest::class.java.getResource("/import_test.json")!!.readText()
        namesStorage = GlobalNamesStorage(readMypyInfoBuildWithoutRoot(sample))
    }

    @Test
    fun testImportlib1() {
        val pathFinderClass = namesStorage.resolveTypeName("import_test", "im.PathFinder")
        assertTrue(pathFinderClass is UtType && (pathFinderClass.meta as TypeMetaDataWithName).name.name == "PathFinder")
    }

    @Test
    fun testImportlib2() {
        val pathFinderClass = namesStorage.resolveTypeName("import_test", "importlib.machinery.PathFinder")
        assertTrue(pathFinderClass is UtType && (pathFinderClass.meta as TypeMetaDataWithName).name.name == "PathFinder")
    }

    @Test
    fun testSimpleAsImport() {
        val deque = namesStorage.resolveTypeName("import_test", "c.deque")
        assertTrue(deque is UtType && deque.pythonTypeRepresentation().startsWith("collections.deque"))
    }

    @Test
    fun testImportFrom() {
        val deque = namesStorage.resolveTypeName("import_test", "deque")
        assertTrue(deque is UtType && deque.pythonTypeRepresentation().startsWith("collections.deque"))
    }

    @Test
    fun testLocal() {
        val classA = namesStorage.resolveTypeName("import_test", "A")
        assertTrue(classA is UtType && classA.pythonTypeRepresentation() == "import_test.A")
    }
}