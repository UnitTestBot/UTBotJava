package org.utbot.framework

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

internal class UtSettingsTest {
    @Test
    fun testMaxTestFileSize() {
        assertEquals(1, UtSettings.parseFileSize("1"))
        assertEquals(12, UtSettings.parseFileSize("12"))
        assertEquals(123, UtSettings.parseFileSize("123"))
        assertEquals(30000, UtSettings.parseFileSize("30K"))
        assertEquals(30000, UtSettings.parseFileSize("30Kb"))
        assertEquals(30000, UtSettings.parseFileSize("30kB"))
        assertEquals(30000, UtSettings.parseFileSize("30kb"))
        assertEquals(7000000, UtSettings.parseFileSize("7MB"))
        assertEquals(7000000, UtSettings.parseFileSize("7Mb"))
        assertEquals(7000000, UtSettings.parseFileSize("7M"))
        assertEquals(7, UtSettings.parseFileSize("7abc"))
        assertEquals(UtSettings.DEFAULT_MAX_FILE_SIZE, UtSettings.parseFileSize("qwerty"))
        assertEquals(UtSettings.DEFAULT_MAX_FILE_SIZE, UtSettings.parseFileSize("MB"))
        assertEquals(UtSettings.DEFAULT_MAX_FILE_SIZE, UtSettings.parseFileSize("1000000"))
    }
}