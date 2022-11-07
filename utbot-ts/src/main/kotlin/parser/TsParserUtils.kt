package parser

import parser.ast.BinaryExpressionNode
import parser.ast.ClassDeclarationNode
import parser.ast.ConstructorNode
import parser.ast.DummyNode
import parser.ast.FunctionDeclarationNode
import parser.ast.IdentifierNode
import parser.ast.MethodDeclarationNode
import parser.ast.ParameterNode
import parser.ast.PropertyDeclarationNode
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import parser.ast.AstNode

object TsParserUtils {

    fun V8Object.getKind(typescript: V8Object): String {
        return try {
            val syntaxKind = typescript.getObject("SyntaxKind")
            val kind = this.getInteger("kind").toString()
            remapIfNecessary(syntaxKind.getString(kind))
        } catch (e: Exception) {
            "Undefined"
        }
    }

    /*
     * TypeScript module has "SyntaxKind" object, that maps string name of node kind to a number.
     * However, the numbers are not unique, and at the end of this object some names occupy
     * Already existing numbers. Those names seem like utility ones, but when we access "SyntaxKind" through
     * J2V8, if the passed number is used more than once, we get the last entry of this number. That's why
     * Remap is necessary not to get "FirstAssignment" from "=" token.
     */
    private fun remapIfNecessary(rawKind: String): String {
        val map = mapOf(
            "FirstToken" to "Unknown",
            "FirstAssignment" to "EqualsToken",
            "LastAssignment" to "CaretEqualsToken",
            "FirstCompoundAssignment" to "PlusEqualsToken",
            "LastCompoundAssignment" to "CaretEqualsToken",
            "FirstReservedWord" to "BreakKeyword",
            "LastReservedWord" to "WithKeyword",
            "FirstKeyword" to "BreakKeyword",
            "LastKeyword" to "OfKeyword",
            "FirstFutureReservedWord" to "ImplementsKeyword",
            "LastFutureReservedWord" to "YieldKeyword",
            "FirstTypeNode" to "TypePredicate",
            "LastTypeNode" to "ImportType",
            "FirstPunctuation" to "OpenBraceToken",
            "LastPunctuation" to "CaretEqualsToken",
            "LastToken" to "OfKeyword",
            "FirstTriviaToken" to "SingleLineCommentTrivia",
            "LastTriviaToken" to "ConflictMarkerTrivia",
            "FirstLiteralToken" to "NumericLiteral",
            "LastLiteralToken" to "NoSubstitutionTemplateLiteral",
            "FirstTemplateToken" to "NoSubstitutionTemplateLiteral",
            "LastTemplateToken" to "TemplateTail",
            "FirstBinaryOperator" to "LessThanToken",
            "LastBinaryOperator" to "CaretEqualsToken",
            "FirstStatement" to "VariableStatement",
            "LastStatement" to "DebuggerStatement",
            "FirstNode" to "QualifiedName",
            "FirstJSDocNode" to "JSDocTypeExpression",
            "LastJSDocNode" to "JSDocPropertyTag",
            "FirstJSDocTagNode" to "JSDocTag",
            "LastJSDocTagNode" to "JSDocPropertyTag",
            )
        return map.getOrDefault(rawKind, rawKind)
    }

    fun V8Object.getChildren(): List<V8Object> {
        val array = this.executeJSFunction("getChildren") as V8Array
        return (0 until array.length()).fold(initial = mutableListOf()) { acc, i ->
            when {
                array[i] is V8Object -> acc.apply { this.add(array[i] as V8Object) }
                array[i] is V8Array -> acc.apply {
                    for (j in array.keys) {
                        acc.add(array[j] as V8Object)
                    }
                }
                else -> acc
            }
        }
    }

    fun V8Object.getArrayAsList(arrayName: String): List<V8Object> {
        val array = this.getArray(arrayName)
        return (0 until array.length()).map {
            array[it] as V8Object
        }
    }

    fun V8Object.getAstNodeByKind(typescript: V8Object): AstNode = when (this.getKind(typescript)) {
        "FunctionDeclaration" -> FunctionDeclarationNode(this, typescript)
        "Parameter" -> ParameterNode(this, typescript)
        "Identifier" -> IdentifierNode(this)
        "BinaryExpression" -> BinaryExpressionNode(this, typescript)
        "ClassDeclaration" -> ClassDeclarationNode(this, typescript)
        "MethodDeclaration" -> MethodDeclarationNode(this, typescript)
        "PropertyDeclaration" -> PropertyDeclarationNode(this, typescript)
        "Constructor" -> ConstructorNode(this, typescript)
        else -> DummyNode(this, typescript)
    }
}