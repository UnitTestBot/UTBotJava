package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getAstNodeByKind
import parser.TsParserUtils.getChildren

class ConstructorNode(
    obj: V8Object,
    override val parent: AstNode?
): FunctionNode(obj, parent) {

    override val name: String = (parent as? ClassDeclarationNode)?.name ?: throw IllegalStateException()

    override val children = obj.getChildren().map { it.getAstNodeByKind(this) }

    override val returnType: TypeNode = CustomTypeNode(obj, parent = this, typeLiteral = name)
}