package org.utbot.examples.objects

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.withProcessingAllClinitSectionsConcretely
import org.utbot.testcheckers.withoutProcessingClinitSections
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.atLeast

internal class ClassForTestClinitSectionsTest : UtValueTestCaseChecker(testClass = ClassForTestClinitSections::class) {
    @Test
    fun testClinitWithoutClinitAnalysis() {
        withoutProcessingClinitSections {
            check(
                ClassForTestClinitSections::resultDependingOnStaticSection,
                eq(2)
            )
        }
    }

    @Test
    fun testClinitWithClinitAnalysis() {
        check(
            ClassForTestClinitSections::resultDependingOnStaticSection,
            eq(1),
            coverage = atLeast(71)
        )
    }

    @Test
    fun testProcessConcretelyWithoutClinitAnalysis() {
        withoutProcessingClinitSections {
            withProcessingAllClinitSectionsConcretely {
                check(
                    ClassForTestClinitSections::resultDependingOnStaticSection,
                    eq(2)
                )
            }
        }
    }

    @Test
    fun testProcessClinitConcretely() {
        withProcessingAllClinitSectionsConcretely {
            check(
                ClassForTestClinitSections::resultDependingOnStaticSection,
                eq(1),
                coverage = atLeast(71)
            )
        }
    }
}

