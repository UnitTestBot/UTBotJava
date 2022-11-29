package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getKind

abstract class TypeNode: AstNode() {

    override val children: List<AstNode> = emptyList()

    abstract val stringTypeName: String
}

fun V8Object.getTypeNode(parent: AstNode?): TypeNode {
    return this.getKind().let { kind ->
        if (kind == "FunctionType") return FunctionTypeNode(this, parent = parent)
        if (kind == "Undefined") return BaseTypeNode(this, "Undefined", parent = parent)
        if (kind.contains("keyword", true)) {
            BaseTypeNode(this, parent = parent)
        } else CustomTypeNode(this, parent = parent)
    }
}