package org.utbot.common

import java.io.InputStream
import mu.KLogger
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


internal class AbstractSettingsTest {
    @Test
    fun testParsing() {
        var settings = createSettings("testBoolean=false\ntestInt=3\ntestIntRange=2\ntestLong=333\ntestLongRange=222")
        Assertions.assertFalse(settings.testBoolean)
        Assertions.assertEquals(3, settings.testInt)
        Assertions.assertEquals(2, settings.testIntRange)
        Assertions.assertEquals(333, settings.testLong)
        Assertions.assertEquals(222, settings.testLongRange)

        settings = createSettings("testBoolean=invalid\ntestInt=-1\ntestIntRange=-1\ntestLong=-111\ntestLongRange=-111")
        Assertions.assertTrue(settings.testBoolean)
        Assertions.assertEquals(-1, settings.testInt)
        Assertions.assertEquals(1, settings.testIntRange)
        Assertions.assertEquals(-111, settings.testLong)
        Assertions.assertEquals(111, settings.testLongRange)

        settings = createSettings("")
        Assertions.assertTrue(settings.testBoolean)
        Assertions.assertEquals(3, settings.testInt)
        Assertions.assertEquals(333, settings.testLongRange)
    }

    private fun createSettings(propertiesText: String): TestSettings {
        AbstractSettings.setupFactory(object : SettingsContainerFactory {
            override fun createSettingsContainer(
                logger: KLogger, defaultKeyForSettingsPath: String, defaultSettingsPath: String?
            ) = object : PropertiesSettingsContainer(logger, defaultKeyForSettingsPath, defaultSettingsPath) {
                override fun getInputStream(): InputStream = propertiesText.byteInputStream()
            }
        })
        return TestSettings()
    }

    internal class TestSettings : AbstractSettings(KotlinLogging.logger {}, "") {
        val testBoolean: Boolean by getBooleanProperty(true)
        val testInt: Int by getIntProperty(3)
        val testIntRange: Int by getIntProperty(3, 1, 5)
        val testLong: Long by getLongProperty(333)
        val testLongRange: Long by getLongProperty(333, 111, 555)
    }
}