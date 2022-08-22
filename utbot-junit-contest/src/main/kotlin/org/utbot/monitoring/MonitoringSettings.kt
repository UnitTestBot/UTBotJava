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
     * Number of running test generation.
     */
    val runTries by getIntProperty(1)

    /**
     * One running generation for all projects timeout in minutes.
     */
    val runTimeoutMinutes by getIntProperty(180)

    /**
     * Target project to generate tests.
     *
     * TODO: change it into list and group GlobalStats by projects.
     */
    val project by getStringProperty("guava")
}