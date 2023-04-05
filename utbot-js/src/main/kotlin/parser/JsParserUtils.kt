package parser

import com.google.javascript.jscomp.Compiler
import com.google.javascript.jscomp.NodeUtil
import com.google.javascript.jscomp.SourceFile
import com.google.javascript.rhino.Node
import fuzzer.JsFuzzedContext
import parser.JsParserUtils.getMethodName
import parser.visitors.JsClassAstVisitor

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
        this.isCall -> {
            if (this.firstChild?.isGetProp == true) {
                this.firstChild?.next?.getAnyValue()
            } else null
        }

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

    /**
     * Checks if node is "require" JavaScript import.
     */
    fun Node.isRequireImport(): Boolean = try {
        this.isCall && this.firstChild?.string == "require"
    } catch (e: ClassCastException) {
        false
    }

    /**
     * Called upon "require" JavaScript import.
     *
     * Returns path to imported file as [String].
     */
    fun Node.getRequireImportText(): String = this.firstChild!!.next!!.string

    /**
     * Called upon "import" JavaScript import.
     *
     * Returns path to imported file as [String].
     */
    fun Node.getModuleImportText(): String = this.firstChild!!.next!!.next!!.string

    /**
     * Called upon "import" JavaScript import.
     *
     * Returns imported objects as [List].
     */
    fun Node.getModuleImportSpecsAsList(): List<Node> {
        val importSpecsNode = NodeUtil.findPreorder(this, { it.isImportSpecs }, { true })
            ?: throw UnsupportedOperationException("Module import doesn't contain \"import_specs\" token as an AST child")
        var currNode: Node? = importSpecsNode.firstChild!!
        val importSpecsList = mutableListOf<Node>()
        do {
            importSpecsList += currNode!!
            currNode = currNode?.next
        } while (currNode?.isImportSpec == true)
        return importSpecsList
    }

    /**
     * Called upon IMPORT_SPEC Node.
     *
     * Returns name of imported object as [String].
     */
    fun Node.getImportSpecName(): String = this.firstChild!!.string

    /**
     * Called upon IMPORT_SPEC Node.
     *
     * Returns import alias as [String].
     */
    fun Node.getImportSpecAliases(): String = this.firstChild!!.next!!.string

    /**
     * Checks if node is any kind of variable declaration.
     */
    fun Node.isAnyVariableDecl(): Boolean =
        this.isVar || this.isConst || this.isLet

    /**
     * Called upon any variable declaration node.
     *
     * Returns variable name as [String].
     */
    fun Node.getVariableName(): String? = try {
        this.firstChild!!.string
    } catch (_: Exception) {
        null
    }

    /**
     * Called upon any variable declaration node.
     *
     * Returns variable initializer as [Node]
     */
    fun Node.getVariableValue(): Node = this.firstChild!!.firstChild!!
}
