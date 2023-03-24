package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import org.utbot.taint.parser.model.*
import org.utbot.taint.parser.yaml.TaintConditionParser.parseConditionsKey
import org.utbot.taint.parser.yaml.TaintSignatureParser.parseSignatureKey
import org.utbot.taint.parser.yaml.TaintEntityParser.parseTaintEntities
import org.utbot.taint.parser.yaml.TaintMarkParser.parseTaintMarks
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object TaintRuleParser {

    /**
     * Recursively parses source rules.
     * @see parseRules
     */
    fun parseSources(node: YamlNode): List<DtoTaintSource> =
        parseRules(node, TaintRuleParser::isSourceRule, TaintRuleParser::parseSourceRule)

    /**
     * Recursively parses pass-through rules.
     * @see parseRules
     */
    fun parsePasses(node: YamlNode): List<DtoTaintPass> =
        parseRules(node, TaintRuleParser::isPassRule, TaintRuleParser::parsePassRule)

    /**
     * Recursively parses cleaner rules.
     * @see parseRules
     */
    fun parseCleaners(node: YamlNode): List<DtoTaintCleaner> =
        parseRules(node, TaintRuleParser::isCleanerRule, TaintRuleParser::parseCleanerRule)

    /**
     * Recursively parses sink rules.
     * @see parseRules
     */
    fun parseSinks(node: YamlNode): List<DtoTaintSink> =
        parseRules(node, TaintRuleParser::isSinkRule, TaintRuleParser::parseSinkRule)

    /**
     * Recursively parses rules (sources/passes/cleaners/sinks).
     *
     * Expects a [YamlMap] (single rule) or a YamlList (several rules).
     *
     * __Input example:__
     *
     * ```yaml
     * - java.lang.String:
     *     - isEmpty: { ... }
     *     - concat: { ... }
     * ```
     */
    private fun <Rule> parseRules(
        node: YamlNode,
        isRule: YamlNode.() -> Boolean,
        parseRule: (YamlNode, List<String>) -> Rule,
        currentPath: List<String> = listOf()
    ): List<Rule> {
        if (node.isRule()) {
            val rule = parseRule(node, currentPath)
            return listOf(rule)
        }

        validate(node is YamlList, "The rules should be stored as a list", node)
        return node.items.flatMap { innerNode ->
            validate(innerNode is YamlMap, "The rule node should be a map with the key `method name part`", innerNode)
            validate(innerNode.entries.size == 1, "The rule map should contain only one key", innerNode)
            val (nextNamePart, nextNode) = innerNode.entries.toList().first()
            parseRules(nextNode, isRule, parseRule, currentPath = currentPath + nextNamePart.content)
        }
    }


    /**
     * Checks that the [node] can be parsed to [DtoTaintSource].
     */
    fun isSourceRule(node: YamlNode): Boolean =
        node.containsKey(Constants.KEY_ADD_TO) && node.containsKey(Constants.KEY_MARKS)

    /**
     * Checks that the [node] can be parsed to [DtoTaintPass].
     */
    fun isPassRule(node: YamlNode): Boolean =
        node.containsKey(Constants.KEY_GET_FROM) && node.containsKey(Constants.KEY_ADD_TO) && node.containsKey(Constants.KEY_MARKS)

    /**
     * Checks that the [node] can be parsed to [DtoTaintCleaner].
     */
    fun isCleanerRule(node: YamlNode): Boolean =
        node.containsKey(Constants.KEY_REMOVE_FROM) && node.containsKey(Constants.KEY_MARKS)

    /**
     * Checks that the [node] can be parsed to [DtoTaintSink].
     */
    fun isSinkRule(node: YamlNode): Boolean =
        node.containsKey(Constants.KEY_CHECK) && node.containsKey(Constants.KEY_MARKS)

    /**
     * Checks that [this] is [YamlMap] and contains [key] as a key.
     */
    private fun YamlNode.containsKey(key: String): Boolean =
        (this as? YamlMap)?.get<YamlNode>(key) != null


    /**
     * This method is expected to be called only if the [isSourceRule] method returned `true`.
     *
     * __Input example:__
     *
     * ```yaml
     * signature: [ ... ]
     * conditions: [ ... ]
     * add-to: ...
     * marks: ...
     * ```
     */
    fun parseSourceRule(node: YamlNode, methodNameParts: List<String>): DtoTaintSource {
        validate(node is YamlMap, "The source-rule node should be a map", node)
        validateYamlMapKeys(node, setOf(Constants.KEY_SIGNATURE, Constants.KEY_CONDITIONS, Constants.KEY_ADD_TO, Constants.KEY_MARKS))

        val methodFqn = getMethodFqnFromParts(methodNameParts)
        val signature = parseSignatureKey(node)
        val conditions = parseConditionsKey(node)
        val addTo = parseTaintEntities(node[Constants.KEY_ADD_TO]!!)
        val marks = parseTaintMarks(node[Constants.KEY_MARKS]!!)

        return DtoTaintSource(methodFqn, addTo, marks, signature, conditions)
    }

    /**
     * This method is expected to be called only if the [isPassRule] method returned `true`.
     *
     * __Input example:__
     *
     * ```yaml
     * signature: [ ... ]
     * conditions: [ ... ]
     * get-from: [ ... ]
     * add-to: ...
     * marks: ...
     * ```
     */
    fun parsePassRule(node: YamlNode, methodNameParts: List<String>): DtoTaintPass {
        validate(node is YamlMap, "The pass-rule node should be a map", node)
        validateYamlMapKeys(node, setOf(Constants.KEY_SIGNATURE, Constants.KEY_CONDITIONS, Constants.KEY_GET_FROM, Constants.KEY_ADD_TO, Constants.KEY_MARKS))

        val methodFqn = getMethodFqnFromParts(methodNameParts)
        val signature = parseSignatureKey(node)
        val conditions = parseConditionsKey(node)
        val getFrom = parseTaintEntities(node[Constants.KEY_GET_FROM]!!)
        val addTo = parseTaintEntities(node[Constants.KEY_ADD_TO]!!)
        val marks = parseTaintMarks(node[Constants.KEY_MARKS]!!)

        return DtoTaintPass(methodFqn, getFrom, addTo, marks, signature, conditions)
    }

    /**
     * This method is expected to be called only if the [isCleanerRule] method returned `true`.
     *
     * __Input example:__
     *
     * ```yaml
     * signature: [ ... ]
     * conditions: [ ... ]
     * remove-from: ...
     * marks: ...
     * ```
     */
    fun parseCleanerRule(node: YamlNode, methodNameParts: List<String>): DtoTaintCleaner {
        validate(node is YamlMap, "The cleaner-rule node should be a map", node)
        validateYamlMapKeys(node, setOf(Constants.KEY_SIGNATURE, Constants.KEY_CONDITIONS, Constants.KEY_REMOVE_FROM, Constants.KEY_MARKS))

        val methodFqn = getMethodFqnFromParts(methodNameParts)
        val signature = parseSignatureKey(node)
        val conditions = parseConditionsKey(node)
        val removeFrom = parseTaintEntities(node[Constants.KEY_REMOVE_FROM]!!)
        val marks = parseTaintMarks(node[Constants.KEY_MARKS]!!)

        return DtoTaintCleaner(methodFqn, removeFrom, marks, signature, conditions)
    }

    /**
     * This method is expected to be called only if the [isSinkRule] method returned `true`.
     *
     * __Input example:__
     *
     * ```yaml
     * signature: [ ... ]
     * conditions: [ ... ]
     * check: ...
     * marks: ...
     * ```
     */
    fun parseSinkRule(node: YamlNode, methodNameParts: List<String>): DtoTaintSink {
        validate(node is YamlMap, "The sink-rule node should be a map", node)
        validateYamlMapKeys(node, setOf(Constants.KEY_SIGNATURE, Constants.KEY_CONDITIONS, Constants.KEY_CHECK, Constants.KEY_MARKS))

        val methodFqn = getMethodFqnFromParts(methodNameParts)
        val signature = parseSignatureKey(node)
        val conditions = parseConditionsKey(node)
        val check = parseTaintEntities(node[Constants.KEY_CHECK]!!)
        val marks = parseTaintMarks(node[Constants.KEY_MARKS]!!)

        return DtoTaintSink(methodFqn, check, marks, signature, conditions)
    }

    /**
     * Constructs [DtoMethodFqn] from the given [methodNameParts].
     *
     * __Input example:__
     *
     * `["org.example", "server", "Controller", "start"]`
     */
    private fun getMethodFqnFromParts(methodNameParts: List<String>): DtoMethodFqn {
        val parts = methodNameParts.flatMap { it.split('.') }
        validate(parts.size >= 2, "Method fqn should contain at least the class name and the method name")
        val packageNames = parts.dropLast(2)
        val (className, methodName) = parts.takeLast(2)
        return DtoMethodFqn(packageNames, className, methodName)
    }
}