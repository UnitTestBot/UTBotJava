package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.utbot.taint.parser.constants.*
import org.utbot.taint.parser.model.*

class ConditionParserTest {

    @Nested
    @DisplayName("parseCondition")
    inner class ParseConditionTest {
        @Test
        fun `should parse yaml null as ValueCondition`() {
            val yamlNull = Yaml.default.parseToYamlNode("null")
            val expectedCondition = ValueCondition(ArgumentValueNull)

            val actualCondition = ConditionParser.parseCondition(yamlNull)
            assertEquals(expectedCondition, actualCondition)
        }

        @Test
        fun `should parse yaml scalar with argument value as ValueCondition`() {
            val yamlScalar = Yaml.default.parseToYamlNode("some-string")
            val expectedCondition = ValueCondition(ArgumentValueString("some-string"))

            val actualCondition = ConditionParser.parseCondition(yamlScalar)
            assertEquals(expectedCondition, actualCondition)
        }

        @Test
        fun `should parse yaml scalar with argument type as TypeCondition`() {
            val yamlScalar = Yaml.default.parseToYamlNode("${k_lt}java.lang.Integer${k_gt}")
            val expectedCondition = TypeCondition(ArgumentTypeString("java.lang.Integer"))

            val actualCondition = ConditionParser.parseCondition(yamlScalar)
            assertEquals(expectedCondition, actualCondition)
        }

        @Test
        fun `should parse yaml list as OrCondition`() {
            val yamlList = Yaml.default.parseToYamlNode("[ 1, true, ${k_lt}java.lang.Integer${k_gt} ]")
            val expectedCondition = OrCondition(listOf(
                ValueCondition(ArgumentValueLong(1L)),
                ValueCondition(ArgumentValueBoolean(true)),
                TypeCondition(ArgumentTypeString("java.lang.Integer")),
            ))

            val actualCondition = ConditionParser.parseCondition(yamlList)
            assertEquals(expectedCondition, actualCondition)
        }

        @Test
        fun `should parse yaml map with a key 'not' as NotCondition`() {
            val yamlMap = Yaml.default.parseToYamlNode("$k_not: ${k_lt}int${k_gt}")
            val expectedCondition = NotCondition(TypeCondition(ArgumentTypeString("int")))

            val actualCondition = ConditionParser.parseCondition(yamlMap)
            assertEquals(expectedCondition, actualCondition)
        }

        @Test
        fun `should fail on yaml map without a key 'not'`() {
            val yamlMap = Yaml.default.parseToYamlNode("net: ${k_lt}int${k_gt}")

            assertThrows<ConfigurationParseError> {
                ConditionParser.parseCondition(yamlMap)
            }
        }

        @Test
        fun `should fail on yaml map with unknown keys`() {
            val yamlMap = Yaml.default.parseToYamlNode("{ $k_not: ${k_lt}int${k_gt}, unknown-key: 0 }")

            assertThrows<ConfigurationParseError> {
                ConditionParser.parseCondition(yamlMap)
            }
        }

        @Test
        fun `should parse complicated yaml node`() {
            val yamlMap = Yaml.default.parseToYamlNode("$k_not: [ { $k_not: 0 }, ${k_lt}int${k_gt}, { $k_not: null } ]")
            val expectedCondition = NotCondition(OrCondition(listOf(
                NotCondition(ValueCondition(ArgumentValueLong(0L))),
                TypeCondition(ArgumentTypeString("int")),
                NotCondition(ValueCondition(ArgumentValueNull))
            )))

            val actualCondition = ConditionParser.parseCondition(yamlMap)
            assertEquals(expectedCondition, actualCondition)
        }

        @Test
        fun `should fail on another yaml type`() {
            val yamlTaggedNode = YamlTaggedNode("some-tag", YamlNull(YamlPath.root))

            assertThrows<ConfigurationParseError> {
                ConditionParser.parseCondition(yamlTaggedNode)
            }
        }
    }

    @Nested
    @DisplayName("parseConditions")
    inner class ParseConditionsTest {
        @Test
        fun `should parse correct yaml map as Conditions`() {
            val yamlMap = Yaml.default.parseToYamlNode("{ $k_this: \"\", ${k_arg}2: { $k_not: ${k_lt}int${k_gt} }, $k_return: [ 0, 1 ] }")
            val expectedConditions = ConditionsMap(mapOf(
                ThisObject to ValueCondition(ArgumentValueString("")),
                MethodArgument(2u) to NotCondition(TypeCondition(ArgumentTypeString("int"))),
                ReturnValue to OrCondition(listOf(ValueCondition(ArgumentValueLong(0L)), ValueCondition(ArgumentValueLong(1L))))
            ))

            val actualConditions = ConditionParser.parseConditions(yamlMap)
            assertEquals(expectedConditions, actualConditions)
        }

        @Test
        fun `should parse empty yaml map as NoConditions`() {
            val yamlMap = Yaml.default.parseToYamlNode("{}")
            val expectedConditions = NoConditions

            val actualConditions = ConditionParser.parseConditions(yamlMap)
            assertEquals(expectedConditions, actualConditions)
        }

        @Test
        fun `should fail on another yaml type`() {
            val yamlList = Yaml.default.parseToYamlNode("[]")

            assertThrows<ConfigurationParseError> {
                ConditionParser.parseConditions(yamlList)
            }
        }
    }

    @Nested
    @DisplayName("parseConditionsKey")
    inner class ParseConditionsKeyTest {
        @Test
        fun `should parse yaml map with a key 'conditions'`() {
            val yamlMap = Yaml.default.parseToYamlNode("$k_conditions: { $k_return: null }").yamlMap
            val expectedConditions = ConditionsMap(mapOf(ReturnValue to ValueCondition(ArgumentValueNull)))

            val actualConditions = ConditionParser.parseConditionsKey(yamlMap)
            assertEquals(expectedConditions, actualConditions)
        }

        @Test
        fun `should parse yaml map without a key 'conditions' as NoConditions`() {
            val yamlMap = Yaml.default.parseToYamlNode("$k_marks: []").yamlMap
            val expectedConditions = NoConditions

            val actualConditions = ConditionParser.parseConditionsKey(yamlMap)
            assertEquals(expectedConditions, actualConditions)
        }
    }
}