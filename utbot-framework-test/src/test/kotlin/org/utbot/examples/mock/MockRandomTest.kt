package org.utbot.examples.mock

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.isParameter
import org.utbot.tests.infrastructure.mockValues
import org.utbot.tests.infrastructure.mocksMethod
import org.utbot.tests.infrastructure.singleMock
import org.utbot.tests.infrastructure.value
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtNewInstanceInstrumentation
import java.util.Random
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class MockRandomTest : UtValueTestCaseChecker(testClass = MockRandomExamples::class) {
    @Test
    fun testRandomAsParameter() {
        val method: Random.() -> Int = Random::nextInt
        checkMocks(
            MockRandomExamples::randomAsParameter,
            eq(3),
            { random, _, _, r -> random == null && r == null }, // NPE
            { random, threshold, mocks, r ->
                val mock = mocks.single()
                assert(mock.isParameter(1) && mock.mocksMethod(method))
                val nextInt = mock.value<Int>()

                random == null && nextInt > threshold && r == threshold + 1
            },
            { random, threshold, mocks, r ->
                val mock = mocks.single()
                assert(mock.isParameter(1) && mock.mocksMethod(method))
                val nextInt = mock.value<Int>()

                random == null && nextInt <= threshold && r == nextInt
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testRandomAsField() {
        val method: Random.() -> Int = Random::nextInt
        checkMocks(
            MockRandomExamples::randomAsField,
            eq(3),
            { _, _, r -> r == null }, // NPE
            { threshold, mocks, r ->
                val mock = mocks.singleMock("random", method)
                val nextInt = mock.value<Int>()

                nextInt > threshold && r == threshold + 1
            },
            { threshold, mocks, r ->
                val mock = mocks.singleMock("random", method)
                val nextInt = mock.value<Int>()

                nextInt <= threshold && r == nextInt
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testRandomAsLocalVariable() {
        checkMocksAndInstrumentation(
            MockRandomExamples::randomAsLocalVariable,
            eq(2),
            { _, instrumentation, r ->
                val mockInstances = instrumentation
                    .filterIsInstance<UtNewInstanceInstrumentation>()
                    .single { it.classId.name == "java.util.Random" }
                    .instances
                    //TODO: support any UtModel here after SAT-1135 is completed
                    .filterIsInstance<UtCompositeModel>()

                assert(mockInstances.size == 2)

                val firstMockValues = mockInstances[0].mockValues<Int>("nextInt")
                val secondMockValues = mockInstances[1].mockValues<Int>("nextInt")

                val sizes = firstMockValues.size == 2 && secondMockValues.size == 2
                val valueConstraint = firstMockValues[0] + firstMockValues[1] + secondMockValues[0] > 1000
                val resultConstraint = r == secondMockValues[1]

                sizes && valueConstraint && resultConstraint
            },
            { _, instrumentation, r ->
                val mockInstances = instrumentation
                    .filterIsInstance<UtNewInstanceInstrumentation>()
                    .single { it.classId.name == "java.util.Random" }
                    .instances
                    .filterIsInstance<UtCompositeModel>()

                assert(mockInstances.size == 3)

                val firstMockValues = mockInstances[0].mockValues<Int>("nextInt")
                val secondMockValues = mockInstances[1].mockValues<Int>("nextInt")
                val thirdMockValues = mockInstances[2].mockValues<Int>("nextInt")

                val sizes = firstMockValues.size == 2 && secondMockValues.size == 1 && thirdMockValues.size == 1
                val valueConstraint = firstMockValues[0] + firstMockValues[1] + secondMockValues[0] <= 1000
                val resultConstraint = r == thirdMockValues[0]

                sizes && valueConstraint && resultConstraint
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testUseSecureRandom() {
        checkMocksAndInstrumentation(
            MockRandomExamples::useSecureRandom,
            eq(2),
            { _, instrumentation, r ->
                val mock = instrumentation
                    .filterIsInstance<UtNewInstanceInstrumentation>()
                    .single { it.classId.name == "java.security.SecureRandom" }
                    .instances
                    .filterIsInstance<UtCompositeModel>()
                    .single()

                val values = mock.mockValues<Int>("nextInt")

                values.size == 1 && values[0] > 1000 && r == 1
            },
            { _, instrumentation, r ->
                val mock = instrumentation
                    .filterIsInstance<UtNewInstanceInstrumentation>()
                    .single { it.classId.name == "java.security.SecureRandom" }
                    .instances
                    .filterIsInstance<UtCompositeModel>()
                    .single()

                val values = mock.mockValues<Int>("nextInt")

                values.size == 2 && values[0] <= 1000 && r == values[1]
            },
            coverage = DoNotCalculate
        )
    }
}