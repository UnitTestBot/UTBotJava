package org.utbot.framework.plugin.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

internal class MockStrategyApiTest {

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