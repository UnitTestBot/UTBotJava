package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getKind

class ComparisonBinaryOperatorNode(
    obj: V8Object,
    typescript: V8Object
): BinaryOperatorNode() {

    companion object {
        val allComparisonOperators = setOf(
            "LessThanToken",
            "LessThanSlashToken",
            "GreaterThanToken",
            "LessThanEqualsToken",
            "GreaterThanEqualsToken",
            "EqualsEqualsToken",
            "ExclamationEqualsToken",
            "EqualsEqualsEqualsToken",
            "ExclamationEqualsEqualsToken",
            "EqualsGreaterThanToken",
            "QuestionQuestionToken",
        )
    }

    override val stringOperatorName = obj.getKind(typescript)
}