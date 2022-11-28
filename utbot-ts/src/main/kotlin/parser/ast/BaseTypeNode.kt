package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getKind

class BaseTypeNode(
    obj: V8Object,
    typeLiteral: String? = null,
    override val parent: AstNode?
): TypeNode() {

    override val stringTypeName = typeLiteral ?: obj.getKind()
}