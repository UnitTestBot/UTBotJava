package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.Yaml
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.utbot.taint.parser.model.*
import org.junit.jupiter.api.assertThrows

class TaintEntityParserTest {

    @Nested
    @DisplayName("taintEntityByName")
    inner class TaintEntityByNameTest {
        @Test
        fun `should return ThisObject on 'this'`() {
            val actualEntity = TaintEntityParser.taintEntityByName("this")
            val expectedEntity = ThisObject
            assertEquals(expectedEntity, actualEntity)
        }

        @Test
        fun `should return ReturnValue on 'return'`() {
            val actualEntity = TaintEntityParser.taintEntityByName("return")
            val expectedEntity = ReturnValue
            assertEquals(expectedEntity, actualEntity)
        }

        @Test
        fun `should return MethodArgument(1) on 'arg1'`() {
            val actualEntity = TaintEntityParser.taintEntityByName("arg1")
            val expectedEntity = MethodArgument(1u)
            assertEquals(expectedEntity, actualEntity)
        }

        @Test
        fun `should return MethodArgument(227) on 'arg227'`() {
            val actualEntity = TaintEntityParser.taintEntityByName("arg227")
            val expectedEntity = MethodArgument(227u)
            assertEquals(expectedEntity, actualEntity)
        }

        @Test
        fun `should fail on zero index 'arg0'`() {
            assertThrows<ConfigurationParseError> {
                TaintEntityParser.taintEntityByName("arg0")
            }
        }

        @Test
        fun `should fail on another entity name`() {
            assertThrows<ConfigurationParseError> {
                TaintEntityParser.taintEntityByName("argument1")
            }
        }
    }

    @Nested
    @DisplayName("parseTaintEntities")
    inner class ParseTaintEntitiesTest {
        @Test
        fun `should parse yaml scalar`() {
            val yamlScalar = Yaml.default.parseToYamlNode("this")
            val expectedEntities = TaintEntitiesSet(setOf(ThisObject))

            val actualEntities = TaintEntityParser.parseTaintEntities(yamlScalar)
            assertEquals(expectedEntities, actualEntities)
        }

        @Test
        fun `should parse yaml list`() {
            val yamlList = Yaml.default.parseToYamlNode("[ this, arg1, arg5, return ]")
            val expectedEntities = TaintEntitiesSet(setOf(ThisObject, MethodArgument(1u), MethodArgument(5u), ReturnValue))

            val actualEntities = TaintEntityParser.parseTaintEntities(yamlList)
            assertEquals(expectedEntities, actualEntities)
        }

        @Test
        fun `should fail on empty yaml list`() {
            val yamlListEmpty = Yaml.default.parseToYamlNode("[]")

            assertThrows<ConfigurationParseError> {
                TaintEntityParser.parseTaintEntities(yamlListEmpty)
            }
        }

        @Test
        fun `should fail on another yaml type`() {
            val yamlMap = Yaml.default.parseToYamlNode("add-to: return")

            assertThrows<ConfigurationParseError> {
                TaintEntityParser.parseTaintEntities(yamlMap)
            }
        }
    }
}