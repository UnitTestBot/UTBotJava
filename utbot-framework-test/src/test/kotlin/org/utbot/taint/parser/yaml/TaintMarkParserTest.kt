package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.Yaml
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.utbot.taint.parser.constants.*
import org.utbot.taint.parser.model.*
import org.junit.jupiter.api.assertThrows

class TaintMarkParserTest {

    @Nested
    @DisplayName("parseTaintMarks")
    inner class ParseTaintMarksTest {
        @Test
        fun `should parse yaml scalar`() {
            val yamlScalar = Yaml.default.parseToYamlNode("sensitive-data")
            val expectedMarks = TaintMarksSet(setOf(TaintMark("sensitive-data")))

            val actualMarks = TaintMarkParser.parseTaintMarks(yamlScalar)
            assertEquals(expectedMarks, actualMarks)
        }

        @Test
        fun `should parse yaml list`() {
            val yamlList = Yaml.default.parseToYamlNode("[ xss, sensitive-data, sql-injection ]")
            val expectedMarks =
                TaintMarksSet(setOf(TaintMark("xss"), TaintMark("sensitive-data"), TaintMark("sql-injection")))

            val actualMarks = TaintMarkParser.parseTaintMarks(yamlList)
            assertEquals(expectedMarks, actualMarks)
        }

        @Test
        fun `should parse empty yaml list`() {
            val yamlListEmpty = Yaml.default.parseToYamlNode("[]")
            val expectedMarks = AllTaintMarks

            val actualMarks = TaintMarkParser.parseTaintMarks(yamlListEmpty)
            assertEquals(expectedMarks, actualMarks)
        }

        @Test
        fun `should fail on another yaml type`() {
            val yamlMap = Yaml.default.parseToYamlNode("$k_marks: [ xss ]")

            assertThrows<ConfigurationParseError> {
                TaintMarkParser.parseTaintMarks(yamlMap)
            }
        }
    }
}