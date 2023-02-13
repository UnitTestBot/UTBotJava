package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.*
import org.utbot.taint.parser.model.*
import org.utbot.taint.parser.yaml.MethodArgumentParser.isArgumentType
import org.utbot.taint.parser.yaml.MethodArgumentParser.isArgumentValue
import org.utbot.taint.parser.yaml.MethodArgumentParser.parseArgumentType
import org.utbot.taint.parser.yaml.MethodArgumentParser.parseArgumentValue
import org.utbot.taint.parser.yaml.TaintEntityParser.taintEntityByName
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object ConditionParser {

    /**
     * Expects a [YamlMap] with (or without) a key [Constants.KEY_CONDITIONS].
     *
     * __Input example:__
     *
     * ```yaml
     * conditions:
     *   this: <java.lang.String>
     *   arg1: [ "in", "out" ]
     *   arg2: 227
     *   return: [ "", { not: <java.lang.String> } ]
     * ```
     */
    fun parseConditionsKey(ruleMap: YamlMap): Conditions =
        ruleMap.get<YamlNode>(Constants.KEY_CONDITIONS)?.let(ConditionParser::parseConditions) ?: NoConditions

    /**
     * Expects a [YamlMap] with taint entities as keys.
     *
     * __Input example:__
     *
     * ```yaml
     * this: <java.lang.String>
     * arg1: [ "in", "out" ]
     * arg2: 227
     * return: [ "", { not: <java.lang.String> } ]
     * ```
     */
    fun parseConditions(node: YamlNode): Conditions {
        validate(node is YamlMap, "The conditions node should be a map", node)
        return if (node.entries.isEmpty()) {
            NoConditions
        } else {
            val entityToCondition = node.entries.map { (taintEntityNameScalar, conditionNode) ->
                taintEntityByName(taintEntityNameScalar.content) to parseCondition(conditionNode)
            }.toMap()
            ConditionsMap(entityToCondition)
        }
    }

    /**
     * Expects a [YamlNode] that describes one condition.
     */
    fun parseCondition(node: YamlNode): Condition =
        when (node) {
            // example: `null`
            is YamlNull -> {
                ValueCondition(parseArgumentValue(node))
            }

            // examples: `227`, `"some string"`, `<java.lang.String>`
            is YamlScalar -> {
                when {
                    isArgumentType(node) -> TypeCondition(parseArgumentType(node))
                    isArgumentValue(node) -> ValueCondition(parseArgumentValue(node))
                    else -> throw ConfigurationParseError("The condition scalar should be a type or a value", node)
                }
            }

            // examples: `[ true, 1, "yes" ]`, `[ "", { not: <java.lang.String> } ]`
            is YamlList -> {
                val innerConditions = node.items.map(ConditionParser::parseCondition)
                OrCondition(innerConditions)
            }

            // examples: `{ not: null }`, `{ not: [1, 2, 3] }`
            is YamlMap -> {
                validateYamlMapKeys(node, setOf(Constants.KEY_CONDITION_NOT))
                val innerNode = node.get<YamlNode>(Constants.KEY_CONDITION_NOT)!!
                val innerCondition = parseCondition(innerNode)
                NotCondition(innerCondition)
            }

            else -> {
                throw ConfigurationParseError("The condition node has an unknown type", node)
            }
        }
}