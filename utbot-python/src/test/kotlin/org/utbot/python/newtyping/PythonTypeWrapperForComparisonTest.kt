package org.utbot.python.newtyping

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.utbot.python.newtyping.general.DefaultSubstitutionProvider
import org.utbot.python.newtyping.general.TypeParameter

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PythonTypeWrapperForComparisonTest {
    lateinit var storage: MypyAnnotationStorage
    @BeforeAll
    fun setup() {
        val sample = AnnotationFromMypyKtTest::class.java.getResource("/annotation_sample.txt")!!.readText()
        storage = readMypyAnnotationStorage(sample)
    }

    @Test
    fun smokeTest() {
        val int = storage.definitions["builtins"]!!["int"]!!.annotation.asUtBotType
        val set = storage.definitions["builtins"]!!["set"]!!.annotation.asUtBotType

        assert(PythonTypeWrapperForComparison(int) == PythonTypeWrapperForComparison(int))
        assert(PythonTypeWrapperForComparison(int).hashCode() == PythonTypeWrapperForComparison(int).hashCode())
        assert(PythonTypeWrapperForComparison(int) != PythonTypeWrapperForComparison(set))
        assert(PythonTypeWrapperForComparison(int).hashCode() != PythonTypeWrapperForComparison(set).hashCode())
    }

    @Test
    fun testSubstitutions() {
        val int = storage.definitions["builtins"]!!["int"]!!.annotation.asUtBotType
        val set = storage.definitions["builtins"]!!["set"]!!.annotation.asUtBotType
        val set1 = DefaultSubstitutionProvider.substitute(set, emptyMap())
        val setOfInt = DefaultSubstitutionProvider.substitute(set, mapOf((set.parameters.first() as TypeParameter) to int))
        val setOfInt1 = DefaultSubstitutionProvider.substitute(set, mapOf((set.parameters.first() as TypeParameter) to int))

        assert(set != set1)
        assert(PythonTypeWrapperForComparison(set) == PythonTypeWrapperForComparison(set1))
        assert(PythonTypeWrapperForComparison(set).hashCode() == PythonTypeWrapperForComparison(set1).hashCode())

        assert(setOfInt != setOfInt1)
        assert(PythonTypeWrapperForComparison(setOfInt) == PythonTypeWrapperForComparison(setOfInt1))
        assert(PythonTypeWrapperForComparison(setOfInt).hashCode() == PythonTypeWrapperForComparison(setOfInt1).hashCode())
    }

    @Test
    fun testBuiltinsModule() {
        storage.definitions["builtins"]!!.forEach { (_, def) ->
            val type = def.annotation.asUtBotType
            val type1 = DefaultSubstitutionProvider.substitute(type, emptyMap())
            assert(type != type1)
            assert(PythonTypeWrapperForComparison(type) == PythonTypeWrapperForComparison(type1))
            assert(PythonTypeWrapperForComparison(type).hashCode() == PythonTypeWrapperForComparison(type1).hashCode())
        }
    }
}