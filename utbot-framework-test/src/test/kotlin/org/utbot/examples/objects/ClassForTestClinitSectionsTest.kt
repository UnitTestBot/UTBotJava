package org.utbot.examples.objects

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.withProcessingAllClinitSectionsConcretely
import org.utbot.testcheckers.withoutConcrete
import org.utbot.testcheckers.withProcessingClinitSections
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.atLeast

internal class ClassForTestClinitSectionsTest : UtValueTestCaseChecker(testClass = ClassForTestClinitSections::class) {
    @Test
    fun testClinitWithoutClinitAnalysis() {
        withoutConcrete {
            withProcessingClinitSections(value = false) {
                check(
                    ClassForTestClinitSections::resultDependingOnStaticSection,
                    eq(2),
                    { r -> r == -1 },
                    { r -> r == 1 }
                )
            }
        }
    }

    @Test
    fun testClinitWithClinitAnalysis() {
        withoutConcrete {
            check(
                ClassForTestClinitSections::resultDependingOnStaticSection,
                eq(2),
                { r -> r == -1 },
                { r -> r == 1 }
            )
        }
    }

    @Test
    fun testProcessConcretelyWithoutClinitAnalysis() {
        withoutConcrete {
            withProcessingClinitSections(value = false) {
                withProcessingAllClinitSectionsConcretely(value = true) {
                    check(
                        ClassForTestClinitSections::resultDependingOnStaticSection,
                        eq(2),
                        { r -> r == -1 },
                        { r -> r == 1 }
                    )
                }
            }
        }
    }

    @Test
    fun testProcessClinitConcretely() {
        withoutConcrete {
            withProcessingAllClinitSectionsConcretely(value = true) {
                check(
                    ClassForTestClinitSections::resultDependingOnStaticSection,
                    eq(1),
                    { r -> r == -1 },
                    coverage = atLeast(71)
                )
            }
        }
    }
}

