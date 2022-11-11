package org.utbot.python.newtyping

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.utbot.python.newtyping.general.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AnnotationFromMypyKtTest {
    lateinit var storage: MypyAnnotationStorage
    @BeforeAll
    fun setup() {
        val sample = AnnotationFromMypyKtTest::class.java.getResource("/annotation_sample.txt")!!.readText()
        storage = readMypyAnnotationStorage(sample)
    }

    @Test
    fun testDefinitions() {
        val int = storage.definitions["builtins"]!!["int"]!!.annotation.asUtBotType as CompositeType
        assertTrue(
            int.getPythonAttributes().map { it.name }.containsAll(
                listOf("__add__", "__sub__", "__pow__", "__abs__", "__or__", "__eq__")
            )
        )

        val set = storage.definitions["builtins"]!!["set"]!!.annotation.asUtBotType as CompositeType
        assertTrue(
            set.getPythonAttributes().map { it.name }.containsAll(
                listOf("add", "copy", "difference", "intersection", "remove", "union")
            )
        )
    }

    @Test
    fun testUnionMethodOfSet() {
        val set = storage.definitions["builtins"]!!["set"]!!.annotation.asUtBotType as CompositeType
        val unionMethod = set.getPythonAttributes().find { it.name == "__or__" }!!.type as FunctionType
        assertTrue(unionMethod.parameters.size == 1)

        val setOfUnion = unionMethod.returnValue as CompositeType
        assertTrue(setOfUnion.getPythonAttributes().find { it.name == "__or__" }!!.type.parameters.size == 1)

        val unionType = setOfUnion.parameters[0] as StatefulType
        assert(unionType.name == pythonUnionName)

        val s = unionType.members[1] as TypeParameter
        val paramOfUnionMethod = setOfUnion.getPythonAttributes().find { it.name == "__or__" }!!.type.parameters[0] as TypeParameter
        assertTrue(s != paramOfUnionMethod)
    }

    @Test
    fun testSubstitution() {
        val set = storage.definitions["builtins"]!!["set"]!!.annotation.asUtBotType as CompositeType
        val int = storage.definitions["builtins"]!!["int"]!!.annotation.asUtBotType as CompositeType
        val setOfInts = DefaultSubstitutionProvider.substitute(
            set,
            mapOf((set.parameters.first() as TypeParameter) to int)
        ) as CompositeType
        assertTrue(setOfInts.meta is PythonConcreteCompositeTypeDescription)
        assertTrue((setOfInts.getPythonAttributes().find { it.name == "add" }!!.type as FunctionType).arguments[1] == int)
    }

    @Test
    fun testUserClass() {
        val classA = storage.definitions["annotation_tests"]!!["A"]!!.annotation.asUtBotType as CompositeType
        assertTrue(classA.parameters.size == 1)
        assertTrue((classA.parameters[0] as TypeParameter).constraints.size == 3)
        assertTrue((classA.parameters[0] as TypeParameter).definedAt === classA)
        assertTrue(
            (classA.parameters[0] as TypeParameter).constraints.any {
                (it.boundary as? CompositeType)?.name == classA.name && it.relation == exactTypeRelation
            }
        )
    }

    @Test
    fun testUserFunction() {
        val int = storage.definitions["builtins"]!!["int"]!!.annotation.asUtBotType as CompositeType
        val square = storage.definitions["annotation_tests"]!!["square"]!!.annotation.asUtBotType as FunctionType
        assertTrue((square.arguments[0].parameters[0] as CompositeType).name == int.name)
    }
}