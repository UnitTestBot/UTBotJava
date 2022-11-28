package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getKind

abstract class BinaryOperatorNode: AstNode() {

    override val children: List<AstNode> = emptyList()

    abstract val stringOperatorName: String
}

fun V8Object.getBinaryOperatorNode(parent: AstNode?): BinaryOperatorNode {
    val kind = this.getKind()
    if (!ComparisonBinaryOperatorNode.allComparisonOperators.contains(kind)) {
        if (MathBinaryOperatorNode.allMathOperators.contains(kind)) {
            return MathBinaryOperatorNode(this, parent)
        } else {
            throw UnsupportedOperationException("Binary operator $kind is unknown")
        }
    }
    return ComparisonBinaryOperatorNode(this, parent)
}
