package org.utbot.examples.codegen

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.UtValueTestCaseChecker

internal class FileWithTopLevelFunctionsTest : UtValueTestCaseChecker(testClass = FileWithTopLevelFunctionsReflectHelper.clazz.kotlin) {
    @Test
    fun topLevelSumTest() {
        check(
            ::topLevelSum,
            eq(1),
        )
    }

    @Test
    fun extensionOnBasicTypeTest() {
        check(
            Int::extensionOnBasicType,
            eq(1),
        )
    }

    @Test
    fun extensionOnCustomClassTest() {
        check(
            CustomClass::extensionOnCustomClass,
            eq(3),
            additionalDependencies = dependenciesForClassExtensions
        )
    }

    companion object {
        // Compilation of extension methods for ref objects produces call to
        // `kotlin.jvm.internal.Intrinsics::checkNotNullParameter`, so we need to add it to dependencies
        val dependenciesForClassExtensions = arrayOf<Class<*>>(kotlin.jvm.internal.Intrinsics::class.java)
    }
}