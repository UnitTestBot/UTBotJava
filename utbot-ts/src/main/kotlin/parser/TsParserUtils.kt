package parser

import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import parser.ast.AstNode
import parser.ast.BinaryExpressionNode
import parser.ast.ClassDeclarationNode
import parser.ast.ConstructorNode
import parser.ast.DummyNode
import parser.ast.FunctionDeclarationNode
import parser.ast.IdentifierNode
import parser.ast.ImportDeclarationNode
import parser.ast.MethodDeclarationNode
import parser.ast.NumericLiteralNode
import parser.ast.ParameterNode
import parser.ast.PropertyDeclarationNode
import parser.visitors.TsClassAstVisitor
import parser.visitors.TsImportsAstVisitor

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
        basePath: String,
        strict: Boolean = false,
    ): ClassDeclarationNode? {
        val visitor = TsClassAstVisitor(className)
        visitor.accept(parsedFile)
        return try {
            visitor.targetClassNode
        } catch (e: Exception) {
            if (!strict && visitor.classNodesCount == 1) {
                visitor.atLeastSomeClassNode
            } else {
                val importsVisitor = TsImportsAstVisitor(basePath, parser)
                importsVisitor.accept(parsedFile)
                className?.let { target ->
                    val parsedFile = importsVisitor.parsedFiles[target] ?: return@let null
                    searchForClassDecl(
                        className = target,
                        parsedFile = parsedFile,
                        basePath = basePath,
                        strict = true,
                    )
                }
            }
        }
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
        return (0 until array.length()).map {
            array[it] as V8Object
        }
    }

    fun V8Object.getAstNodeByKind(): AstNode {
        val node =  when (this.getKind()) {
            "FunctionDeclaration" -> FunctionDeclarationNode(this)
            "Parameter" -> ParameterNode(this)
            "Identifier" -> IdentifierNode(this)
            "BinaryExpression" -> BinaryExpressionNode(this)
            "ClassDeclaration" -> ClassDeclarationNode(this)
            "MethodDeclaration" -> MethodDeclarationNode(this)
            "PropertyDeclaration" -> PropertyDeclarationNode(this)
            "Constructor" -> ConstructorNode(this)
            "NumericLiteral" -> NumericLiteralNode(this)
            "ImportDeclaration" -> ImportDeclarationNode(this)
            "VariableDeclaration" -> {
                // TypeScript parses require as function call, so we check
                // if some call expression contains require keyword.
                if (syntaxKind.getString(
                        this.getObject("initializer")
                            .getObject("expression")
                            .getInteger("originalKeywordKind")
                            .toString()
                    ) == "RequireKeyword"
                ) {
                    ImportDeclarationNode(this)
                }
                else DummyNode(this)
            }
            else -> DummyNode(this)
       }
        return node
    }
}