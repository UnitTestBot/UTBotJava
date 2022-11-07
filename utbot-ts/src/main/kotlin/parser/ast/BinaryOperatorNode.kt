package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getAstNodeByKind
import parser.TsParserUtils.getKind

abstract class BinaryOperatorNode(
    obj: V8Object,
    typescript: V8Object
): AstNode() {

    override val children: List<AstNode> = emptyList()

    val left = (obj.get("left") as V8Object).getAstNodeByKind(typescript)

    val right = (obj.get("right") as V8Object).getAstNodeByKind(typescript)

    abstract val stringOperatorName: String
}

fun V8Object.getBinaryOperatorNode(typescript: V8Object): BinaryOperatorNode {
    val kind = this.getKind(typescript)
    if (!ComparisonBinaryOperatorNode.allComparisonOperators.contains(kind)) {
        if (MathBinaryOperatorNode.allMathOperators.contains(kind)) {
            return MathBinaryOperatorNode(this, typescript)
        } else {
            throw UnsupportedOperationException("Binary operator $kind is unknown")
        }
    }
    return ComparisonBinaryOperatorNode(this, typescript)
}
