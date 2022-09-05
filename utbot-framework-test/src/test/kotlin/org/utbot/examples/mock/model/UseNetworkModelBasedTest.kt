package org.utbot.examples.mock.model

import org.utbot.tests.infrastructure.UtModelTestCaseChecker
import org.utbot.examples.mock.UseNetwork
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtVoidModel
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class UseNetworkModelBasedTest : UtModelTestCaseChecker(testClass = UseNetwork::class) {
    @Test
    fun testMockVoidMethod() {
        check(
            UseNetwork::mockVoidMethod,
            eq(1),
            { network, _ ->
                require(network is UtCompositeModel)

                val mock = network.mocks.values.single().single()

                mock is UtVoidModel
            },
            mockStrategy = MockStrategyApi.OTHER_PACKAGES
        )
    }
}