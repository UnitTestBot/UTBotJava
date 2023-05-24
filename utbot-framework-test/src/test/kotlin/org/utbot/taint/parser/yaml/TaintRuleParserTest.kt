package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.Yaml
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.utbot.taint.parser.constants.*

class TaintRuleParserTest {

    @Nested
    @DisplayName("isRule")
    inner class IsRuleTest {

        private val isRuleData = listOf(
            SourceData to TaintRuleParser::isSourceRule,
            PassData to TaintRuleParser::isPassRule,
            CleanerData to TaintRuleParser::isCleanerRule,
            SinkData to TaintRuleParser::isSinkRule
        )

        @TestFactory
        fun `should return true on a rule`() = isRuleData.map { (ruleData, isRule) ->
            DynamicTest.dynamicTest(isRule.name) {
                val yamlMap = Yaml.default.parseToYamlNode(ruleData.yamlInput)
                assertTrue(isRule(yamlMap))
            }
        }

        @TestFactory
        fun `should return true on a rule with optional keys`() = isRuleData.map { (ruleData, isRule) ->
            DynamicTest.dynamicTest(isRule.name) {
                val yamlMap = Yaml.default.parseToYamlNode(ruleData.yamlInputAdvanced)
                assertTrue(isRule(yamlMap))
            }
        }

        @TestFactory
        fun `should return false on an invalid rule`() = isRuleData.map { (ruleData, isRule) ->
            DynamicTest.dynamicTest(isRule.name) {
                val yamlMap = Yaml.default.parseToYamlNode(ruleData.yamlInputInvalid)
                assertFalse(isRule(yamlMap))
            }
        }

        @TestFactory
        fun `should return false on rules node`() = isRuleData.map { (ruleData, isRule) ->
            DynamicTest.dynamicTest(isRule.name) {
                val rulesData = RulesData(ruleData)
                val yamlMap = Yaml.default.parseToYamlNode(rulesData.yamlInput)
                assertFalse(isRule(yamlMap))
            }
        }
    }

    @Nested
    @DisplayName("parseRule")
    inner class ParseRuleTest {

        private val parseRuleData = listOf(
            SourceData to TaintRuleParser::parseSourceRule,
            PassData to TaintRuleParser::parsePassRule,
            CleanerData to TaintRuleParser::parseCleanerRule,
            SinkData to TaintRuleParser::parseSinkRule
        )

        @TestFactory
        fun `should parse yaml map that satisfies isRule`() = parseRuleData.map { (ruleData, parseRule) ->
            DynamicTest.dynamicTest(parseRule.name) {
                val yamlMap = Yaml.default.parseToYamlNode(ruleData.yamlInput)
                val expectedRule = ruleData.yamlInputParsed(defaultMethodFqn)

                val actualRule = parseRule(yamlMap, defaultMethodNameParts)
                assertEquals(expectedRule, actualRule)
            }
        }

        @TestFactory
        fun `should parse yaml map with optional keys that satisfies isRule`() = parseRuleData.map { (ruleData, parseRule) ->
            DynamicTest.dynamicTest(parseRule.name) {
                val yamlMap = Yaml.default.parseToYamlNode(ruleData.yamlInputAdvanced)
                val expectedRule = ruleData.yamlInputAdvancedParsed(defaultMethodFqn)

                val actualRule = parseRule(yamlMap, defaultMethodNameParts)
                assertEquals(expectedRule, actualRule)
            }
        }

        @TestFactory
        fun `should fail on yaml map with unknown keys`() = parseRuleData.map { (ruleData, parseRule) ->
            DynamicTest.dynamicTest(parseRule.name) {
                val yamlMap = Yaml.default.parseToYamlNode(ruleData.yamlInputUnknownKey)

                assertThrows<TaintParseError> {
                    parseRule(yamlMap, defaultMethodNameParts)
                }
            }
        }
    }

