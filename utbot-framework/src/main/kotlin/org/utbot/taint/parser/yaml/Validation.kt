package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.YamlNode
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class ConfigurationParseError(
    message: String,
    node: YamlNode? = null
) : RuntimeException(message + (if (node != null) "\nYaml node: ${node.contentToString()}" else ""))

@ExperimentalContracts
fun validate(condition: Boolean, reason: String, node: YamlNode? = null) {
    contract {
        returns() implies condition
    }
    if (!condition) {
        throw ConfigurationParseError(reason, node)
    }
}
