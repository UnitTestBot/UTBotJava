package org.utbot.language.ts.parser.ast

import com.eclipsesource.v8.V8Object
import org.utbot.language.ts.parser.TsParserUtils.getArrayAsList
import org.utbot.language.ts.parser.TsParserUtils.getAstNodeByKind
import org.utbot.language.ts.parser.TsParserUtils.getChildren

class ConstructorNode(
    obj: V8Object,
    override val parent: AstNode?
): FunctionNode() {

    override val children = obj.getChildren().map { it.getAstNodeByKind(this) }

    override val name: Lazy<String> = lazy { (parent as? ClassDeclarationNode)?.name ?: throw IllegalStateException() }

    @Suppress("UNCHECKED_CAST")
    override val parameters = obj.getArrayAsList("parameters").map { it.getAstNodeByKind(this) }
            as List<ParameterNode>

    override val body = obj.getObject("body").getArrayAsList("statements")
        .map { it.getAstNodeByKind(this) }

    override val returnType: Lazy<TypeNode> = lazy { CustomTypeNode(obj, parent = this, typeLiteral = name.value) }
}