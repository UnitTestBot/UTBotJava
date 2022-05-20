package org.utbot.framework.plugin.api

import org.utbot.engine.MockStrategy
import org.utbot.framework.plugin.api.UtBotTestCaseGenerator.apiToModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


internal class UtBotTestCaseGeneratorTest {

    @Test
    fun testApiToModel() {
        assertEquals(
            MockStrategyApi.values().size, MockStrategy.values().size,
            "The number of strategies in the contract and engine model should match"
        )

        assertEquals(3, MockStrategyApi.values().size, "Three options only (so far)")
        assertEquals(MockStrategy.NO_MOCKS, apiToModel(MockStrategyApi.NO_MOCKS))
        assertEquals(MockStrategy.OTHER_PACKAGES, apiToModel(MockStrategyApi.OTHER_PACKAGES))
        assertEquals(MockStrategy.OTHER_CLASSES, apiToModel(MockStrategyApi.OTHER_CLASSES))
    }
}