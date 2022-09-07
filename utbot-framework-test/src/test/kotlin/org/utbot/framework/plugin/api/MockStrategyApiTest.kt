package org.utbot.framework.plugin.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.utbot.engine.MockStrategy
import org.utbot.framework.util.toModel

internal class MockStrategyApiTest {

    @Test
    fun testApiToModel() {
        assertEquals(
            MockStrategyApi.values().size, MockStrategy.values().size,
            "The number of strategies in the contract and engine model should match"
        )

        assertEquals(3, MockStrategyApi.values().size, "Three options only (so far)")
        assertEquals(MockStrategy.NO_MOCKS, MockStrategyApi.NO_MOCKS.toModel())
        assertEquals(MockStrategy.OTHER_PACKAGES, MockStrategyApi.OTHER_PACKAGES.toModel())
        assertEquals(MockStrategy.OTHER_CLASSES, MockStrategyApi.OTHER_CLASSES.toModel())
    }

    @Test
    fun ensureDefaultStrategyIsOtherPackages() {
        assertEquals(
            MockStrategyApi.OTHER_PACKAGES,
            MockStrategyApi.defaultItem,
            "Expecting that ${MockStrategyApi.OTHER_PACKAGES} is the default policy for Mocks " +
                    "but ${MockStrategyApi.defaultItem} found"
        )
    }

    @Test
    fun testLabelToEnum() {
        assertEquals(
            MockStrategyApi.values().size,
            MockStrategyApi.labels().toSet().size,
            "Expecting all labels are unique"
        )

        assertFalse(
            MockStrategyApi.labels().any { it.isBlank() },
            "Expecting all labels are not empty"
        )
    }

}