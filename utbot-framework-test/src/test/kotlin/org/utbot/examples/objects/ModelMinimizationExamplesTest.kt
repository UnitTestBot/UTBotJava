package org.utbot.examples.objects

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.junit.Test
import org.utbot.testcheckers.eq

internal class ModelMinimizationExamplesTest : UtValueTestCaseChecker(testClass = ModelMinimizationExamples::class) {
    @Test
    fun singleValueComparisonTest() {
        check(
            ModelMinimizationExamples::singleValueComparison,
            eq(4),
            { quad, _ -> quad == null }, // NPE
            { quad, _ -> quad.a == null }, // NPE
            { quad, r -> quad.a.value == 0 && r == true },
            { quad, r -> quad.a.value != 0 && r == false }
        )
    }

    @Test
    fun singleValueComparisonNotNullTest() {
        check(
            ModelMinimizationExamples::singleValueComparisonNotNull,
            eq(2),
            { quad, r -> quad.a.value == 0 && r == true },
            { quad, r -> quad.a.value != 0 && r == false },
            coverage = DoNotCalculate // TODO: JIRA:1688
        )
    }
    
    @Test
    fun conditionCheckANeTest() {
        // Parameters `a` and `b` should be not null.
        // Parameters `a` and `b` should be distinct instances.
        // The field `a.value` is used and should be initialized.
        // The field `b.value` is not used and should not be initialized to avoid redundancy.
        check(
            ModelMinimizationExamples::conditionCheckANe,
            eq(3),
            { a, _, r -> a.value == 42 && r == true },
            { a, _, r -> a.value <= 0 && r == true },
            { a, _, r -> a.value > 0 && a.value != 42 && r == false},
            coverage = DoNotCalculate // TODO: JIRA:1688
        )
    }

    @Test
    fun conditionCheckAEqTest() {
        // Parameters `a` and `b` should be not null.
        // Parameters `a` and `b` should refer to the same instance.
        // The field `a.value` is used and should be initialized.
        // The field `b.value` is not used but should be implicitly initialized, as `b` is `a` restored from cache.
        check(
            ModelMinimizationExamples::conditionCheckAEq,
            eq(3),
            { a, _, r -> a.value == 42 && r == true },
            { a, _, r -> a.value <= 0 && r == true },
            { a, _, r -> a.value > 0 && a.value != 42 && r == false},
            coverage = DoNotCalculate // TODO: JIRA:1688
        )
    }

    @Test
    fun conditionCheckBNeTest() {
        // Parameters `a` and `b` should be not null.
        // Parameters `a` and `b` should be distinct instances.
        // The field `a.value` is not used and should not be initialized to avoid redundancy.
        // The field `b.value` is used and should be initialized.
        check(
            ModelMinimizationExamples::conditionCheckBNe,
            eq(3),
            { _, b, r -> b.value == 42 && r == true },
            { _, b, r -> b.value <= 0 && r == true },
            { _, b, r -> b.value > 0 && b.value != 42 && r == false},
            coverage = DoNotCalculate // TODO: JIRA:1688
        )
    }

    @Test
    fun conditionCheckBEqTest() {
        // Parameters `a` and `b` should be not null.
        // Parameters `a` and `b` should refer to the same instance.
        // The field `a.value` is not used but should be initialized, as `b.value` is used, and `a === b`.
        // The field `b.value` is used and should be initialized.
        // `a` should be initialized even if its model is created first and stored in the cache.
        // Note: `a` and `b` might have different `addr` but they will have the same `concreteAddr`.
        check(
            ModelMinimizationExamples::conditionCheckBEq,
            eq(3),
            { _, b, r -> b.value == 42 && r == true },
            { _, b, r -> b.value <= 0 && r == true },
            { _, b, r -> b.value > 0 && b.value != 42 && r == false},
            coverage = DoNotCalculate // TODO: JIRA:1688
        )
    }

    @Test
    fun conditionCheckNoNullabilityConstraintTest() {
        // Note: in this test we have no constraints on the second argument, so it becomes `null`.
        check(
            ModelMinimizationExamples::conditionCheckNoNullabilityConstraintExample,
            eq(4),
            { a, _, _ -> a == null }, // NPE
            { a, _, r -> a.value == 42 && r == true },
            { a, _, r -> a.value <= 0 && r == true },
            { a, _, r -> a.value > 0 && a.value != 42 && r == false}
        )
    }

    @Test
    fun firstArrayElementContainsSentinelTest() {
        check(
            ModelMinimizationExamples::firstArrayElementContainsSentinel,
            eq(2),
            { values, r -> values[0].value == 42 && r == true },
            { values, r -> values[0].value != 42 && r == false },
            coverage = DoNotCalculate // TODO: JIRA:1688
        )
    }

    @Test
    fun multipleConstraintsTest() {
        check(
            ModelMinimizationExamples::multipleConstraintsExample,
            eq(3),
            { a, _, _, r -> a.value == 42 && r == 1 },
            { a, b, _, r -> a.value != 42 && b.value == 73 && r == 2 },
            { a, b, _, r -> a.value != 42 && b.value != 73 && r == 3 },
            coverage = DoNotCalculate // TODO: JIRA:1688
        )
    }
}