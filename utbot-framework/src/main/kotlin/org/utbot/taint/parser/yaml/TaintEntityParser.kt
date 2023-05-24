package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlScalar
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object TaintEntityParser {

    /**
     * Expects a [YamlScalar] (single taint entity) or a [YamlList] (several taint entities).
     *
     * __Input example:__
     *
     * `[ this, arg1, arg7, return ]`
     */
    fun parseTaintEntities(node: YamlNode): DtoTaintEntities =
        when (node) {
            is YamlScalar -> {
                DtoTaintEntitiesSet(setOf(taintEntityByName(node.content)))
            }
            is YamlList -> {
                validate(node.items.isNotEmpty(), "The taint entities set should contain at least one value", node)
                val entities = node.items.map { innerNode ->
                    validate(innerNode is YamlScalar, "The taint entity name should be a scalar", node)
                    taintEntityByName(innerNode.content)
                }
                DtoTaintEntitiesSet(entities.toSet())
            }
            else -> {
                throw TaintParseError("The taint-entities node should be a scalar or a list", node)
            }
        }

    /**
     * Constructs [DtoTaintEntity] by the given [name] &ndash "this", "return" or "argN".
     */
    fun taintEntityByName(name: String): DtoTaintEntity =
        when (name) {
            Constants.KEY_THIS -> DtoTaintEntityThis
            Constants.KEY_RETURN -> DtoTaintEntityReturn
            else -> {
                val index = name.removePrefix(Constants.KEY_ARG).toUIntOrNull()
                    ?: throw TaintParseError("Method argument should be like `arg` + index, but is `$name`")
                validate(index >= 1u, "Method arguments indexes are numbered from one, but index = `$index`")
                DtoTaintEntityArgument(index)
            }
        }
}