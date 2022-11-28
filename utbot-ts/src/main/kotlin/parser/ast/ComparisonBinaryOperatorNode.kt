package parser.ast

import com.eclipsesource.v8.V8Object
import org.utbot.fuzzer.FuzzedContext
import parser.TsParserUtils.getKind

class ComparisonBinaryOperatorNode(
    obj: V8Object,
    override val parent: AstNode?
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

    override val stringOperatorName = obj.getKind()

    fun toFuzzedContext(): FuzzedContext =
        when (stringOperatorName) {
            "LessThanToken" -> FuzzedContext.Comparison.LT
            "LessThanEqualsToken" -> FuzzedContext.Comparison.LE
            "GreaterThanToken" -> FuzzedContext.Comparison.GT
            "GreaterThanEqualsToken" -> FuzzedContext.Comparison.GE
            "EqualsEqualsToken" -> FuzzedContext.Comparison.EQ
            "ExclamationEqualsEqualsToken" -> FuzzedContext.Comparison.NE
            else -> FuzzedContext.Unknown
        }
}