package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getArrayAsList
import parser.TsParserUtils.getKind

class ImportDeclarationNode(
    obj: V8Object,
    override val parent: AstNode?
): AstNode() {

    override val children: List<AstNode> = emptyList()

    val importText: String = when (obj.getKind()) {
        "ImportDeclaration" -> obj.getObject("moduleSpecifier").getString("text")
        "VariableDeclaration" -> obj.getObject("initializer")
            .getArrayAsList("arguments")
            .first()
            .getString("text")
        else -> throw IllegalStateException("Can't extract import text from ${obj.getKind()}")
    }

    val nameBindings: List<String> = when(obj.getKind()) {
        "ImportDeclaration" -> obj.getObject("importClause")
            .getObject("namedBindings")
            .getArrayAsList("elements").map {
                it.getObject("name").getString("escapedText")
            }
        "VariableDeclaration" -> listOf(obj.getObject("name").getString("escapedText"))
        else -> throw IllegalStateException("Can't extract name bindings from ${obj.getKind()}")
    }

    val importedNodes = mutableMapOf<String, AstNode>()

}