package org.utbot.python.newtyping

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.utbot.python.newtyping.general.DefaultSubstitutionProvider
import org.utbot.python.newtyping.general.TypeParameter

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PythonSubtypeCheckerTest {
    lateinit var storage: MypyAnnotationStorage
    @BeforeAll
    fun setup() {
        val sample = PythonSubtypeCheckerTest::class.java.getResource("/subtypes_sample.json")!!.readText()
        storage = readMypyAnnotationStorage(sample)
    }

    @Test
    fun testCustomProtocol() {
        val protocolP = storage.definitions["subtypes"]!!["P"]!!.annotation.asUtBotType
        assertTrue(protocolP.pythonDescription() is PythonProtocolDescription)
        val classS = storage.definitions["subtypes"]!!["S"]!!.annotation.asUtBotType
        assertTrue(classS.pythonDescription() is PythonConcreteCompositeTypeDescription)
        val classS1 = storage.definitions["subtypes"]!!["S1"]!!.annotation.asUtBotType
        assertTrue(classS1.pythonDescription() is PythonConcreteCompositeTypeDescription)
        assertTrue(
            PythonSubtypeChecker(
                left = protocolP,
                right = classS,
                emptyList()
            ).rightIsSubtypeOfLeft()
        )
        assertFalse(
            PythonSubtypeChecker(
                left = protocolP,
                right = classS1,
                emptyList()
            ).rightIsSubtypeOfLeft()
        )
    }

    @Test
    fun testVariance() {
        val list = storage.definitions["builtins"]!!["list"]!!.annotation.asUtBotType
        val frozenset = storage.definitions["builtins"]!!["frozenset"]!!.annotation.asUtBotType
        val int = storage.definitions["builtins"]!!["int"]!!.annotation.asUtBotType
        val obj = storage.definitions["builtins"]!!["object"]!!.annotation.asUtBotType
        val listOfInt = DefaultSubstitutionProvider.substitute(list,
            mapOf(list.parameters.first() as TypeParameter to int)
        )
        val listOfObj = DefaultSubstitutionProvider.substitute(list,
            mapOf(list.parameters.first() as TypeParameter to obj)
        )
        val listOfAny = DefaultSubstitutionProvider.substitute(list,
            mapOf(list.parameters.first() as TypeParameter to pythonAnyType)
        )
        val frozensetOfInt = DefaultSubstitutionProvider.substitute(frozenset,
            mapOf(frozenset.parameters.first() as TypeParameter to int)
        )
        val frozensetOfObj = DefaultSubstitutionProvider.substitute(frozenset,
            mapOf(frozenset.parameters.first() as TypeParameter to obj)
        )

        // list is invariant
        assertFalse(
            PythonSubtypeChecker(
                left = listOfObj,
                right = listOfInt,
                emptyList()
            ).rightIsSubtypeOfLeft()
        )
        assertTrue(
            PythonSubtypeChecker(
                left = listOfAny,
                right = listOfInt,
                emptyList()
            ).rightIsSubtypeOfLeft()
        )
        assertTrue(
            PythonSubtypeChecker(
                left = listOfInt,
                right = listOfAny,
                emptyList()
            ).rightIsSubtypeOfLeft()
        )

        // frozenset is covariant
        assertTrue(
            PythonSubtypeChecker(
                left = frozensetOfObj,
                right = frozensetOfInt,
                emptyList()
            ).rightIsSubtypeOfLeft()
        )
        assertFalse(
            PythonSubtypeChecker(
                left = frozensetOfInt,
                right = frozensetOfObj,
                emptyList()
            ).rightIsSubtypeOfLeft()
        )
    }
}