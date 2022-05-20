package org.utbot.cli

import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.slf4j.event.Level

internal class UtBotCliTest {

    @Test
    fun testGetVersion() {
        assertNotEquals("N/A", getVersion())
    }

    @ParameterizedTest
    @EnumSource
    fun checkVerbosityForLevel(level: Level) {
        val utBotCli = UtBotCli()
        utBotCli.main(listOf("--verbosity", level.name))
        assertEquals(level.name, LogManager.getRootLogger().level.name())
    }
}