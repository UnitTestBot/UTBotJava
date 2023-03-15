package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.utbot.taint.parser.constants.*
import org.utbot.taint.parser.model.*

class TaintConditionParserTest {

    @Nested
    @DisplayName("parseCondition")
    inner class ParseConditionTest {
        @Test
        fun `should parse yaml null as ValueCondition`() {
            val yamlNull = Yaml.default.parseToYamlNode("null")
            val expectedCondition = DtoTaintConditionEqualValue(DtoArgumentValueNull)

            val actualCondition = TaintConditionParser.parseCondition(yamlNull)
            assertEquals(expectedCondition, actualCondition)
        }

        @Test
        fun `should parse yaml scalar with argument value as ValueCondition`() {
            val yamlScalar = Yaml.default.parseToYamlNode("some-string")
            val expectedCondition = DtoTaintConditionEqualValue(DtoArgumentValueString("some-string"))

            val actualCondition = TaintConditionParser.parseCondition(yamlScalar)
            assertEquals(expectedCondition, actualCondition)
        }

        @Test
        fun `should parse yaml scalar with argument type as TypeCondition`() {
            val yamlScalar = Yaml.default.parseToYamlNode("${k_lt}java.lang.Integer${k_gt}")
            val expectedCondition = DtoTaintConditionIsType(DtoArgumentTypeString("java.lang.Integer"))

            val actualCondition = TaintConditionParser.parseCondition(yamlScalar)
            assertEquals(expectedCondition, actualCondition)
        }

        @Test
        fun `should parse yaml list as OrCondition`() {
            val yamlList = Yaml.default.parseToYamlNode("[ 1, true, ${k_lt}java.lang.Integer${k_gt} ]")
            val expectedCondition = DtoTaintConditionOr(listOf(
                DtoTaintConditionEqualValue(DtoArgumentValueLong(1L)),
                DtoTaintConditionEqualValue(DtoArgumentValueBoolean(true)),
                DtoTaintConditionIsType(DtoArgumentTypeString("java.lang.Integer")),
            ))

            val actualCondition = TaintConditionParser.parseCondition(yamlList)
            assertEquals(expectedCondition, actualCondition)
        }

        @Test
        fun `should parse yaml map with a key 'not' as NotCondition`() {
            val yamlMap = Yaml.default.parseToYamlNode("$k_not: ${k_lt}int${k_gt}")
            val expectedCondition = DtoTaintConditionNot(DtoTaintConditionIsType(DtoArgumentTypeString("int")))

            val actualCondition = TaintConditionParser.parseCondition(yamlMap)
            assertEquals(expectedCondition, actualCondition)
        }

        @Test
        fun `should fail on yaml map without a key 'not'`() {
            val yamlMap = Yaml.default.parseToYamlNode("net: ${k_lt}int${k_gt}")

            assertThrows<TaintParseError> {
                TaintConditionParser.parseCondition(yamlMap)
            }
        }

        @Test
        fun `should fail on yaml map with unknown keys`() {
            val yamlMap = Yaml.default.parseToYamlNode("{ $k_not: ${k_lt}int${k_gt}, unknown-key: 0 }")

            assertThrows<TaintParseError> {
                TaintConditionParser.parseCondition(yamlMap)
            }
        }

        @Test
        fun `should parse complicated yaml node`() {
            val yamlMap = Yaml.default.parseToYamlNode("$k_not: [ { $k_not: 0 }, ${k_lt}int${k_gt}, { $k_not: null } ]")
            val expectedCondition = DtoTaintConditionNot(DtoTaintConditionOr(listOf(
                DtoTaintConditionNot(DtoTaintConditionEqualValue(DtoArgumentValueLong(0L))),
                DtoTaintConditionIsType(DtoArgumentTypeString("int")),
                DtoTaintConditionNot(DtoTaintConditionEqualValue(DtoArgumentValueNull))
            )))

            val actualCondition = TaintConditionParser.parseCondition(yamlMap)
            assertEquals(expectedCondition, actualCondition)
        }

        @Test
        fun `should fail on another yaml type`() {
            val yamlTaggedNode = YamlTaggedNode("some-tag", YamlNull(YamlPath.root))

            assertThrows<TaintParseError> {
                TaintConditionParser.parseCondition(yamlTaggedNode)
            }
        }
    }

    @Nested
    @DisplayName("parseConditions")
    inner class ParseConditionsTest {
        @Test
        fun `should parse correct yaml map as Conditions`() {
            val yamlMap = Yaml.default.parseToYamlNode("{ $k_this: \"\", ${k_arg}2: { $k_not: ${k_lt}int${k_gt} }, $k_return: [ 0, 1 ] }")
            val expectedConditions = DtoTaintConditionsMap(mapOf(
                DtoTaintEntityThis to DtoTaintConditionEqualValue(DtoArgumentValueString("")),
                DtoTaintEntityArgument(2u) to DtoTaintConditionNot(DtoTaintConditionIsType(DtoArgumentTypeString("int"))),
                DtoTaintEntityReturn to DtoTaintConditionOr(listOf(DtoTaintConditionEqualValue(DtoArgumentValueLong(0L)), DtoTaintConditionEqualValue(DtoArgumentValueLong(1L))))
            ))

            val actualConditions = TaintConditionParser.parseConditions(yamlMap)
            assertEquals(expectedConditions, actualConditions)
        }

        @Test
        fun `should parse empty yaml map as NoConditions`() {
            val yamlMap = Yaml.default.parseToYamlNode("{}")
            val expectedConditions = DtoNoTaintConditions

            val actualConditions = TaintConditionParser.parseConditions(yamlMap)
            assertEquals(expectedConditions, actualConditions)
        }

        @Test
        fun `should fail on another yaml type`() {
            val yamlList = Yaml.default.parseToYamlNode("[]")

            assertThrows<TaintParseError> {
                TaintConditionParser.parseConditions(yamlList)
            }
        }
    }

    @Nested
    @DisplayName("parseConditionsKey")
    inner class ParseConditionsKeyTest {
        @Test
        fun `should parse yaml map with a key 'conditions'`() {
            val yamlMap = Yaml.default.parseToYamlNode("$k_conditions: { $k_return: null }").yamlMap
            val expectedConditions = DtoTaintConditionsMap(mapOf(DtoTaintEntityReturn to DtoTaintConditionEqualValue(DtoArgumentValueNull)))

            val actualConditions = TaintConditionParser.parseConditionsKey(yamlMap)
            assertEquals(expectedConditions, actualConditions)
        }

        @Test
        fun `should parse yaml map without a key 'conditions' as NoConditions`() {
            val yamlMap = Yaml.default.parseToYamlNode("$k_marks: []").yamlMap
            val expectedConditions = DtoNoTaintConditions

            val actualConditions = TaintConditionParser.parseConditionsKey(yamlMap)
            assertEquals(expectedConditions, actualConditions)
        }
    }
}