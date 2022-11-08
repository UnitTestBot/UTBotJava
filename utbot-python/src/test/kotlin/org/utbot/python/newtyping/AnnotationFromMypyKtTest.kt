package org.utbot.python.newtyping

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.utbot.python.newtyping.general.StatefulType
import org.utbot.python.newtyping.general.TypeParameter

internal class AnnotationFromMypyKtTest {
    @Test
    fun smokeTest() {
        val sample = AnnotationFromMypyKtTest::class.java.getResource("/annotation_sample.txt")!!.readText()
        val storage = readMypyAnnotationStorage(sample)

        val int = storage.definitions["builtins"]!!["int"]!!.annotation.asUtBotType as PythonConcreteCompositeType
        assertTrue(
            int.memberNames.containsAll(
                listOf("__add__", "__sub__", "__pow__", "__abs__", "__or__", "__eq__")
            )
        )

        val set = storage.definitions["builtins"]!!["set"]!!.annotation.asUtBotType as PythonConcreteCompositeType
        assertTrue(
            set.memberNames.containsAll(
                listOf("add", "copy", "difference", "intersection", "remove", "union")
            )
        )

        val unionMethod = set.namedMembers.find { it.name == "__or__" }!!.type as PythonCallable
        assertTrue(unionMethod.parameters.size == 1)

        val setOfUnion = unionMethod.returnValue as PythonConcreteCompositeType
        assertTrue(setOfUnion.namedMembers.find { it.name == "__or__" }!!.type.parameters.size == 1)
        val unionType = setOfUnion.parameters[0] as StatefulType
        assert(unionType.name == pythonUnionName)
        val s = unionType.members[1] as TypeParameter
        assertTrue(setOfUnion.namedMembers.find { it.name == "__or__" }!!.type.parameters[0] is TypeParameter)
        assertTrue(s != setOfUnion.namedMembers.find { it.name == "__or__" }!!.type.parameters[0])

        val setOfInts = PythonTypeSubstitutionProvider.substitute(
            set,
            mapOf((set.parameters.first() as TypeParameter) to int)
        ) as PythonConcreteCompositeType

        assertTrue((setOfInts.namedMembers.find { it.name == "add" }!!.type as PythonCallable).arguments[1] == int)

        val classA = storage.definitions["annotation_tests"]!!["A"]!!.annotation.asUtBotType as PythonCompositeType
        assertTrue(classA.parameters.size == 1)
        assertTrue((classA.parameters[0] as TypeParameter).constraints.size == 3)
        assertTrue((classA.parameters[0] as TypeParameter).definedAt === classA)
        assertTrue(
            (classA.parameters[0] as TypeParameter).constraints.any {
                (it.boundary as? PythonCompositeType)?.name == classA.name && it.relation == exactTypeRelation
            }
        )

        val square = storage.definitions["annotation_tests"]!!["square"]!!.annotation.asUtBotType as PythonCallable
        assertTrue((square.arguments[0].parameters[0] as PythonCompositeType).name == int.name)
    }
}