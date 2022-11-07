package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getKind

class MathBinaryOperatorNode(
    obj: V8Object,
    typescript: V8Object
): BinaryOperatorNode(obj, typescript) {

    companion object {
        val allMathOperators = setOf(
            "PlusToken",
            "MinusToken",
            "AsteriskToken",
            "AsteriskAsteriskToken",
            "SlashToken",
            "PercentToken",
            "AmpersandToken",
            "BarToken",
            "AmpersandAmpersandToken",
            "BarBarToken",
            // Not exactly math, here only not to be among comparison ones.
            "EqualsToken",
            "FirstAssignment"
        )
    }

    override val stringOperatorName = obj.getKind(typescript)
}