package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlException

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