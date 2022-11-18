package parser.ast

import com.eclipsesource.v8.V8Object

class CustomTypeNode(
    obj: V8Object,
): TypeNode() {

    override val stringTypeName: String = obj.getObject("typeName").getString("escapedText")
}