    @Nested
    @DisplayName("parseSources")
    inner class ParseSourcesTest {

        private val parseRulesData = listOf(
            SourcesData to TaintRuleParser::parseSources,
            PassesData to TaintRuleParser::parsePasses,
            CleanersData to TaintRuleParser::parseCleaners,
            SinksData to TaintRuleParser::parseSinks
        )

        @TestFactory
        fun `should parse yaml list with rules`() = parseRulesData.map { (rulesData, parseRules) ->
            DynamicTest.dynamicTest(parseRules.name) {
                val yamlMap = Yaml.default.parseToYamlNode(rulesData.yamlInput)
                val expectedRules = rulesData.yamlInputParsed

                val actualRules = parseRules(yamlMap)
                assertEquals(expectedRules, actualRules)
            }
        }

        @TestFactory
        fun `should parse yaml list with rules specified using nested names`() = parseRulesData.map { (rulesData, parseRules) ->
            DynamicTest.dynamicTest(parseRules.name) {
                val yamlMap = Yaml.default.parseToYamlNode(rulesData.yamlInputNestedNames)
                val expectedRules = rulesData.yamlInputNestedNamesParsed

                val actualRules = parseRules(yamlMap)
                assertEquals(expectedRules, actualRules)
            }
        }

        @TestFactory
        fun `should fail on invalid yaml structure`() = parseRulesData.map { (rulesData, parseRules) ->
            DynamicTest.dynamicTest(parseRules.name) {
                val yamlMap = Yaml.default.parseToYamlNode(rulesData.yamlInputInvalid)

                assertThrows<TaintParseError> {
                    parseRules(yamlMap)
                }
            }
        }
    }

    // test data

    private val defaultMethodNameParts = listOf("org.example.Server.start")
    private val defaultMethodFqn = DtoMethodFqn(listOf("org", "example"), "Server", "start")

    // one rule

    interface RuleData<Rule> {
        val yamlInput: String
        val yamlInputAdvanced: String
        val yamlInputInvalid: String
        val yamlInputUnknownKey: String

        fun yamlInputParsed(methodFqn: DtoMethodFqn): Rule
        fun yamlInputAdvancedParsed(methodFqn: DtoMethodFqn): Rule
    }

    private object SourceData : RuleData<DtoTaintSource> {
        override val yamlInput = "{ $k_addTo: $k_return, $k_marks: [] }"
        override val yamlInputAdvanced = "{ $k_signature: [], $k_conditions: {}, $k_addTo: $k_return, $k_marks: [] }"
        override val yamlInputInvalid = "{ invalid-key: $k_return, $k_marks: [] }"
        override val yamlInputUnknownKey = "{ $k_addTo: $k_return, $k_marks: [], unknown-key: [] }"

        override fun yamlInputParsed(methodFqn: DtoMethodFqn) =
            DtoTaintSource(methodFqn, DtoTaintEntitiesSet(setOf(DtoTaintEntityReturn)), DtoTaintMarksAll)

        override fun yamlInputAdvancedParsed(methodFqn: DtoMethodFqn) =
            DtoTaintSource(methodFqn, DtoTaintEntitiesSet(setOf(DtoTaintEntityReturn)), DtoTaintMarksAll, DtoTaintSignatureList(listOf()), DtoNoTaintConditions)
    }

    private object PassData : RuleData<DtoTaintPass> {
        override val yamlInput = "{ $k_getFrom: $k_this, $k_addTo: $k_return, $k_marks: [] }"
        override val yamlInputAdvanced = "{ $k_signature: [], $k_conditions: {}, $k_getFrom: $k_this, $k_addTo: $k_return, $k_marks: [] }"
        override val yamlInputInvalid = "{ $k_getFrom: $k_this, $k_addTo: $k_return, invalid-key: [] }"
        override val yamlInputUnknownKey = "{ $k_getFrom: $k_this, $k_addTo: $k_return, $k_marks: [], unknown-key: [] }"

        override fun yamlInputParsed(methodFqn: DtoMethodFqn) =
            DtoTaintPass(methodFqn, DtoTaintEntitiesSet(setOf(DtoTaintEntityThis)), DtoTaintEntitiesSet(setOf(DtoTaintEntityReturn)), DtoTaintMarksAll)

        override fun yamlInputAdvancedParsed(methodFqn: DtoMethodFqn) =
            DtoTaintPass(methodFqn, DtoTaintEntitiesSet(setOf(DtoTaintEntityThis)), DtoTaintEntitiesSet(setOf(DtoTaintEntityReturn)), DtoTaintMarksAll, DtoTaintSignatureList(listOf()), DtoNoTaintConditions)
    }

