package org.utbot.examples.java11.collections

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.between
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.CodeGeneration
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.tests.infrastructure.isException

class ToArrayWithGeneratorTest : UtValueTestCaseChecker(
    testClass = ToArrayWithGenerator::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        // TODO: We are generating Kotlin code for generic collections that will not compile
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, lastStage = CodeGeneration)
)
) {
    @Test
    fun testCheckSetSize() {
        check(
            ToArrayWithGenerator<Int>::checkSetSize,
            ignoreExecutionsNumber,
            { size, result -> size < 0 && result == false },
            { size, result -> size >= 0 && result == true }
        )
    }

    @Test
    @Disabled("TODO: we can't yet model throwing ArrayStoreException")
    fun testCheckSetSizeArrayStoreException() {
        checkWithException(
            ToArrayWithGenerator<Int>::checkSetSizeArrayStoreException,
            eq(3),
            { size, result -> size < 0 && result.isSuccess && result.getOrNull() == false },
            { size, result -> size == 0 && result.isSuccess && result.getOrNull() == true },
            { size, result -> size > 0 && result.isException<ArrayStoreException>() }
        )
    }

    @Test
    fun testCheckListSize() {
        check(
            ToArrayWithGenerator<Int>::checkListSize,
            ignoreExecutionsNumber,
            { size, result -> size < 0 && result == false },
            { size, result -> size >= 0 && result == true }
        )
    }

    @Test
    fun testCheckMapKeysSize() {
        check(
            ToArrayWithGenerator<Int>::checkMapKeysSize,
            ignoreExecutionsNumber,
            { size, result -> size < 0 && result == false },
            { size, result -> size >= 0 && result == true }
        )
    }

    @Test
    fun testCheckMapValuesSize() {
        check(
            ToArrayWithGenerator<Int>::checkMapValuesSize,
            ignoreExecutionsNumber,
            { size, result -> size < 0 && result == false },
            { size, result -> size >= 0 && result == true }
        )
    }

    @Test
    @Disabled("TODO: we can't yet model throwing ArrayStoreException")
    fun testGetMapEntrySetArrayStoreException() {
        checkWithException(
            ToArrayWithGenerator<Int>::getMapEntrySetArrayStoreException,
            eq(1),
            { result -> result.isFailure && result.isException<ArrayStoreException>() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testGetMapEntrySetSize() {
        check(
            ToArrayWithGenerator<Int>::getMapEntrySetSize,
            eq(1),
            { result -> result == 2 }
        )
    }

    @Test
    fun testGetCollectionArgumentSize() {
        check(
            ToArrayWithGenerator<Int>::getCollectionArgumentSize,
            ignoreExecutionsNumber,
            { arg, result -> result == arg.size }
        )
    }

    @Test
    fun testSetCollectionArgumentSize() {
        check(
            ToArrayWithGenerator<Int>::getSetArgumentSize,
            ignoreExecutionsNumber,
            { arg, result -> result == arg.size }
        )
    }

    @Test
    fun testListCollectionArgumentSize() {
        check(
            ToArrayWithGenerator<Int>::getListArgumentSize,
            ignoreExecutionsNumber,
            { arg, result -> result == arg.size }
        )
    }

    @Test
    @Disabled("TODO: this test takes too long and results in non-instantiable concrete type substitutions")
    fun testGetAbstractCollectionArgumentSize() {
        check(
            ToArrayWithGenerator<Int>::getAbstractCollectionArgumentSize,
            ignoreExecutionsNumber,
            { arg, result -> result == arg.size }
        )
    }

    @Test
    @Disabled("TODO: translate of UtArrayApplyForAll expression (#630)")
    fun testGetGenericCollectionArgumentSize() {
        check(
            ToArrayWithGenerator<Int>::getGenericCollectionArgumentSize,
            ignoreExecutionsNumber,
            { arg, result -> result == arg.size },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCountMatchingElements() {
        check(
            ToArrayWithGenerator<Int>::countMatchingElements,
            between(3..4),
            { arg, result -> arg.isEmpty() && result == 0 },
            { arg, result -> arg.contains(null) && result == arg.size },
            { arg, result -> arg.isNotEmpty() && !arg.contains(null) && result == arg.size },
            coverage = DoNotCalculate // TODO: investigate the incomplete coverage
        )
    }
}
