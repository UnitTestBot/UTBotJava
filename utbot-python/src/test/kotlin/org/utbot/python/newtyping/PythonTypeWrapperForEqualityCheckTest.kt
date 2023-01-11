package org.utbot.python.newtyping

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.utbot.python.newtyping.general.DefaultSubstitutionProvider
import org.utbot.python.newtyping.general.TypeParameter
import org.utbot.python.newtyping.mypy.MypyAnnotationStorage
import org.utbot.python.newtyping.mypy.readMypyAnnotationStorage

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PythonTypeWrapperForEqualityCheckTest {
    lateinit var storage: MypyAnnotationStorage
    @BeforeAll
    fun setup() {
        val sample = AnnotationFromMypyKtTest::class.java.getResource("/annotation_sample.json")!!.readText()
        storage = readMypyAnnotationStorage(sample)
    }

    @Test
    fun smokeTest() {
        val int = storage.definitions["builtins"]!!["int"]!!.annotation.asUtBotType
        val set = storage.definitions["builtins"]!!["set"]!!.annotation.asUtBotType

        assert(PythonTypeWrapperForEqualityCheck(int) == PythonTypeWrapperForEqualityCheck(int))
        assert(PythonTypeWrapperForEqualityCheck(int).hashCode() == PythonTypeWrapperForEqualityCheck(int).hashCode())
        assert(PythonTypeWrapperForEqualityCheck(int) != PythonTypeWrapperForEqualityCheck(set))
        assert(PythonTypeWrapperForEqualityCheck(int).hashCode() != PythonTypeWrapperForEqualityCheck(set).hashCode())
    }

    @Test
    fun testSubstitutions() {
        val int = storage.definitions["builtins"]!!["int"]!!.annotation.asUtBotType
        val set = storage.definitions["builtins"]!!["set"]!!.annotation.asUtBotType
        val set1 = DefaultSubstitutionProvider.substitute(set, emptyMap())
        val setOfInt = DefaultSubstitutionProvider.substitute(set, mapOf((set.parameters.first() as TypeParameter) to int))
        val setOfInt1 = DefaultSubstitutionProvider.substitute(set, mapOf((set.parameters.first() as TypeParameter) to int))

        assert(set != set1)
        assert(PythonTypeWrapperForEqualityCheck(set) == PythonTypeWrapperForEqualityCheck(set1))
        assert(PythonTypeWrapperForEqualityCheck(set).hashCode() == PythonTypeWrapperForEqualityCheck(set1).hashCode())

        assert(setOfInt != setOfInt1)
        assert(PythonTypeWrapperForEqualityCheck(setOfInt) == PythonTypeWrapperForEqualityCheck(setOfInt1))
        assert(PythonTypeWrapperForEqualityCheck(setOfInt).hashCode() == PythonTypeWrapperForEqualityCheck(setOfInt1).hashCode())
    }

    @Test
    fun testBuiltinsModule() {
        storage.definitions["builtins"]!!.forEach { (_, def) ->
            val type = def.annotation.asUtBotType
            val type1 = DefaultSubstitutionProvider.substitute(type, emptyMap())
            assert(type != type1)
            assert(PythonTypeWrapperForEqualityCheck(type) == PythonTypeWrapperForEqualityCheck(type1))
            assert(PythonTypeWrapperForEqualityCheck(type).hashCode() == PythonTypeWrapperForEqualityCheck(type1).hashCode())
        }
    }
}