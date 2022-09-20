package org.utbot.examples.mock

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.isException
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.UtConcreteValue
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class UseNetworkTest : UtValueTestCaseChecker(testClass = UseNetwork::class) {
    @Test
    fun testReadBytes() {
        val method = UseNetwork::readBytes
        checkStaticMethodWithException(
            method,
            eq(5),
            { _, network, r -> network == null && r.isException<NullPointerException>() },
            { _, _, r -> r.getOrNull() == 0 },
            { pkg, _, r -> pkg == null && r.isException<NullPointerException>() },
            { pkg, _, r -> pkg.isEmpty() && r.isException<IndexOutOfBoundsException>() },
            { pkg, _, r -> pkg.isNotEmpty() && (r.isException<IndexOutOfBoundsException>() || r.getOrNull()!! > 0) },
            mockStrategy = MockStrategyApi.OTHER_PACKAGES,
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testReadBytesWithMocks() {
        val method = UseNetwork::readBytes
        checkMocksInStaticMethod(
            method,
            eq(5),
            { packet, _, _, _ -> packet == null },
            { _, network, _, _ -> network == null },
            { _, _, mocks, r -> (mocks.single().values.single() as UtConcreteValue<*>).value == -1 && r == 0 },
            { packet, _, mocks, _ ->
                require(packet != null)

                val mockConstraint = (mocks.single().values.single() as UtConcreteValue<*>).value != -1
                val sizeConstraint = packet.isEmpty()

                mockConstraint && sizeConstraint
            },
            { packet, _, mocks, r ->
                require(packet != null)

                val values = mocks.single().values.map { (it as UtConcreteValue<*>).value }
                val mockConstraint = values.dropLast(1).all { it != -1 } && values.last() == -1
                val sizeConstraint = packet.size >= values.lastIndex

                mockConstraint && sizeConstraint && r == values.lastIndex

            },
            mockStrategy = MockStrategyApi.OTHER_PACKAGES,
            coverage = DoNotCalculate
        )
    }
}