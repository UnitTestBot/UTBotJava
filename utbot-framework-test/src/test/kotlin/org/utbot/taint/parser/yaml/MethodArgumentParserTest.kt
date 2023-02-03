package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.Yaml
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.utbot.taint.parser.model.*
import org.junit.jupiter.api.assertThrows

class MethodArgumentParserTest {

    @Nested
    @DisplayName("isArgumentType")
    inner class IsArgumentTypeTest {
        @Test
        fun `should return true on underscore`() {
            val yamlScalar = Yaml.default.parseToYamlNode("_")
            assertTrue(MethodArgumentParser.isArgumentType(yamlScalar))
        }

        @Test
        fun `should return true on type fqn in brackets`() {
            val yamlScalar = Yaml.default.parseToYamlNode("<java.lang.String>")
            assertTrue(MethodArgumentParser.isArgumentType(yamlScalar))
        }

        @Test
        fun `should return false on values`() {
            val yamlScalar = Yaml.default.parseToYamlNode("some-value")
            assertFalse(MethodArgumentParser.isArgumentType(yamlScalar))
        }

        @Test
        fun `should return false on another yaml type`() {
            val yamlList = Yaml.default.parseToYamlNode("[ <int> ]")
            assertFalse(MethodArgumentParser.isArgumentType(yamlList))
        }
    }

    @Nested
    @DisplayName("isArgumentValue")
    inner class IsArgumentValueTest {
        @Test
        fun `should return true on scalar`() {
            val yamlScalar = Yaml.default.parseToYamlNode("0")
            assertTrue(MethodArgumentParser.isArgumentValue(yamlScalar))
        }

        @Test
        fun `should return true on type fqn in brackets`() {
            val yamlNull = Yaml.default.parseToYamlNode("null")
            assertTrue(MethodArgumentParser.isArgumentValue(yamlNull))
        }

        @Test
        fun `should return false on type fqn in brackets`() {
            val yamlScalar = Yaml.default.parseToYamlNode("<float>")
            assertFalse(MethodArgumentParser.isArgumentValue(yamlScalar))
        }

        @Test
        fun `should return false on another yaml type`() {
            val yamlList = Yaml.default.parseToYamlNode("[ test ]")
            assertFalse(MethodArgumentParser.isArgumentValue(yamlList))
        }
    }

    @Nested
    @DisplayName("parseArgumentType")
    inner class ParseArgumentTypeTest {
        @Test
        fun `should parse underscore as ArgumentTypeAny`() {
            val yamlScalar = Yaml.default.parseToYamlNode("_")
            val expectedArgumentType = ArgumentTypeAny

            val actualArgumentType = MethodArgumentParser.parseArgumentType(yamlScalar)
            assertEquals(expectedArgumentType, actualArgumentType)
        }

        @Test
        fun `should parse type fqn in brackets`() {
            val yamlScalar = Yaml.default.parseToYamlNode("<double>")
            val expectedArgumentType = ArgumentTypeString("double")

            val actualArgumentType = MethodArgumentParser.parseArgumentType(yamlScalar)
            assertEquals(expectedArgumentType, actualArgumentType)
        }

        @Test
        fun `should fail on another yaml type`() {
            val yamlMap = Yaml.default.parseToYamlNode("type: <double>")

            assertThrows<ConfigurationParseError> {
                MethodArgumentParser.parseArgumentType(yamlMap)
            }
        }
    }

    @Nested
    @DisplayName("parseArgumentValue")
    inner class ParseArgumentValueTest {
        @Test
        fun `should parse yaml null as ArgumentValueNull`() {
            val yamlNull = Yaml.default.parseToYamlNode("null")
            val expectedArgumentValue = ArgumentValueNull

            val actualArgumentValue = MethodArgumentParser.parseArgumentValue(yamlNull)
            assertEquals(expectedArgumentValue, actualArgumentValue)
        }

        @Test
        fun `should parse boolean yaml scalar`() {
            val yamlScalar = Yaml.default.parseToYamlNode("false")
            val expectedArgumentValue = ArgumentValueBoolean(false)

            val actualArgumentValue = MethodArgumentParser.parseArgumentValue(yamlScalar)
            assertEquals(expectedArgumentValue, actualArgumentValue)
        }

        @Test
        fun `should parse long yaml scalar`() {
            val yamlScalar = Yaml.default.parseToYamlNode("17")
            val expectedArgumentValue = ArgumentValueLong(17L)

            val actualArgumentValue = MethodArgumentParser.parseArgumentValue(yamlScalar)
            assertEquals(expectedArgumentValue, actualArgumentValue)
        }

        @Test
        fun `should parse double yaml scalar`() {
            val yamlScalar = Yaml.default.parseToYamlNode("1.2")
            val expectedArgumentValue = ArgumentValueDouble(1.2)

            val actualArgumentValue = MethodArgumentParser.parseArgumentValue(yamlScalar)
            assertEquals(expectedArgumentValue, actualArgumentValue)
        }

        @Test
        fun `should parse string yaml scalar`() {
            val yamlScalar = Yaml.default.parseToYamlNode("some-string")
            val expectedArgumentValue = ArgumentValueString("some-string")

            val actualArgumentValue = MethodArgumentParser.parseArgumentValue(yamlScalar)
            assertEquals(expectedArgumentValue, actualArgumentValue)
        }

        @Test
        fun `should fail on another yaml type`() {
            val yamlList = Yaml.default.parseToYamlNode("[ some-string ]")

            assertThrows<ConfigurationParseError> {
                MethodArgumentParser.parseArgumentValue(yamlList)
            }
        }
    }
}