package org.utbot.examples.types

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.eq
import org.utbot.testing.CodeGeneration
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.atLeast

internal class PathDependentGenericsExampleTest : UtValueTestCaseChecker(
    testClass = PathDependentGenericsExample::class,
    pipelines = listOf(
        TestLastStage(CodegenLanguage.JAVA),
        TestLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testPathDependentGenerics() {
        check(
            PathDependentGenericsExample::pathDependentGenerics,
            eq(3),
            { elem, r -> elem is ClassWithOneGeneric<*> && r == 1 },
            { elem, r -> elem is ClassWithTwoGenerics<*, *> && r == 2 },
            { elem, r -> elem !is ClassWithOneGeneric<*> && elem !is ClassWithTwoGenerics<*, *> && r == 3 },
        )
    }

    @Test
    fun testFunctionWithSeveralTypeConstraintsForTheSameObject() {
        check(
            PathDependentGenericsExample::functionWithSeveralTypeConstraintsForTheSameObject,
            eq(2),
            { e, r -> e !is List<*> && r == 3 },
            { e, r -> e is List<*> && r == 1 },
            coverage = atLeast(89)
        )
    }
}