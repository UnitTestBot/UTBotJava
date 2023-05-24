package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.Yaml
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.utbot.taint.parser.constants.*
import org.junit.jupiter.api.assertThrows

class TaintEntityParserTest {

    @Nested
    @DisplayName("taintEntityByName")
    inner class TaintEntityByNameTest {
        @Test
        fun `should return ThisObject on 'this'`() {
            val actualEntity = TaintEntityParser.taintEntityByName(k_this)
            val expectedEntity = DtoTaintEntityThis
            assertEquals(expectedEntity, actualEntity)
        }

        @Test
        fun `should return ReturnValue on 'return'`() {
            val actualEntity = TaintEntityParser.taintEntityByName(k_return)
            val expectedEntity = DtoTaintEntityReturn
            assertEquals(expectedEntity, actualEntity)
        }

        @Test
        fun `should return MethodArgument(1) on 'arg1'`() {
            val actualEntity = TaintEntityParser.taintEntityByName("${k_arg}1")
            val expectedEntity = DtoTaintEntityArgument(1u)
            assertEquals(expectedEntity, actualEntity)
        }

        @Test
        fun `should return MethodArgument(227) on 'arg227'`() {
            val actualEntity = TaintEntityParser.taintEntityByName("${k_arg}227")
            val expectedEntity = DtoTaintEntityArgument(227u)
            assertEquals(expectedEntity, actualEntity)
        }

        @Test
        fun `should fail on zero index 'arg0'`() {
            assertThrows<TaintParseError> {
                TaintEntityParser.taintEntityByName("${k_arg}0")
            }
        }

        @Test
        fun `should fail on another entity name`() {
            assertThrows<TaintParseError> {
                TaintEntityParser.taintEntityByName("argument1")
            }
        }
    }

    @Nested
    @DisplayName("parseTaintEntities")
    inner class ParseTaintEntitiesTest {
        @Test
        fun `should parse yaml scalar`() {
            val yamlScalar = Yaml.default.parseToYamlNode(k_this)
            val expectedEntities = DtoTaintEntitiesSet(setOf(DtoTaintEntityThis))

            val actualEntities = TaintEntityParser.parseTaintEntities(yamlScalar)
            assertEquals(expectedEntities, actualEntities)
        }

        @Test
        fun `should parse yaml list`() {
            val yamlList = Yaml.default.parseToYamlNode("[ $k_this, ${k_arg}1, ${k_arg}5, $k_return ]")
            val expectedEntities = DtoTaintEntitiesSet(setOf(DtoTaintEntityThis, DtoTaintEntityArgument(1u), DtoTaintEntityArgument(5u), DtoTaintEntityReturn))

            val actualEntities = TaintEntityParser.parseTaintEntities(yamlList)
            assertEquals(expectedEntities, actualEntities)
        }

        @Test
        fun `should fail on empty yaml list`() {
            val yamlListEmpty = Yaml.default.parseToYamlNode("[]")

            assertThrows<TaintParseError> {
                TaintEntityParser.parseTaintEntities(yamlListEmpty)
            }
        }

        @Test
        fun `should fail on another yaml type`() {
            val yamlMap = Yaml.default.parseToYamlNode("$k_addTo: $k_return")

            assertThrows<TaintParseError> {
                TaintEntityParser.parseTaintEntities(yamlMap)
            }
        }
    }
}