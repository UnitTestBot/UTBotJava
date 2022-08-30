package org.utbot.examples.mock

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.examples.mock.others.Generator
import org.utbot.tests.infrastructure.singleMock
import org.utbot.tests.infrastructure.singleMockOrNull
import org.utbot.tests.infrastructure.value
import org.utbot.framework.plugin.api.FieldMockTarget
import org.utbot.framework.plugin.api.MockInfo
import org.utbot.framework.plugin.api.MockStrategyApi.OTHER_PACKAGES
import kotlin.reflect.KClass
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.withoutConcrete

internal class MockStaticFieldExampleTest : UtValueTestCaseChecker(testClass = MockStaticFieldExample::class) {

    @Test
    fun testMockStaticField() {
        withoutConcrete { // TODO JIRA:1420
            checkMocks(
                MockStaticFieldExample::calculate,
                eq(4), // 2 NPE
                // NPE, privateGenerator is null
                { _, mocks, r ->
                    val privateGenerator = mocks.singleMockOrNull("privateGenerator", Generator::generateInt)
                    privateGenerator == null && r == null
                },
                // NPE, publicGenerator is null
                { _, mocks, r ->
                    val publicGenerator = mocks.singleMockOrNull("publicGenerator", Generator::generateInt)
                    publicGenerator == null && r == null
                },
                { threshold, mocks, r ->
                    val mock1 = mocks.singleMock("privateGenerator", Generator::generateInt)
                    val mock2 = mocks.singleMock("publicGenerator", Generator::generateInt)

                    val (index1, index2) = if (mock1.values.size > 1) 0 to 1 else 0 to 0

                    val value1 = mock1.value<Int>(index1)
                    val value2 = mock2.value<Int>(index2)

                    val firstMockConstraint = mock1.mocksStaticField(MockStaticFieldExample::class)
                    val secondMockConstraint = mock2.mocksStaticField(MockStaticFieldExample::class)
                    val resultConstraint = threshold < value1 + value2 && r == threshold

                    firstMockConstraint && secondMockConstraint && resultConstraint
                },
                { threshold, mocks, r ->
                    val mock1 = mocks.singleMock("privateGenerator", Generator::generateInt)
                    val mock2 = mocks.singleMock("publicGenerator", Generator::generateInt)

                    val (index1, index2) = if (mock1.values.size > 1) 0 to 1 else 0 to 0

                    val value1 = mock1.value<Int>(index1)
                    val value2 = mock2.value<Int>(index2)

                    val firstMockConstraint = mock1.mocksStaticField(MockStaticFieldExample::class)
                    val secondMockConstraint = mock2.mocksStaticField(MockStaticFieldExample::class)
                    val resultConstraint = threshold >= value1 + value2 && r == value1 + value2 + 1

                    firstMockConstraint && secondMockConstraint && resultConstraint
                },
                coverage = DoNotCalculate,
                mockStrategy = OTHER_PACKAGES
            )
        }
    }

    private fun MockInfo.mocksStaticField(kClass: KClass<*>) = when (val mock = mock) {
        is FieldMockTarget -> mock.ownerClassName == kClass.qualifiedName && mock.owner == null
        else -> false
    }
}