    private object CleanerData : RuleData<DtoTaintCleaner> {
        override val yamlInput = "{ $k_removeFrom: $k_this, $k_marks: [] }"
        override val yamlInputAdvanced = "{ $k_signature: [], $k_conditions: {}, $k_removeFrom: $k_this, $k_marks: [] }"
        override val yamlInputInvalid = "{ $k_removeFrom: $k_this, invalid-key: [] }"
        override val yamlInputUnknownKey = "{ $k_removeFrom: $k_this, $k_marks: [], unknown-key: [] }"

        override fun yamlInputParsed(methodFqn: DtoMethodFqn) =
            DtoTaintCleaner(methodFqn, DtoTaintEntitiesSet(setOf(DtoTaintEntityThis)), DtoTaintMarksAll)

        override fun yamlInputAdvancedParsed(methodFqn: DtoMethodFqn) =
            DtoTaintCleaner(methodFqn, DtoTaintEntitiesSet(setOf(DtoTaintEntityThis)), DtoTaintMarksAll, DtoTaintSignatureList(listOf()), DtoNoTaintConditions)
    }

    private object SinkData : RuleData<DtoTaintSink> {
        override val yamlInput = "{ $k_check: $k_this, $k_marks: [] }"
        override val yamlInputAdvanced = "{ $k_signature: [], $k_conditions: {}, $k_check: $k_this, $k_marks: [] }"
        override val yamlInputInvalid = "{ $k_check: $k_this, invalid-key: [] }"
        override val yamlInputUnknownKey = "{ $k_check: $k_this, $k_marks: [], unknown-key: [] }"

        override fun yamlInputParsed(methodFqn: DtoMethodFqn) =
            DtoTaintSink(methodFqn, DtoTaintEntitiesSet(setOf(DtoTaintEntityThis)), DtoTaintMarksAll)

        override fun yamlInputAdvancedParsed(methodFqn: DtoMethodFqn) =
            DtoTaintSink(methodFqn, DtoTaintEntitiesSet(setOf(DtoTaintEntityThis)), DtoTaintMarksAll, DtoTaintSignatureList(listOf()), DtoNoTaintConditions)
    }

    // combined rules

    private object SourcesData : RulesData<DtoTaintSource>(SourceData)
    private object PassesData : RulesData<DtoTaintPass>(PassData)
    private object CleanersData : RulesData<DtoTaintCleaner>(CleanerData)
    private object SinksData : RulesData<DtoTaintSink>(SinkData)

    private open class RulesData<Rule>(ruleData: RuleData<Rule>) {
        val yamlInput = """
            - a.b.c.m: ${ruleData.yamlInput}
            - d.e.f.m: ${ruleData.yamlInputAdvanced}
        """.trimIndent()

        val yamlInputNestedNames = """
            - a:
                - b.c:
                    - m: ${ruleData.yamlInput}
                    - m: ${ruleData.yamlInputAdvanced}
                - d:
                    - e.m1: ${ruleData.yamlInput}
                    - e.m2: ${ruleData.yamlInputAdvanced}
        """.trimIndent()

        val yamlInputInvalid = """
            - a:
                - b.c:
                    m: ${ruleData.yamlInput}
        """.trimIndent()

        val yamlInputParsed = listOf(
            ruleData.yamlInputParsed(DtoMethodFqn(listOf("a", "b"), "c", "m")),
            ruleData.yamlInputAdvancedParsed(DtoMethodFqn(listOf("d", "e"), "f", "m"))
        )

        val yamlInputNestedNamesParsed = listOf(
            ruleData.yamlInputParsed(DtoMethodFqn(listOf("a", "b"), "c", "m")),
            ruleData.yamlInputAdvancedParsed(DtoMethodFqn(listOf("a", "b"), "c", "m")),
            ruleData.yamlInputParsed(DtoMethodFqn(listOf("a", "d"), "e", "m1")),
            ruleData.yamlInputAdvancedParsed(DtoMethodFqn(listOf("a", "d"), "e", "m2"))
        )
    }
}