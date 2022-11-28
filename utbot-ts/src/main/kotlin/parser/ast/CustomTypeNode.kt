package parser.ast

import com.eclipsesource.v8.V8Object

class CustomTypeNode(
    obj: V8Object,
    override val parent: AstNode?
): TypeNode() {

    override val stringTypeName: String = obj.getObject("typeName").getString("escapedText")
}