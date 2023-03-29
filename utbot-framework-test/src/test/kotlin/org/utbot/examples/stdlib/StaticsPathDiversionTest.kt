package org.utbot.examples.stdlib

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.util.id
import org.utbot.testcheckers.ge
import org.utbot.testcheckers.withoutConcrete
import org.utbot.testing.AtLeast
import org.utbot.testing.Compilation
import org.utbot.testing.UtValueTestCaseChecker

class StaticsPathDiversionTest : UtValueTestCaseChecker(
    testClass = StaticsPathDiversion::class,
    testCodeGeneration = true,
    pipelines = listOf(
        TestLastStage(CodegenLanguage.JAVA, Compilation), // See the comment for the testJavaIOFile
        TestLastStage(CodegenLanguage.KOTLIN, Compilation),  // See the comment for the testJavaIOFile
    )
) {
    @Test
    fun testJavaIOFile() {
        // TODO Here we have a path diversion example - the static field `java.io.File#separator` is considered as not meaningful,
        //  so it is not passed to the concrete execution because of absence in the `stateBefore` models.
        //  So, the symbolic engine has 2 results - true and false, as expected, but the concrete executor may produce 1 or 2,
        //  depending on the model for the argument of the MUT produced by the solver.
        //  Such diversion was predicted to some extent - see `org.utbot.common.WorkaroundReason.IGNORE_STATICS_FROM_TRUSTED_LIBRARIES`
        //  and the corresponding issue https://github.com/UnitTestBot/UTBotJava/issues/716
        withoutConcrete {
            check(
                StaticsPathDiversion::separatorEquality,
                ge(2), // We cannot guarantee the exact number of branches without minimization
                { _, r -> r == true },
                { _, r -> r == false },
                additionalMockAlwaysClasses = setOf(java.io.File::class.id), // From the use-case
                coverage = AtLeast(78)
            )
        }
    }
}
