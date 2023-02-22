package org.utbot.taint.parser

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlException
import org.utbot.taint.parser.model.Configuration
import org.utbot.taint.parser.yaml.ConfigurationParseError
import org.utbot.taint.parser.yaml.ConfigurationParser.parseConfiguration

/**
 * YAML configuration file parser.
 */
object TaintAnalysisConfigurationParser {
    /**
     * Parses the YAML configuration file to our data classes.
     *
     * @throws YamlException
     * @throws ConfigurationParseError
     */
    fun parse(yamlInput: String): Configuration {
        val rootNode = Yaml.default.parseToYamlNode(yamlInput)
        return parseConfiguration(rootNode)
    }
}