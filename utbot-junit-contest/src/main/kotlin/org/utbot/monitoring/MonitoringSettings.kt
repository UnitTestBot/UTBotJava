package org.utbot.monitoring

import mu.KotlinLogging
import org.utbot.common.AbstractSettings


private val logger = KotlinLogging.logger {}

private const val defaultKeyForSettingsPath = "utbot.monitoring.settings.path"

object MonitoringSettings : AbstractSettings(
    logger, defaultKeyForSettingsPath
) {
    /**
     * Test generation for one class timeout.
     */
    val classTimeoutSeconds by getIntProperty(20)

    /**
     * Bound of classes for generation.
     */
    val processedClassesThreshold by getIntProperty(9999)

    /**
     * One running generation for all projects timeout in minutes.
     */
    val runTimeoutMinutes by getIntProperty(180)

    /**
     * The target project to generate tests.
     */
    val project by getStringProperty("guava")

    /**
     * Set up fuzzing timeout relatively total generation time.
     */
    val fuzzingRatios by getListProperty(listOf(0.1), String::toDouble)
}