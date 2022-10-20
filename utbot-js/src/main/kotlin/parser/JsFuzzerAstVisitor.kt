package parser

import com.oracle.js.parser.ir.BinaryNode
import com.oracle.js.parser.ir.CaseNode
import com.oracle.js.parser.ir.LexicalContext
import com.oracle.js.parser.ir.LiteralNode
import com.oracle.js.parser.ir.Node
import com.oracle.js.parser.ir.visitor.NodeVisitor
import com.oracle.truffle.api.strings.TruffleString
import org.utbot.framework.plugin.api.js.util.jsBooleanClassId
import org.utbot.framework.plugin.api.js.util.jsNumberClassId
import org.utbot.framework.plugin.api.js.util.jsStringClassId
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedOp
import org.utbot.fuzzer.types.ClassIdWrapper

class JsFuzzerAstVisitor : NodeVisitor<LexicalContext>(LexicalContext()) {
    private var lastFuzzedOpGlobal = FuzzedOp.NONE

    val fuzzedConcreteValues = mutableSetOf<FuzzedConcreteValue>()
    override fun enterCaseNode(caseNode: CaseNode?): Boolean {
        caseNode?.test?.let {
            validateNode(it)
        }
        return true
    }

    override fun enterBinaryNode(binaryNode: BinaryNode?): Boolean {
        binaryNode?.let { binNode ->
            val compOp = """>=|<=|>|<|==|!=""".toRegex()
            val curOp = compOp.find(binNode.toString())?.value
            val currentFuzzedOp = FuzzedOp.values().find { curOp == it.sign } ?: FuzzedOp.NONE
            lastFuzzedOpGlobal = currentFuzzedOp
            validateNode(binNode.lhs)
            lastFuzzedOpGlobal = lastFuzzedOpGlobal.reverseOrElse { FuzzedOp.NONE }
            validateNode(binNode.rhs)
        }
        return true
    }

    private fun validateNode(literalNode: Node) {
        if (literalNode !is LiteralNode<*>) return
        when (literalNode.value) {
            is TruffleString -> {
                fuzzedConcreteValues.add(
                    FuzzedConcreteValue(
                        ClassIdWrapper(jsStringClassId),
                        literalNode.value.toString(),
                        lastFuzzedOpGlobal
                    )
                )
            }

            is Boolean -> {
                fuzzedConcreteValues.add(
                    FuzzedConcreteValue(
                        ClassIdWrapper(jsBooleanClassId),
                        literalNode.value,
                        lastFuzzedOpGlobal
                    )
                )
            }

            is Int -> {
                fuzzedConcreteValues.add(FuzzedConcreteValue(ClassIdWrapper(jsNumberClassId), literalNode.value, lastFuzzedOpGlobal))
            }

            is Long -> {
                fuzzedConcreteValues.add(FuzzedConcreteValue(ClassIdWrapper(jsNumberClassId), literalNode.value, lastFuzzedOpGlobal))
            }

            is Double -> {
                fuzzedConcreteValues.add(FuzzedConcreteValue(ClassIdWrapper(jsNumberClassId), literalNode.value, lastFuzzedOpGlobal))
            }
        }
    }
}