package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlScalar
import org.utbot.taint.parser.model.DtoTaintMarksAll
import org.utbot.taint.parser.model.DtoTaintMarksSet
import org.utbot.taint.parser.model.DtoTaintMark
import org.utbot.taint.parser.model.DtoTaintMarks
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
    fun parseTaintMarks(node: YamlNode): DtoTaintMarks =
        when (node) {
            is YamlScalar -> {
                DtoTaintMarksSet(setOf(DtoTaintMark(node.content)))
            }
            is YamlList -> {
                if (node.items.isEmpty()) {
                    DtoTaintMarksAll
                } else {
                    val marks = node.items.map { innerNode ->
                        validate(innerNode is YamlScalar, "The mark name should be a scalar", innerNode)
                        DtoTaintMark(innerNode.content)
                    }
                    DtoTaintMarksSet(marks.toSet())
                }
            }
            else -> {
                throw TaintParseError("The marks node should be a scalar or a list", node)
            }
        }
}