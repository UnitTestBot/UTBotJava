package org.utbot.examples.arrays

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.AtLeast
import org.utbot.tests.infrastructure.CodeGeneration
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.isException

class ArrayStoreExceptionExamplesTest : UtValueTestCaseChecker(
    testClass = ArrayStoreExceptionExamples::class,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        // Type inference errors in generated Kotlin code
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testCorrectAssignmentSamePrimitiveType() {
        checkWithException(
            ArrayStoreExceptionExamples::correctAssignmentSamePrimitiveType,
            eq(3),
            { data, result -> result.isSuccess && result.getOrNull() == data?.isNotEmpty() }
        )
    }

    @Test
    fun testCorrectAssignmentIntToIntegerArray() {
        checkWithException(
            ArrayStoreExceptionExamples::correctAssignmentIntToIntegerArray,
            eq(3),
            { data, result -> result.isSuccess && result.getOrNull() == data?.isNotEmpty() }
        )
    }

    @Test
    fun testCorrectAssignmentSubtype() {
        checkWithException(
            ArrayStoreExceptionExamples::correctAssignmentSubtype,
            eq(3),
            { data, result -> result.isSuccess && result.getOrNull() == data?.isNotEmpty() }
        )
    }

    @Test
    fun testCorrectAssignmentToObjectArray() {
        checkWithException(
            ArrayStoreExceptionExamples::correctAssignmentToObjectArray,
            eq(3),
            { data, result -> result.isSuccess && result.getOrNull() == data?.isNotEmpty() }
        )
    }

    @Test
    fun testWrongAssignmentUnrelatedType() {
        checkWithException(
            ArrayStoreExceptionExamples::wrongAssignmentUnrelatedType,
            eq(3),
            { data, result -> data == null && result.isSuccess },
            { data, result -> data.isEmpty() && result.isSuccess },
            { data, result -> data.isNotEmpty() && result.isException<ArrayStoreException>() },
            coverage = AtLeast(91) // TODO: investigate
        )
    }

    @Test
    fun testCheckGenericAssignmentWithCorrectCast() {
        checkWithException(
            ArrayStoreExceptionExamples::checkGenericAssignmentWithCorrectCast,
            eq(1),
            { result -> result.isSuccess }
        )
    }

    @Test
    fun testCheckGenericAssignmentWithWrongCast() {
        checkWithException(
            ArrayStoreExceptionExamples::checkGenericAssignmentWithWrongCast,
            eq(1),
            { result -> result.isException<ArrayStoreException>() },
            coverage = AtLeast(87) // TODO: investigate
        )
    }

    @Test
    fun testCheckGenericAssignmentWithExtendsSubtype() {
        checkWithException(
            ArrayStoreExceptionExamples::checkGenericAssignmentWithExtendsSubtype,
            eq(1),
            { result -> result.isSuccess }
        )
    }

    @Test
    fun testCheckGenericAssignmentWithExtendsUnrelated() {
        checkWithException(
            ArrayStoreExceptionExamples::checkGenericAssignmentWithExtendsUnrelated,
            eq(1),
            { result -> result.isException<ArrayStoreException>() },
            coverage = AtLeast(87) // TODO: investigate
        )
    }

    @Test
    fun testCheckObjectAssignment() {
        checkWithException(
            ArrayStoreExceptionExamples::checkObjectAssignment,
            eq(1),
            { result -> result.isSuccess }
        )
    }

    // Should this be allowed at all?
    @Test
    fun testCheckWrongAssignmentOfItself() {
        checkWithException(
            ArrayStoreExceptionExamples::checkWrongAssignmentOfItself,
            eq(1),
            { result -> result.isException<ArrayStoreException>() },
            coverage = AtLeast(87)
        )
    }

    @Test
    fun testCheckGoodAssignmentOfItself() {
        checkWithException(
            ArrayStoreExceptionExamples::checkGoodAssignmentOfItself,
            eq(1),
            { result -> result.isSuccess }
        )
    }

    @Test
    fun testCheckAssignmentToObjectArray() {
        checkWithException(
            ArrayStoreExceptionExamples::checkAssignmentToObjectArray,
            eq(1),
            { result -> result.isSuccess }
        )
    }

    @Test
    fun testArrayCopyForIncompatiblePrimitiveTypes() {
        checkWithException(
            ArrayStoreExceptionExamples::arrayCopyForIncompatiblePrimitiveTypes,
            eq(3),
            { data, result -> data == null && result.isSuccess && result.getOrNull() == null },
            { data, result -> data != null && data.isEmpty() && result.isSuccess && result.getOrNull()?.size == 0 },
            { data, result -> data != null && data.isNotEmpty() && result.isException<ArrayStoreException>() }
        )
    }

    @Test
    fun testFill2DPrimitiveArray() {
        checkWithException(
            ArrayStoreExceptionExamples::fill2DPrimitiveArray,
            eq(1),
            { result -> result.isSuccess }
        )
    }

    @Test
    fun testFillObjectArrayWithList() {
        check(
            ArrayStoreExceptionExamples::fillObjectArrayWithList,
            eq(2),
            { list, result -> list != null && result != null && result[0] != null },
            { list, result -> list == null && result == null }
        )
    }

    @Test
    fun testFillWithTreeSet() {
        check(
            ArrayStoreExceptionExamples::fillWithTreeSet,
            eq(2),
            { treeSet, result -> treeSet != null && result != null && result[0] != null },
            { treeSet, result -> treeSet == null && result == null }
        )
    }

    @Test
    fun testFillSomeInterfaceArrayWithSomeInterface() {
        check(
            ArrayStoreExceptionExamples::fillSomeInterfaceArrayWithSomeInterface,
            eq(2),
            { impl, result -> impl == null && result == null },
            { impl, result -> impl != null && result != null && result[0] != null }
        )
    }

    @Test
    @Disabled("TODO: Not null path is not found, need to investigate")
    fun testFillObjectArrayWithSomeInterface() {
        check(
            ArrayStoreExceptionExamples::fillObjectArrayWithSomeInterface,
            eq(2),
            { impl, result -> impl == null && result == null },
            { impl, result -> impl != null && result != null && result[0] != null }
        )
    }

    @Test
    fun testFillWithSomeImplementation() {
        check(
            ArrayStoreExceptionExamples::fillWithSomeImplementation,
            eq(2),
            { impl, result -> impl == null && result == null },
            { impl, result -> impl != null && result != null && result[0] != null }
        )
    }
}
