package org.utbot.taint.parser

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlException
import org.utbot.taint.parser.model.DtoTaintConfiguration
import org.utbot.taint.parser.yaml.TaintConfigurationParser
import org.utbot.taint.parser.yaml.TaintParseError

/**
 * YAML configuration file parser.
 */
object TaintYamlParser {
    /**
     * Parses the YAML configuration file to our data classes.
     *
     * @throws YamlException
     * @throws TaintParseError
     */
    fun parse(yamlInput: String): DtoTaintConfiguration {
        val rootNode = Yaml.default.parseToYamlNode(yamlInput)
        return TaintConfigurationParser.parseConfiguration(rootNode)
    }
}