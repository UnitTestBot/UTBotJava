package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getKind

abstract class TypeNode: AstNode() {

    override val children: List<AstNode> = emptyList()

    abstract val stringTypeName: String
}

fun V8Object.getTypeNode(typescript: V8Object): TypeNode {
    return this.getKind(typescript).let { kind ->
        if (kind == "Undefined") return BaseTypeNode(this, typescript, "Undefined")
        if (kind.contains("keyword", true)) {
            BaseTypeNode(this, typescript)
        } else CustomTypeNode(this, typescript)
    }
}