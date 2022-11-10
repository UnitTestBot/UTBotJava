package org.utbot.examples.mixed

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.eq

// TODO parameterized tests disabled due to https://github.com/UnitTestBot/UTBotJava/issues/1266
internal class PrivateConstructorExampleTest : UtValueTestCaseChecker(
    testClass = PrivateConstructorExample::class,
    pipelines = listOf(
        TestLastStage(
            CodegenLanguage.JAVA,
            parameterizedModeLastStage = Compilation
        ),
        TestLastStage(
            CodegenLanguage.KOTLIN,
            parameterizedModeLastStage = Compilation
        )
    )
) {

    /**
     * Two branches need to be covered:
     * 1. argument must be <= a - b,
     * 2. argument must be > a - b
     *
     * a and b are fields of the class under test
     */
    @Test
    fun testLimitedSub() {
        checkWithThis(
            PrivateConstructorExample::limitedSub,
            eq(2),
            { caller, limit, r -> caller.a - caller.b >= limit && r == caller.a - caller.b },
            { caller, limit, r -> caller.a - caller.b < limit && r == limit },
            coverage = DoNotCalculate // TODO: Method coverage with `this` parameter isn't supported
        )
    }
}
