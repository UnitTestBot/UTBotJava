package parser

import com.oracle.js.parser.ir.ClassNode
import com.oracle.js.parser.ir.FunctionNode
import com.oracle.js.parser.ir.LexicalContext
import com.oracle.js.parser.ir.visitor.NodeVisitor

class JsToplevelFunctionAstVisitor : NodeVisitor<LexicalContext>(LexicalContext()) {

    val extractedMethods = mutableListOf<FunctionNode>()

    override fun enterClassNode(classNode: ClassNode?): Boolean {
        return false
    }

    override fun enterFunctionNode(functionNode: FunctionNode?): Boolean {
        functionNode?.let {
            extractedMethods += it
        }
        return false
    }
}