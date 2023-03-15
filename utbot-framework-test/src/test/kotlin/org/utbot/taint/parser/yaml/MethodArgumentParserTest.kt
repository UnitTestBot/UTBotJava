package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.Yaml
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.utbot.taint.parser.constants.*
import org.utbot.taint.parser.model.*
import org.junit.jupiter.api.assertThrows

class MethodArgumentParserTest {

    @Nested
    @DisplayName("isArgumentType")
    inner class IsArgumentTypeTest {
        @Test
        fun `should return true on underscore`() {
            val yamlScalar = Yaml.default.parseToYamlNode(k__)
            assertTrue(MethodArgumentParser.isArgumentType(yamlScalar))
        }

        @Test
        fun `should return true on type fqn in brackets`() {
            val yamlScalar = Yaml.default.parseToYamlNode("${k_lt}java.lang.String${k_gt}")
            assertTrue(MethodArgumentParser.isArgumentType(yamlScalar))
        }

        @Test
        fun `should return false on values`() {
            val yamlScalar = Yaml.default.parseToYamlNode("some-value")
            assertFalse(MethodArgumentParser.isArgumentType(yamlScalar))
        }

        @Test
        fun `should return false on another yaml type`() {
            val yamlList = Yaml.default.parseToYamlNode("[ ${k_lt}int${k_gt} ]")
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
            val yamlScalar = Yaml.default.parseToYamlNode("${k_lt}float${k_gt}")
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
            val yamlScalar = Yaml.default.parseToYamlNode(k__)
            val expectedArgumentType = DtoArgumentTypeAny

            val actualArgumentType = MethodArgumentParser.parseArgumentType(yamlScalar)
            assertEquals(expectedArgumentType, actualArgumentType)
        }

        @Test
        fun `should parse type fqn in brackets`() {
            val yamlScalar = Yaml.default.parseToYamlNode("${k_lt}double${k_gt}")
            val expectedArgumentType = DtoArgumentTypeString("double")

            val actualArgumentType = MethodArgumentParser.parseArgumentType(yamlScalar)
            assertEquals(expectedArgumentType, actualArgumentType)
        }

        @Test
        fun `should fail on another yaml type`() {
            val yamlMap = Yaml.default.parseToYamlNode("type: ${k_lt}double${k_gt}")

            assertThrows<TaintParseError> {
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
            val expectedArgumentValue = DtoArgumentValueNull

            val actualArgumentValue = MethodArgumentParser.parseArgumentValue(yamlNull)
            assertEquals(expectedArgumentValue, actualArgumentValue)
        }

        @Test
        fun `should parse boolean yaml scalar`() {
            val yamlScalar = Yaml.default.parseToYamlNode("false")
            val expectedArgumentValue = DtoArgumentValueBoolean(false)

            val actualArgumentValue = MethodArgumentParser.parseArgumentValue(yamlScalar)
            assertEquals(expectedArgumentValue, actualArgumentValue)
        }

        @Test
        fun `should parse long yaml scalar`() {
            val yamlScalar = Yaml.default.parseToYamlNode("17")
            val expectedArgumentValue = DtoArgumentValueLong(17L)

            val actualArgumentValue = MethodArgumentParser.parseArgumentValue(yamlScalar)
            assertEquals(expectedArgumentValue, actualArgumentValue)
        }

        @Test
        fun `should parse double yaml scalar`() {
            val yamlScalar = Yaml.default.parseToYamlNode("1.2")
            val expectedArgumentValue = DtoArgumentValueDouble(1.2)

            val actualArgumentValue = MethodArgumentParser.parseArgumentValue(yamlScalar)
            assertEquals(expectedArgumentValue, actualArgumentValue)
        }

        @Test
        fun `should parse string yaml scalar`() {
            val yamlScalar = Yaml.default.parseToYamlNode("some-string")
            val expectedArgumentValue = DtoArgumentValueString("some-string")

            val actualArgumentValue = MethodArgumentParser.parseArgumentValue(yamlScalar)
            assertEquals(expectedArgumentValue, actualArgumentValue)
        }

        @Test
        fun `should fail on another yaml type`() {
            val yamlList = Yaml.default.parseToYamlNode("[ some-string ]")

            assertThrows<TaintParseError> {
                MethodArgumentParser.parseArgumentValue(yamlList)
            }
        }
    }
}