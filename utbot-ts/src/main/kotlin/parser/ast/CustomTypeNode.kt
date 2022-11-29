package parser.ast

import com.eclipsesource.v8.V8Object

class CustomTypeNode(
    obj: V8Object,
    typeLiteral: String? = null,
    override val parent: AstNode?
): TypeNode() {

    override val stringTypeName: String = typeLiteral ?: obj.getObject("typeName").getString("escapedText")
}