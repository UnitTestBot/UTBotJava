package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import org.utbot.taint.parser.model.DtoTaintSignatureAny
import org.utbot.taint.parser.model.DtoTaintSignature
import org.utbot.taint.parser.model.DtoTaintSignatureList
import org.utbot.taint.parser.yaml.MethodArgumentParser.isArgumentType
import org.utbot.taint.parser.yaml.MethodArgumentParser.parseArgumentType
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object TaintSignatureParser {

    /**
     * Expects a [YamlMap] with (or without) a key [Constants.KEY_SIGNATURE].
     *
     * __Input example:__
     *
     * `signature: [ _, _, <java.lang.Object> ]`
     */
    fun parseSignatureKey(ruleMap: YamlMap): DtoTaintSignature =
        ruleMap.get<YamlNode>(Constants.KEY_SIGNATURE)?.let(TaintSignatureParser::parseSignature) ?: DtoTaintSignatureAny

    /**
     * Expects a [YamlList] with argument types as keys.
     *
     * __Input example:__
     *
     * `[ _, _, <java.lang.Object> ]`
     */
    fun parseSignature(node: YamlNode): DtoTaintSignature {
        validate(node is YamlList, "The signature node should be a list", node)
        validate(node.items.all(::isArgumentType), "All items should be argument types", node)
        val argumentTypes = node.items.map(::parseArgumentType)
        return DtoTaintSignatureList(argumentTypes)
    }
}