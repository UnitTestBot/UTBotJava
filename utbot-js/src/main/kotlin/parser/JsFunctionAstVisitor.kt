package parser

import com.oracle.js.parser.ir.ClassNode
import com.oracle.js.parser.ir.FunctionNode
import com.oracle.js.parser.ir.LexicalContext
import com.oracle.js.parser.ir.visitor.NodeVisitor

class JsFunctionAstVisitor(
    private val target: String,
    private val className: String?
) : NodeVisitor<LexicalContext>(LexicalContext()) {

    private var lastVisitedClassName: String = ""
    lateinit var targetFunctionNode: FunctionNode

    override fun enterClassNode(classNode: ClassNode?): Boolean {
        classNode?.let {
            lastVisitedClassName = it.ident.name.toString()
        }
        return true
    }

    override fun enterFunctionNode(functionNode: FunctionNode?): Boolean {
        functionNode?.let {
            if (it.name.toString() == target && (className ?: "") == lastVisitedClassName) {
                targetFunctionNode = it
                return false
            }
        }
        return true
    }
}