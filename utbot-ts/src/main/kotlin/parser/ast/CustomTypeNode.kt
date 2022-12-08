package parser.ast

import com.eclipsesource.v8.V8Object
import com.eclipsesource.v8.utils.V8ObjectUtils

class CustomTypeNode(
    obj: V8Object,
    typeLiteral: String? = null,
    override val parent: AstNode?
): TypeNode() {

    override val stringTypeName: String = typeLiteral ?: run {
        try {
            obj.getObject("typeName").getString("escapedText")
        } catch (e: Exception) {
            val a = V8ObjectUtils.toMap(obj)
            throw e
        }
    }
}