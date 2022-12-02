package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getArrayAsList
import parser.TsParserUtils.getAstNodeByKind
import parser.TsParserUtils.getChildren

class ArrowFunctionNode(
    obj: V8Object,
    override val parent: AstNode?
): FunctionNode() {

    override val children = obj.getChildren().map { it.getAstNodeByKind(this) }

    override val name: Lazy<String> = lazy {
        if (parent is VariableDeclarationNode) parent.name else ""
    }

    @Suppress("UNCHECKED_CAST")
    override val parameters = obj.getArrayAsList("parameters").map { it.getAstNodeByKind(this) }
            as List<ParameterNode>

    override val body = obj.getObject("body").getArrayAsList("statements")
        .map { it.getAstNodeByKind(this) }

    override val returnType: Lazy<TypeNode> = lazy { CustomTypeNode(obj, parent = this, typeLiteral = name.value) }
}