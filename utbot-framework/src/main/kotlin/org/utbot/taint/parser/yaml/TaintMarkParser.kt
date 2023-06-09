package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlScalar
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object TaintMarkParser {

    /**
     * Expects a [YamlScalar] (single mark) or a [YamlList] (several marks).
     *
     * __Input example:__
     *
     * `[ sensitive-data, xss ]`
     */
    fun parseTaintMarks(node: YamlNode): YamlTaintMarks =
        when (node) {
            is YamlScalar -> {
                YamlTaintMarksSet(setOf(YamlTaintMark(node.content)))
            }

            is YamlList -> {
                if (node.items.isEmpty()) {
                    YamlTaintMarksAll
                } else {
                    val marks = node.items.map { innerNode ->
                        validate(innerNode is YamlScalar, "The mark name should be a scalar", innerNode)
                        YamlTaintMark(innerNode.content)
                    }
                    YamlTaintMarksSet(marks.toSet())
                }
            }

            else -> {
                throw TaintParseError("The marks node should be a scalar or a list", node)
            }
        }
}