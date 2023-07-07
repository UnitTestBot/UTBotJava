package org.utbot.examples.codegen.deepequals

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker

// TODO failed Kotlin compilation (generics) SAT-1332
class DeepEqualsTest : UtValueTestCaseChecker(
    testClass = DeepEqualsTestingClass::class,
    testCodeGeneration = true,
    configurations = ignoreKotlinCompilationConfigurations,
) {
    @Test
    fun testReturnList() {
        check(
            DeepEqualsTestingClass::returnList,
            eq(1),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testReturnSet() {
        check(
            DeepEqualsTestingClass::returnSet,
            eq(1),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testReturnMap() {
        check(
            DeepEqualsTestingClass::returnMap,
            eq(1),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testReturnArray() {
        check(
            DeepEqualsTestingClass::returnArray,
            eq(1),
            coverage = DoNotCalculate
        )
    }

    @Test
    @Disabled("We do not support 2d generics containers right now")
    fun testReturn2DList() {
        check(
            DeepEqualsTestingClass::return2DList,
            eq(1),
            coverage = DoNotCalculate
        )
    }

    @Test
    @Disabled("We do not support 2d generics containers right now")
    fun testReturn2DSet() {
        check(
            DeepEqualsTestingClass::return2DSet,
            eq(1),
            coverage = DoNotCalculate
        )
    }

    @Test
    @Disabled("We do not support 2d generics containers right now")
    fun testReturn2DMap() {
        check(
            DeepEqualsTestingClass::return2DMap,
            eq(1),
            coverage = DoNotCalculate
        )
    }

    @Test
    @Disabled("We do not support 2d generics containers right now")
    fun testIntegers2DList() {
        check(
            DeepEqualsTestingClass::returnIntegers2DList,
            eq(1),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testReturn2DArray() {
        check(
            DeepEqualsTestingClass::return2DArray,
            eq(1),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testReturnCommonClass() {
        check(
            DeepEqualsTestingClass::returnCommonClass,
            eq(1),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testTriangle() {
        check(
            DeepEqualsTestingClass::returnTriangle,
            eq(1),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testQuadrilateral() {
        check(
            DeepEqualsTestingClass::returnQuadrilateralFromNode,
            eq(1),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testIntMultiArray() {
        check(
            DeepEqualsTestingClass::fillIntMultiArrayWithConstValue,
            eq(3),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testDoubleMultiArray() {
        check(
            DeepEqualsTestingClass::fillDoubleMultiArrayWithConstValue,
            eq(3),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testIntegerWrapperMultiArray() {
        check(
            DeepEqualsTestingClass::fillIntegerWrapperMultiArrayWithConstValue,
            eq(3),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testDoubleWrapperMultiArray() {
        check(
            DeepEqualsTestingClass::fillDoubleWrapperMultiArrayWithConstValue,
            eq(3),
            coverage = DoNotCalculate
        )
    }
}