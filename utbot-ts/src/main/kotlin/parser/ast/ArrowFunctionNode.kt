package parser.ast

import com.eclipsesource.v8.V8Object

class ArrowFunctionNode(
    obj: V8Object,
    override val parent: AstNode?
): FunctionNode(obj, parent) {

    override val name: String = ""
}