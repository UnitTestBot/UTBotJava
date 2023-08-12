package org.utbot.python.newtyping

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.utbot.python.newtyping.general.DefaultSubstitutionProvider
import org.utbot.python.newtyping.mypy.MypyBuildKtTest
import org.utbot.python.newtyping.mypy.MypyInfoBuild
import org.utbot.python.newtyping.mypy.readMypyInfoBuildWithoutRoot

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PythonTypeConstraintPropagationKtTest {
    lateinit var storage: MypyInfoBuild
    lateinit var pythonTypeStorage: PythonTypeHintsStorage
    @BeforeAll
    fun setup() {
        val sample = this::class.java.getResource("/annotation_sample.json")!!.readText()
        storage = readMypyInfoBuildWithoutRoot(sample)
        pythonTypeStorage = PythonTypeHintsStorage.get(storage)
    }

    @Test
    fun testSimpleCompositeTypePropagation() {
        val dict = storage.definitions["builtins"]!!["dict"]!!.getUtBotType()
        val str = storage.definitions["builtins"]!!["str"]!!.getUtBotType()
        val int = storage.definitions["builtins"]!!["int"]!!.getUtBotType()
        val dictOfAny = DefaultSubstitutionProvider.substituteAll(dict, listOf(pythonAnyType, pythonAnyType))
        val dictOfStrToInt = DefaultSubstitutionProvider.substituteAll(dict, listOf(str, int))
        val constraint = TypeConstraint(dictOfStrToInt, ConstraintKind.LowerBound)
        val propagated = propagateConstraint(dictOfAny, constraint, pythonTypeStorage)
        assertTrue(propagated.size == 2)
        assertTrue(PythonTypeWrapperForEqualityCheck(propagated[0]!!.type) == PythonTypeWrapperForEqualityCheck(str))
        assertTrue(propagated[0]!!.kind == ConstraintKind.BothSided)
        assertTrue(PythonTypeWrapperForEqualityCheck(propagated[1]!!.type) == PythonTypeWrapperForEqualityCheck(int))
        assertTrue(propagated[1]!!.kind == ConstraintKind.BothSided)
    }
}