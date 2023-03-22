package org.utbot.python.newtyping.mypy

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.general.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MypyStorageKtTest {
    lateinit var storage: MypyAnnotationStorage
    lateinit var typeStorage: PythonTypeStorage
    lateinit var storageBoruvka: MypyAnnotationStorage
    @BeforeAll
    fun setup() {
        val sample = MypyStorageKtTest::class.java.getResource("/annotation_sample.json")!!.readText()
        storage = readMypyAnnotationStorage(sample)
        typeStorage = PythonTypeStorage.get(storage)
        val sample1 = MypyStorageKtTest::class.java.getResource("/boruvka.json")!!.readText()
        storageBoruvka = readMypyAnnotationStorage(sample1)
    }

    @Test
    fun testDefinitions() {
        val int = storage.definitions["builtins"]!!["int"]!!.getUtBotType() as CompositeType
        assertTrue(
            int.getPythonAttributes().map { it.meta.name }.containsAll(
                listOf("__add__", "__sub__", "__pow__", "__abs__", "__or__", "__eq__")
            )
        )

        val set = storage.definitions["builtins"]!!["set"]!!.getUtBotType() as CompositeType
        assertTrue(
            set.getPythonAttributes().map { it.meta.name }.containsAll(
                listOf("add", "copy", "difference", "intersection", "remove", "union")
            )
        )
    }

    @Test
    fun testUnionMethodOfSet() {
        val set = storage.definitions["builtins"]!!["set"]!!.getUtBotType() as CompositeType
        val unionMethod = set.getPythonAttributes().find { it.meta.name == "__or__" }!!.type as FunctionType
        assertTrue(unionMethod.parameters.size == 1)

        val setOfUnion = unionMethod.returnValue as CompositeType
        assertTrue(setOfUnion.getPythonAttributes().find { it.meta.name == "__or__" }!!.type.parameters.size == 1)

        val unionType = setOfUnion.parameters[0]
        assert(unionType.pythonDescription().name == pythonUnionName)

        val s = unionType.parameters[1] as TypeParameter
        val paramOfUnionMethod = setOfUnion.getPythonAttributes().find { it.meta.name == "__or__" }!!.type.parameters[0] as TypeParameter
        assertTrue(s != paramOfUnionMethod)
    }

    @Test
    fun testSubstitution() {
        val set = storage.definitions["builtins"]!!["set"]!!.getUtBotType() as CompositeType
        val int = storage.definitions["builtins"]!!["int"]!!.getUtBotType() as CompositeType
        val setOfInts = DefaultSubstitutionProvider.substitute(
            set,
            mapOf((set.parameters.first() as TypeParameter) to int)
        ) as CompositeType
        assertTrue(setOfInts.meta is PythonConcreteCompositeTypeDescription)
        assertTrue((setOfInts.getPythonAttributes().find { it.meta.name == "add" }!!.type as FunctionType).arguments[1] == int)
    }

    @Test
    fun testSubstitution2() {
        val counter = storage.definitions["collections"]!!["Counter"]!!.getUtBotType() as CompositeType
        val int = storage.definitions["builtins"]!!["int"]!!.getUtBotType() as CompositeType
        val counterOfInt = DefaultSubstitutionProvider.substituteByIndex(counter, 0, int)
        val subtract = counterOfInt.getPythonAttributeByName(typeStorage, "subtract")!!.type.parameters[2] as FunctionType
        val iterable = storage.definitions["typing"]!!["Iterable"]!!.getUtBotType()
        val iterableOfInt = DefaultSubstitutionProvider.substituteByIndex(iterable, 0, int)
        assertTrue(typesAreEqual(subtract.arguments.last(), iterableOfInt))
    }

    @Test
    fun testUserClass() {
        val classA = storage.definitions["annotation_tests"]!!["A"]!!.getUtBotType() as CompositeType
        assertTrue(classA.parameters.size == 1)
        assertTrue((classA.parameters[0] as TypeParameter).constraints.size == 2)
        assertTrue((classA.parameters[0] as TypeParameter).definedAt === classA)
        assertTrue(
            (classA.parameters[0] as TypeParameter).constraints.any {
                it.boundary.pythonDescription().name == classA.pythonDescription().name && it.relation == exactTypeRelation
            }
        )
        assertTrue(
            (classA.parameters[0] as TypeParameter).constraints.all {
                it.relation == exactTypeRelation
            }
        )
    }

    @Test
    fun testUserFunction() {
        val int = storage.definitions["builtins"]!!["int"]!!.getUtBotType() as CompositeType
        val square = storage.definitions["annotation_tests"]!!["square"]!!.getUtBotType() as FunctionType
        assertTrue(square.arguments[0].parameters[0].pythonDescription().name == int.pythonDescription().name)
    }

    @Test
    fun initializeAllTypes() {
        storage.definitions.forEach { (_, contents) ->
            contents.forEach { (_, annotation) ->
                assert(annotation.getUtBotType().isPythonType())
            }
        }
    }

    @Test
    fun testIncludedDefinitions() {
        val defs = storage.definitions["annotation_tests"]!!.keys
        assertTrue(listOf("Optional", "collections", "Enum", "Iterable", "list", "int").all { !defs.contains(it) })
        assertTrue(listOf("sequence", "enum_literal", "Color", "A", "tuple_").all { defs.contains(it) })
    }

    @Test
    fun testFunctionArgNames() {
        val square = storage.definitions["annotation_tests"]!!["square"]!!.getUtBotType()
        assertTrue(
            (square.pythonDescription() as PythonCallableTypeDescription).argumentNames == listOf("collection", "x")
        )
    }

    @Test
    fun testCustomClassAttributes() {
        val A = storage.definitions["annotation_tests"]!!["A"]!!.getUtBotType()
        val attrs = A.getPythonAttributes().map { it.meta.name }
        assertTrue(attrs.containsAll(listOf("y", "x", "self_")))
    }

    @Test
    fun testTypeAlias() {
        val isinstance = storageBoruvka.types["boruvka"]!!.first { it.line == 96L && it.type.asUtBotType is FunctionType }.type.asUtBotType
        val func = isinstance as FunctionType
        val classInfo = func.arguments[1]
        assertTrue(classInfo.pythonDescription() is PythonTypeAliasDescription)
    }
}