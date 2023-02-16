package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlScalar
import org.utbot.taint.parser.model.AllTaintMarks
import org.utbot.taint.parser.model.TaintMarksSet
import org.utbot.taint.parser.model.TaintMark
import org.utbot.taint.parser.model.TaintMarks
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
    fun parseTaintMarks(node: YamlNode): TaintMarks =
        when (node) {
            is YamlScalar -> {
                TaintMarksSet(setOf(TaintMark(node.content)))
            }
            is YamlList -> {
                if (node.items.isEmpty()) {
                    AllTaintMarks
                } else {
                    val marks = node.items.map { innerNode ->
                        validate(innerNode is YamlScalar, "The mark name should be a scalar", innerNode)
                        TaintMark(innerNode.content)
                    }
                    TaintMarksSet(marks.toSet())
                }
            }
            else -> {
                throw ConfigurationParseError("The marks node should be a scalar or a list", node)
            }
        }
}