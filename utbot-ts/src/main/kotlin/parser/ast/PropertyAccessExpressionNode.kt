package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.findParentOfType
import parser.TsParserUtils.getAstNodeByKind
import parser.TsParserUtils.getChildren
import parser.TsParserUtils.getKind

class PropertyAccessExpressionNode(
    obj: V8Object,
    override val parent: AstNode?
) : AstNode() {

    override val children = obj.getChildren().map { it.getAstNodeByKind(parent) }

    val type = obj.getObject("type").getTypeNode(parent)

    val propertyName: String = obj.getObject("name").getString("escapedText")

    val parentName = obj.getObject("expression").getAstNodeByKind(this)

    val accessChain = lazy { buildAccessChain(obj, emptyList()) }

    val className: String = ""

    private tailrec fun buildAccessChain(currentObj: V8Object, acc: List<String>): List<String> {
        if (currentObj.getKind() == "ThisKeyword") {
            return listOf(
                this.findParentOfType(ClassDeclarationNode::class.java)?.name ?: throw IllegalStateException(),
                *acc.toTypedArray()
            )
        }
        val newList = when {
            currentObj.contains("expression") && currentObj.getObject("expression")
                .contains("escapedText") -> listOf(
                currentObj.getObject("expression").getString("escapedText"),
                *acc.toTypedArray()
            )

            else -> acc
        } + run {
            if (currentObj.contains("name")) {
                listOf(currentObj.getObject("name").getString("escapedText"))
            } else emptyList<String>()
        }
        if (currentObj.contains("expression") && currentObj.getObject("expression")
                .contains("escapedText")
        ) return newList

        return buildAccessChain(currentObj.getObject("expression"), newList)
    }
}
