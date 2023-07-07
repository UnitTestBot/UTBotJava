package org.utbot.examples.stdlib

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.util.id
import org.utbot.testcheckers.ge
import org.utbot.testing.FullWithAssumptions
import org.utbot.testing.UtValueTestCaseChecker
import java.io.File

internal class StaticsPathDiversionTest : UtValueTestCaseChecker(
    testClass = StaticsPathDiversion::class,
) {
    @Test
    @Disabled("See https://github.com/UnitTestBot/UTBotJava/issues/716")
    fun testJavaIOFile() {
        // TODO Here we have a path diversion example - the static field `java.io.File#separator` is considered as not meaningful,
        //  so it is not passed to the concrete execution because of absence in the `stateBefore` models.
        //  So, the symbolic engine has 2 results - true and false, as expected, but the concrete executor may produce 1 or 2,
        //  depending on the model for the argument of the MUT produced by the solver.
        //  Such diversion was predicted to some extent - see `org.utbot.common.WorkaroundReason.IGNORE_STATICS_FROM_TRUSTED_LIBRARIES`
        //  and the corresponding issue https://github.com/UnitTestBot/UTBotJava/issues/716
        check(
            StaticsPathDiversion::separatorEquality,
            ge(2), // We cannot guarantee the exact number of branches without minimization

            // In the matchers below we check that the symbolic does not change the static field `File.separator` - we should
            // change the parameter, not the static field
            { s, separator -> separator == File.separator && s == separator },
            { s, separator -> separator == File.separator && s != separator },
            additionalMockAlwaysClasses = setOf(java.io.File::class.id), // From the use-case
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }
}
