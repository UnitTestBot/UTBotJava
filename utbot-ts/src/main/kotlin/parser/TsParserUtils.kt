package parser

import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import parser.ast.ArrowFunctionNode
import parser.ast.AstNode
import parser.ast.BinaryExpressionNode
import parser.ast.CallExpressionNode
import parser.ast.ClassDeclarationNode
import parser.ast.ConstructorNode
import parser.ast.DummyNode
import parser.ast.FunctionDeclarationNode
import parser.ast.IdentifierNode
import parser.ast.ImportDeclarationNode
import parser.ast.MethodDeclarationNode
import parser.ast.NumericLiteralNode
import parser.ast.ParameterNode
import parser.ast.PropertyAccessExpressionNode
import parser.ast.PropertyDeclarationNode
import parser.ast.VariableDeclarationNode
import parser.ast.VariableStatementNode
import parser.visitors.TsClassAstVisitor

object TsParserUtils {


    private lateinit var syntaxKind: V8Object
    private lateinit var parser: TsParser

    /*
        I dislike this function, but it removes typescript V8Object from the parser API, and we don't
        have to access SyntaxKind every time we need node kind.
     */
    fun initParserUtils(ts: V8Object, tsParser: TsParser) {
        syntaxKind = ts.getObject("SyntaxKind")
        parser = tsParser
    }

    @Suppress("NAME_SHADOWING")
    fun searchForClassDecl(
        className: String?,
        parsedFile: AstNode,
        strict: Boolean = false,
        parsedImportedFiles: Map<String, AstNode>? = null,
    ): ClassDeclarationNode? {
        val visitor = TsClassAstVisitor(className)
        visitor.accept(parsedFile)
        return try {
            visitor.targetClassNode
        } catch (e: Exception) {
            if (!strict && visitor.classNodesCount == 1) {
                visitor.atLeastSomeClassNode
            } else {
                className?.let { target ->
                    val parsedFile = parsedImportedFiles?.get(target) ?: return@let null
                    searchForClassDecl(
                        className = target,
                        parsedFile = parsedFile,
                        strict = true,
                        parsedImportedFiles = parsedImportedFiles,
                    )
                }
            }
        }
    }

    fun <T: AstNode>AstNode.findParentOfType(clazz: Class<T>): T? {
        var currentNode = this.parent
        while (currentNode != null && !clazz.isInstance(currentNode)) {
            currentNode = currentNode.parent
        }
        @Suppress("UNCHECKED_CAST")
        return currentNode as T?
    }

    fun V8Object.getKind(): String {
        return try {
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
     * Remap is necessary not to get, for example, "FirstAssignment" from "=" token.
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
        return try {
            (0 until array.length()).map {
                array[it] as V8Object
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun V8Object.getAstNodeByKind(parent: AstNode?): AstNode {
        val node = when (this.getKind()) {
            "ArrowFunction" -> ArrowFunctionNode(this, parent)
            "FunctionDeclaration" -> FunctionDeclarationNode(this, parent)
            "Parameter" -> ParameterNode(this, parent)
            "Identifier" -> IdentifierNode(this, parent)
            "BinaryExpression" -> BinaryExpressionNode(this, parent)
            "ClassDeclaration" -> ClassDeclarationNode(this, parent)
            "MethodDeclaration" -> MethodDeclarationNode(this, parent)
            "PropertyDeclaration" -> PropertyDeclarationNode(this, parent)
            "Constructor" -> ConstructorNode(this, parent)
            "NumericLiteral" -> NumericLiteralNode(this, parent)
            "ImportDeclaration" -> ImportDeclarationNode(this, parent)
            "PropertyAccessExpression" -> PropertyAccessExpressionNode(this, parent)
            // Currently not supporting method access calls
            "CallExpression" -> try {
                CallExpressionNode(this, parent)
            } catch (e: Exception) { DummyNode(this, parent) }
            "VariableDeclaration" -> this.checkForRequire(parent)
            "VariableStatement" -> VariableStatementNode(this, parent)
            else -> DummyNode(this, parent)
       }
        return node
    }

    private fun V8Object.checkForRequire(parent: AstNode?): AstNode = try {
        if (syntaxKind.getString(
                this.getObject("initializer")
                    .getObject("expression")
                    .getInteger("originalKeywordKind")
                    .toString()
            ) == "RequireKeyword"
        ) {
            ImportDeclarationNode(this, parent)
        } else DummyNode(this, parent)
    } catch (e: Exception) { VariableDeclarationNode(this, parent) }
}