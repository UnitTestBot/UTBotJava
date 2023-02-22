package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import org.utbot.taint.parser.model.*
import org.utbot.taint.parser.yaml.ConditionParser.parseConditionsKey
import org.utbot.taint.parser.yaml.SignatureParser.parseSignatureKey
import org.utbot.taint.parser.yaml.TaintEntityParser.parseTaintEntities
import org.utbot.taint.parser.yaml.TaintMarkParser.parseTaintMarks
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object RuleParser {

    /**
     * Recursively parses source rules.
     * @see parseRules
     */
    fun parseSources(node: YamlNode): List<Source> =
        parseRules(node, RuleParser::isSourceRule, RuleParser::parseSourceRule)

    /**
     * Recursively parses pass-through rules.
     * @see parseRules
     */
    fun parsePasses(node: YamlNode): List<Pass> =
        parseRules(node, RuleParser::isPassRule, RuleParser::parsePassRule)

    /**
     * Recursively parses cleaner rules.
     * @see parseRules
     */
    fun parseCleaners(node: YamlNode): List<Cleaner> =
        parseRules(node, RuleParser::isCleanerRule, RuleParser::parseCleanerRule)

    /**
     * Recursively parses sink rules.
     * @see parseRules
     */
    fun parseSinks(node: YamlNode): List<Sink> =
        parseRules(node, RuleParser::isSinkRule, RuleParser::parseSinkRule)

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
     * Checks that the [node] can be parsed to [Source].
     */
    fun isSourceRule(node: YamlNode): Boolean =
        node.containsKey(Constants.KEY_ADD_TO) && node.containsKey(Constants.KEY_MARKS)

    /**
     * Checks that the [node] can be parsed to [Pass].
     */
    fun isPassRule(node: YamlNode): Boolean =
        node.containsKey(Constants.KEY_GET_FROM) && node.containsKey(Constants.KEY_ADD_TO) && node.containsKey(Constants.KEY_MARKS)

    /**
     * Checks that the [node] can be parsed to [Cleaner].
     */
    fun isCleanerRule(node: YamlNode): Boolean =
        node.containsKey(Constants.KEY_REMOVE_FROM) && node.containsKey(Constants.KEY_MARKS)

    /**
     * Checks that the [node] can be parsed to [Sink].
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
    fun parseSourceRule(node: YamlNode, methodNameParts: List<String>): Source {
        validate(node is YamlMap, "The source-rule node should be a map", node)
        validateYamlMapKeys(node, setOf(Constants.KEY_SIGNATURE, Constants.KEY_CONDITIONS, Constants.KEY_ADD_TO, Constants.KEY_MARKS))

        val methodFqn = getMethodFqnFromParts(methodNameParts)
        val signature = parseSignatureKey(node)
        val conditions = parseConditionsKey(node)
        val addTo = parseTaintEntities(node[Constants.KEY_ADD_TO]!!)
        val marks = parseTaintMarks(node[Constants.KEY_MARKS]!!)

        return Source(methodFqn, addTo, marks, signature, conditions)
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
    fun parsePassRule(node: YamlNode, methodNameParts: List<String>): Pass {
        validate(node is YamlMap, "The pass-rule node should be a map", node)
        validateYamlMapKeys(node, setOf(Constants.KEY_SIGNATURE, Constants.KEY_CONDITIONS, Constants.KEY_GET_FROM, Constants.KEY_ADD_TO, Constants.KEY_MARKS))

        val methodFqn = getMethodFqnFromParts(methodNameParts)
        val signature = parseSignatureKey(node)
        val conditions = parseConditionsKey(node)
        val getFrom = parseTaintEntities(node[Constants.KEY_GET_FROM]!!)
        val addTo = parseTaintEntities(node[Constants.KEY_ADD_TO]!!)
        val marks = parseTaintMarks(node[Constants.KEY_MARKS]!!)

        return Pass(methodFqn, getFrom, addTo, marks, signature, conditions)
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
    fun parseCleanerRule(node: YamlNode, methodNameParts: List<String>): Cleaner {
        validate(node is YamlMap, "The cleaner-rule node should be a map", node)
        validateYamlMapKeys(node, setOf(Constants.KEY_SIGNATURE, Constants.KEY_CONDITIONS, Constants.KEY_REMOVE_FROM, Constants.KEY_MARKS))

        val methodFqn = getMethodFqnFromParts(methodNameParts)
        val signature = parseSignatureKey(node)
        val conditions = parseConditionsKey(node)
        val removeFrom = parseTaintEntities(node[Constants.KEY_REMOVE_FROM]!!)
        val marks = parseTaintMarks(node[Constants.KEY_MARKS]!!)

        return Cleaner(methodFqn, removeFrom, marks, signature, conditions)
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
    fun parseSinkRule(node: YamlNode, methodNameParts: List<String>): Sink {
        validate(node is YamlMap, "The sink-rule node should be a map", node)
        validateYamlMapKeys(node, setOf(Constants.KEY_SIGNATURE, Constants.KEY_CONDITIONS, Constants.KEY_CHECK, Constants.KEY_MARKS))

        val methodFqn = getMethodFqnFromParts(methodNameParts)
        val signature = parseSignatureKey(node)
        val conditions = parseConditionsKey(node)
        val check = parseTaintEntities(node[Constants.KEY_CHECK]!!)
        val marks = parseTaintMarks(node[Constants.KEY_MARKS]!!)

        return Sink(methodFqn, check, marks, signature, conditions)
    }

    /**
     * Constructs [MethodFqn] from the given [methodNameParts].
     *
     * __Input example:__
     *
     * `["org.example", "server", "Controller", "start"]`
     */
    private fun getMethodFqnFromParts(methodNameParts: List<String>): MethodFqn {
        val parts = methodNameParts.flatMap { it.split('.') }
        validate(parts.size >= 2, "Method fqn should contain at least the class name and the method name")
        val packageNames = parts.dropLast(2)
        val (className, methodName) = parts.takeLast(2)
        return MethodFqn(packageNames, className, methodName)
    }
}