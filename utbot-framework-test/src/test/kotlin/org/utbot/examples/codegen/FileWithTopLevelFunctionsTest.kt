package org.utbot.examples.codegen

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.UtValueTestCaseChecker
import kotlin.reflect.KFunction3

@Suppress("UNCHECKED_CAST")
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
            // NB: cast is important here because we need to treat receiver as an argument to be able to check its content in matchers
            CustomClass::extensionOnCustomClass as KFunction3<*, CustomClass, CustomClass, Boolean>,
            eq(2),
            { receiver, argument, result -> receiver === argument && result == true },
            { receiver, argument, result -> receiver !== argument && result == false },
            additionalDependencies = dependenciesForClassExtensions
        )
    }

    companion object {
        // Compilation of extension methods for ref objects produces call to
        // `kotlin.jvm.internal.Intrinsics::checkNotNullParameter`, so we need to add it to dependencies
        val dependenciesForClassExtensions = arrayOf<Class<*>>(kotlin.jvm.internal.Intrinsics::class.java)
    }
}