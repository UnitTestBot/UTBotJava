package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import org.utbot.taint.parser.model.Configuration
import org.utbot.taint.parser.yaml.Constants.KEY_CLEANERS
import org.utbot.taint.parser.yaml.Constants.KEY_PASSES
import org.utbot.taint.parser.yaml.Constants.KEY_SINKS
import org.utbot.taint.parser.yaml.Constants.KEY_SOURCES
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object ConfigurationParser {

    /**
     * Expects a [YamlMap] with keys [KEY_SOURCES], [KEY_PASSES], [KEY_CLEANERS] and [KEY_SINKS].
     *
     * __Input example:__
     *
     * ```yaml
     * sources: [ ... ]
     * passes: [ ... ]
     * cleaners: [ ... ]
     * sinks: [ ... ]
     * ```
     */
    fun parseConfiguration(node: YamlNode): Configuration {
        validate(node is YamlMap, "The root node should be a map", node)
        validateYamlMapKeys(node, setOf(KEY_SOURCES, KEY_PASSES, KEY_CLEANERS, KEY_SINKS))

        val sourcesNode = node.get<YamlNode>(KEY_SOURCES)
        val passesNode = node.get<YamlNode>(KEY_PASSES)
        val cleanersNode = node.get<YamlNode>(KEY_CLEANERS)
        val sinksNode = node.get<YamlNode>(KEY_SINKS)

        val sources = sourcesNode?.let(RuleParser::parseSources) ?: listOf()
        val passes = passesNode?.let(RuleParser::parsePasses) ?: listOf()
        val cleaners = cleanersNode?.let(RuleParser::parseCleaners) ?: listOf()
        val sinks = sinksNode?.let(RuleParser::parseSinks) ?: listOf()

        return Configuration(sources, passes, cleaners, sinks)
    }
}