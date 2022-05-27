package org.utbot

import java.io.FileInputStream
import java.util.Properties

object QualityAnalysisConfig {
    private const val configPath = "utbot-analytics/src/main/resources/config.properties"

    private val properties = Properties().also { props ->
        FileInputStream(configPath).use { inputStream ->
            props.load(inputStream)
        }
    }

    val project: String = properties.getProperty("project")
    val selectors: List<String> = properties.getProperty("selectors").split(",")
    val covStatistics: List<String> = properties.getProperty("covStatistics").split(",")

    const val outputDir: String = "eval/res"
    val classesList: String = "utbot-junit-contest/src/main/resources/classes/${project}/list"
}