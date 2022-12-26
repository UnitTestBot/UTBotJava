package org.utbot.python.newtyping

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.utbot.python.newtyping.PythonSubtypeChecker.Companion.checkIfRightIsSubtypeOfLeft
import org.utbot.python.newtyping.general.DefaultSubstitutionProvider
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.general.TypeParameter

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PythonSubtypeCheckerTest {
    lateinit var storage: MypyAnnotationStorage
    lateinit var pythonTypeStorage: PythonTypeStorage
    @BeforeAll
    fun setup() {
        val sample = PythonSubtypeCheckerTest::class.java.getResource("/subtypes_sample.json")!!.readText()
        storage = readMypyAnnotationStorage(sample)
        pythonTypeStorage = PythonTypeStorage.get(storage)
    }

    @Test
    fun testCustomProtocol() {
        val protocolP = storage.definitions["subtypes"]!!["P"]!!.annotation.asUtBotType
        assertTrue(protocolP.pythonDescription() is PythonProtocolDescription)
        val classS = storage.definitions["subtypes"]!!["S"]!!.annotation.asUtBotType
        assertTrue(classS.pythonDescription() is PythonConcreteCompositeTypeDescription)
        val classS1 = storage.definitions["subtypes"]!!["S1"]!!.annotation.asUtBotType
        assertTrue(classS1.pythonDescription() is PythonConcreteCompositeTypeDescription)
        assertTrue(checkIfRightIsSubtypeOfLeft(protocolP, classS, pythonTypeStorage))
        assertFalse(checkIfRightIsSubtypeOfLeft(protocolP, classS1, pythonTypeStorage))
    }

    @Test
    fun testCustomProtocolWithPossibleInfiniteRecursion() {
        val protocolR = storage.definitions["subtypes"]!!["R"]!!.annotation.asUtBotType
        assertTrue(protocolR.pythonDescription() is PythonProtocolDescription)
        val classRImpl = storage.definitions["subtypes"]!!["RImpl"]!!.annotation.asUtBotType
        assertTrue(classRImpl.pythonDescription() is PythonConcreteCompositeTypeDescription)
        assertTrue(checkIfRightIsSubtypeOfLeft(protocolR, classRImpl, pythonTypeStorage))
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
        assertFalse(checkIfRightIsSubtypeOfLeft(listOfObj, listOfInt, pythonTypeStorage))
        assertTrue(checkIfRightIsSubtypeOfLeft(listOfAny, listOfInt, pythonTypeStorage))
        assertTrue(checkIfRightIsSubtypeOfLeft(listOfInt, listOfAny, pythonTypeStorage))

        // frozenset is covariant
        assertTrue(checkIfRightIsSubtypeOfLeft(frozensetOfObj, frozensetOfInt, pythonTypeStorage))
        assertFalse(checkIfRightIsSubtypeOfLeft(frozensetOfInt, frozensetOfObj, pythonTypeStorage))
    }

    @Test
    fun testSimpleFunctionWithVariables() {
        val b = storage.definitions["subtypes"]!!["b"]!!.annotation.asUtBotType
        val func = storage.definitions["subtypes"]!!["func_abs"]!!.annotation.asUtBotType as FunctionType

        assertTrue(checkIfRightIsSubtypeOfLeft(func.arguments[0], b, pythonTypeStorage))
    }
}