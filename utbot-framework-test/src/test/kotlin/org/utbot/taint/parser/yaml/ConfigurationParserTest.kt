package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.Yaml
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.utbot.taint.parser.model.*
import org.junit.jupiter.api.assertThrows

class ConfigurationParserTest {

    @Nested
    @DisplayName("parseConfiguration")
    inner class ParseConfigurationTest {
        @Test
        fun `should parse yaml map as Configuration`() {
            val yamlMap = Yaml.default.parseToYamlNode("{ sources: [], passes: [], cleaners: [], sinks: [] }")
            val expectedConfiguration = Configuration(listOf(), listOf(), listOf(), listOf())

            val actualConfiguration = ConfigurationParser.parseConfiguration(yamlMap)
            assertEquals(expectedConfiguration, actualConfiguration)
        }

        @Test
        fun `should not fail if yaml map does not contain some keys`() {
            val yamlMap = Yaml.default.parseToYamlNode("{ sources: [], sinks: [] }")
            val expectedConfiguration = Configuration(listOf(), listOf(), listOf(), listOf())

            val actualConfiguration = ConfigurationParser.parseConfiguration(yamlMap)
            assertEquals(expectedConfiguration, actualConfiguration)
        }

        @Test
        fun `should fail on other yaml types`() {
            val yamlList = Yaml.default.parseToYamlNode("[]")

            assertThrows<ConfigurationParseError> {
                ConfigurationParser.parseConfiguration(yamlList)
            }
        }
    }
}