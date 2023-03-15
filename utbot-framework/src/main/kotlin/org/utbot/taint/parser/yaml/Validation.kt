package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class TaintParseError(
    message: String,
    node: YamlNode? = null
) : RuntimeException(message + (if (node != null) "\nYaml node: ${node.contentToString()}" else ""))

@ExperimentalContracts
fun validate(condition: Boolean, reason: String, node: YamlNode? = null) {
    contract {
        returns() implies condition
    }
    if (!condition) {
        throw TaintParseError(reason, node)
    }
}

fun validateYamlMapKeys(node: YamlMap, allowedKeys: Set<String>) {
    val actualKeys = node.entries.keys.map { it.content }.toSet()
    val unknownKeys = actualKeys - allowedKeys
    if (unknownKeys.isNotEmpty()) {
        throw TaintParseError("Unknown keys was encountered: $unknownKeys", node)
    }
}
