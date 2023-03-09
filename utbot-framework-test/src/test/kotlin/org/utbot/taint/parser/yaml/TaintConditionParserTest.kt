package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TaintConditionParserTest {

    @Nested
    @DisplayName("parseCondition")
    inner class ParseConditionTest {
        @Test
        fun `should parse yaml null as ValueCondition`() {
            val yamlNull = Yaml.default.parseToYamlNode("null")
            val expectedCondition = YamlTaintConditionEqualValue(YamlArgumentValueNull)

            val actualCondition = TaintConditionParser.parseCondition(yamlNull)
            assertEquals(expectedCondition, actualCondition)
        }

        @Test
        fun `should parse yaml scalar with argument value as ValueCondition`() {
            val yamlScalar = Yaml.default.parseToYamlNode("some-string")
            val expectedCondition = YamlTaintConditionEqualValue(YamlArgumentValueString("some-string"))

            val actualCondition = TaintConditionParser.parseCondition(yamlScalar)
            assertEquals(expectedCondition, actualCondition)
        }

        @Test
        fun `should parse yaml scalar with argument type as TypeCondition`() {
            val yamlScalar = Yaml.default.parseToYamlNode("${k_lt}java.lang.Integer$k_gt")
            val expectedCondition = YamlTaintConditionIsType(YamlArgumentTypeString("java.lang.Integer"))

            val actualCondition = TaintConditionParser.parseCondition(yamlScalar)
            assertEquals(expectedCondition, actualCondition)
        }

        @Test
        fun `should parse yaml list as OrCondition`() {
            val yamlList = Yaml.default.parseToYamlNode("[ 1, true, ${k_lt}java.lang.Integer$k_gt ]")
            val expectedCondition = YamlTaintConditionOr(
                listOf(
                    YamlTaintConditionEqualValue(YamlArgumentValueLong(1L)),
                    YamlTaintConditionEqualValue(YamlArgumentValueBoolean(true)),
                    YamlTaintConditionIsType(YamlArgumentTypeString("java.lang.Integer")),
                )
            )

            val actualCondition = TaintConditionParser.parseCondition(yamlList)
            assertEquals(expectedCondition, actualCondition)
        }

        @Test
        fun `should parse yaml map with a key 'not' as NotCondition`() {
            val yamlMap = Yaml.default.parseToYamlNode("$k_not: ${k_lt}int$k_gt")
            val expectedCondition = YamlTaintConditionNot(YamlTaintConditionIsType(YamlArgumentTypeString("int")))

            val actualCondition = TaintConditionParser.parseCondition(yamlMap)
            assertEquals(expectedCondition, actualCondition)
        }

        @Test
        fun `should fail on yaml map without a key 'not'`() {
            val yamlMap = Yaml.default.parseToYamlNode("net: ${k_lt}int$k_gt")

            assertThrows<TaintParseError> {
                TaintConditionParser.parseCondition(yamlMap)
            }
        }

        @Test
        fun `should fail on yaml map with unknown keys`() {
            val yamlMap = Yaml.default.parseToYamlNode("{ $k_not: ${k_lt}int$k_gt, unknown-key: 0 }")

            assertThrows<TaintParseError> {
                TaintConditionParser.parseCondition(yamlMap)
            }
        }

        @Test
        fun `should parse complicated yaml node`() {
            val yamlMap = Yaml.default.parseToYamlNode("$k_not: [ { $k_not: 0 }, ${k_lt}int$k_gt, { $k_not: null } ]")
            val expectedCondition = YamlTaintConditionNot(
                YamlTaintConditionOr(
                    listOf(
                        YamlTaintConditionNot(YamlTaintConditionEqualValue(YamlArgumentValueLong(0L))),
                        YamlTaintConditionIsType(YamlArgumentTypeString("int")),
                        YamlTaintConditionNot(YamlTaintConditionEqualValue(YamlArgumentValueNull))
                    )
                )
            )

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
            val yamlMap =
                Yaml.default.parseToYamlNode("{ $k_this: \"\", ${k_arg}2: { $k_not: ${k_lt}int$k_gt }, $k_return: [ 0, 1 ] }")
            val expectedConditions = YamlTaintConditionsMap(
                mapOf(
                    YamlTaintEntityThis to YamlTaintConditionEqualValue(YamlArgumentValueString("")),
                    YamlTaintEntityArgument(2u) to YamlTaintConditionNot(
                        YamlTaintConditionIsType(
                            YamlArgumentTypeString(
                                "int"
                            )
                        )
                    ),
                    YamlTaintEntityReturn to YamlTaintConditionOr(
                        listOf(
                            YamlTaintConditionEqualValue(YamlArgumentValueLong(0L)), YamlTaintConditionEqualValue(
                                YamlArgumentValueLong(1L)
                            )
                        )
                    )
                )
            )

            val actualConditions = TaintConditionParser.parseConditions(yamlMap)
            assertEquals(expectedConditions, actualConditions)
        }

        @Test
        fun `should parse empty yaml map as NoConditions`() {
            val yamlMap = Yaml.default.parseToYamlNode("{}")
            val expectedConditions = YamlNoTaintConditions

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
            val expectedConditions = YamlTaintConditionsMap(
                mapOf(
                    YamlTaintEntityReturn to YamlTaintConditionEqualValue(
                        YamlArgumentValueNull
                    )
                )
            )

            val actualConditions = TaintConditionParser.parseConditionsKey(yamlMap)
            assertEquals(expectedConditions, actualConditions)
        }

        @Test
        fun `should parse yaml map without a key 'conditions' as NoConditions`() {
            val yamlMap = Yaml.default.parseToYamlNode("$k_marks: []").yamlMap
            val expectedConditions = YamlNoTaintConditions

            val actualConditions = TaintConditionParser.parseConditionsKey(yamlMap)
            assertEquals(expectedConditions, actualConditions)
        }
    }
}