package parser

import com.google.javascript.jscomp.Compiler
import com.google.javascript.jscomp.SourceFile
import com.google.javascript.rhino.Node
import fuzzer.JsFuzzedContext
import parser.JsParserUtils.getMethodName

// TODO: make methods more safe by checking the Node method is called on.
// Used for .children() calls.
@Suppress("DEPRECATION")
object JsParserUtils {

    fun runParser(fileText: String): Node =
        Compiler().parse(SourceFile.fromCode("jsFile", fileText))

    // TODO SEVERE: function only works in the same file scope. Add search in exports.
    fun searchForClassDecl(className: String?, parsedFile: Node, strict: Boolean = false): Node? {
        val visitor = JsClassAstVisitor(className)
        visitor.accept(parsedFile)
        return try {
            visitor.targetClassNode
        } catch (e: Exception) {
            if (!strict && visitor.classNodesCount == 1) {
                visitor.atLeastSomeClassNode
            } else null
        }
    }

    /**
     * Called upon node with Class token.
     */
    fun Node.getClassName(): String =
        this.firstChild?.string ?: throw IllegalStateException("Class AST node has no children")

    /**
     * Called upon node with Method token.
     */
    private fun Node.getMethodName(): String = this.string

    /**
     * Called upon node with Function token.
     */
    private fun Node.getFunctionName(): String =
        this.firstChild?.string ?: throw IllegalStateException("Function AST node has no children")

    /**
     * Called upon node with Parameter token.
     */
    fun Node.getParamName(): String = this.string

    /**
     * Convenience method. Used as a wrapper for [getFunctionName] and [getMethodName]
     * functions when the type of function is unknown.
     */
    fun Node.getAbstractFunctionName(): String = when {
        this.isMemberFunctionDef -> this.getMethodName()
        this.isFunction -> this.getFunctionName()
        else -> throw IllegalStateException()
    }

    /**
     * Called upon node with any kind of literal value token.
     */
    fun Node.getAnyValue(): Any? = when {
        this.isNumber -> this.double
        this.isString -> this.string
        this.isTrue -> true
        this.isFalse -> false
        else -> null
    }

    // For some reason Closure Compiler doesn't contain a built-in method
    // to check for some tokens.
    /**
     * Called upon node with any kind of binary comparison token.
     */
    fun Node.toFuzzedContextComparisonOrNull(): JsFuzzedContext? = when {
        this.isEQ -> JsFuzzedContext.EQ
        this.isNE -> JsFuzzedContext.NE
        this.token.name == "LT" -> JsFuzzedContext.LT
        this.token.name == "GT" -> JsFuzzedContext.GT
        this.token.name == "LE" -> JsFuzzedContext.LE
        this.token.name == "GE" -> JsFuzzedContext.GE
        this.token.name == "SHEQ" -> JsFuzzedContext.EQ
        else -> null
    }

    /**
     * Called upon node with any kind of binary comparison token.
     */
    fun Node.getBinaryExprLeftOperand(): Node = this.getChildAtIndex(0)

    /**
     * Called upon node with any kind of binary comparison token.
     */
    fun Node.getBinaryExprRightOperand(): Node = this.getChildAtIndex(1)

    /**
     * Called upon node with Function token.
     */
    private fun Node.getFunctionParams(): List<Node> = this.getChildAtIndex(1).children().map { it }

    /**
     * Called upon node with Method token.
     */
    private fun Node.getMethodParams(): List<Node> =
        this.firstChild?.getFunctionParams() ?: throw IllegalStateException("Method AST node has no children")

    /**
     * Convenience method. Used as a wrapper for [getFunctionParams] and [getMethodParams]
     * function when the type of function is unknown.
     */
    fun Node.getAbstractFunctionParams(): List<Node> = when {
        this.isMemberFunctionDef -> getMethodParams()
        this.isFunction -> getFunctionParams()
        else -> throw IllegalStateException()
    }

    /**
     * Called upon node with Class token.
     */
    fun Node.getClassMethods(): List<Node> {
        val classMembers = this.children().find { it.isClassMembers }
            ?: throw IllegalStateException("Can't extract class members of class ${this.getClassName()}")
        return classMembers.children().filter { it.isMemberFunctionDef }

    }

    /**
     * Called upon node with Class token.
     *
     * Returns null if class has no constructor.
     */
    fun Node.getConstructor(): Node? {
        val classMembers = this.children().find { it.isClassMembers }
            ?: throw IllegalStateException("Can't extract methods of class ${this.getClassName()}")
        return classMembers.children().find {
            it.isMemberFunctionDef && it.getMethodName() == "constructor"
        }?.firstChild
    }

    /**
     * Called upon node with Method token.
     */
    fun Node.isStatic(): Boolean = this.isStaticMember
}
