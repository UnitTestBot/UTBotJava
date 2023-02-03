package org.utbot.framework.codegen.renderer

import org.apache.commons.text.StringEscapeUtils
import org.utbot.common.FileUtil
import org.utbot.common.WorkaroundReason.LONG_CODE_FRAGMENTS
import org.utbot.common.workaround
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.domain.Import
import org.utbot.framework.codegen.domain.RegularImport
import org.utbot.framework.codegen.domain.StaticImport
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgClassFile
import org.utbot.framework.codegen.domain.models.CgAbstractFieldAccess
import org.utbot.framework.codegen.domain.models.CgAbstractMultilineComment
import org.utbot.framework.codegen.domain.models.CgArrayElementAccess
import org.utbot.framework.codegen.domain.models.CgAssignment
import org.utbot.framework.codegen.domain.models.CgAuxiliaryClass
import org.utbot.framework.codegen.domain.models.CgBreakStatement
import org.utbot.framework.codegen.domain.models.CgClass
import org.utbot.framework.codegen.domain.models.CgComment
import org.utbot.framework.codegen.domain.models.CgCommentedAnnotation
import org.utbot.framework.codegen.domain.models.CgComparison
import org.utbot.framework.codegen.domain.models.CgContinueStatement
import org.utbot.framework.codegen.domain.models.CgCustomTagStatement
import org.utbot.framework.codegen.domain.models.CgDeclaration
import org.utbot.framework.codegen.domain.models.CgDecrement
import org.utbot.framework.codegen.domain.models.CgDoWhileLoop
import org.utbot.framework.codegen.domain.models.CgDocClassLinkStmt
import org.utbot.framework.codegen.domain.models.CgDocCodeStmt
import org.utbot.framework.codegen.domain.models.CgDocMethodLinkStmt
import org.utbot.framework.codegen.domain.models.CgDocPreTagStatement
import org.utbot.framework.codegen.domain.models.CgDocRegularLineStmt
import org.utbot.framework.codegen.domain.models.CgDocRegularStmt
import org.utbot.framework.codegen.domain.models.CgDocumentationComment
import org.utbot.framework.codegen.domain.models.CgElement
import org.utbot.framework.codegen.domain.models.CgEmptyLine
import org.utbot.framework.codegen.domain.models.CgEnumConstantAccess
import org.utbot.framework.codegen.domain.models.CgErrorTestMethod
import org.utbot.framework.codegen.domain.models.CgErrorWrapper
import org.utbot.framework.codegen.domain.models.CgExecutableCall
import org.utbot.framework.codegen.domain.models.CgMethodsCluster
import org.utbot.framework.codegen.domain.models.CgExpression
import org.utbot.framework.codegen.domain.models.CgFieldAccess
import org.utbot.framework.codegen.domain.models.CgForEachLoop
import org.utbot.framework.codegen.domain.models.CgForLoop
import org.utbot.framework.codegen.domain.models.CgGreaterThan
import org.utbot.framework.codegen.domain.models.CgIfStatement
import org.utbot.framework.codegen.domain.models.CgIncrement
import org.utbot.framework.codegen.domain.models.CgInnerBlock
import org.utbot.framework.codegen.domain.models.CgIsInstance
import org.utbot.framework.codegen.domain.models.CgLessThan
import org.utbot.framework.codegen.domain.models.CgLiteral
import org.utbot.framework.codegen.domain.models.CgLogicalAnd
import org.utbot.framework.codegen.domain.models.CgLogicalOr
import org.utbot.framework.codegen.domain.models.CgLoop
import org.utbot.framework.codegen.domain.models.CgMethod
import org.utbot.framework.codegen.domain.models.CgMethodCall
import org.utbot.framework.codegen.domain.models.CgMultilineComment
import org.utbot.framework.codegen.domain.models.CgMultipleArgsAnnotation
import org.utbot.framework.codegen.domain.models.CgNamedAnnotationArgument
import org.utbot.framework.codegen.domain.models.CgNestedClassesRegion
import org.utbot.framework.codegen.domain.models.CgNonStaticRunnable
import org.utbot.framework.codegen.domain.models.CgParameterDeclaration
import org.utbot.framework.codegen.domain.models.CgParameterizedTestDataProviderMethod
import org.utbot.framework.codegen.domain.models.CgRegion
import org.utbot.framework.codegen.domain.models.CgReturnStatement
import org.utbot.framework.codegen.domain.models.CgSimpleRegion
import org.utbot.framework.codegen.domain.models.CgSingleArgAnnotation
import org.utbot.framework.codegen.domain.models.CgSingleLineComment
import org.utbot.framework.codegen.domain.models.CgSpread
import org.utbot.framework.codegen.domain.models.CgStatement
import org.utbot.framework.codegen.domain.models.CgStatementExecutableCall
import org.utbot.framework.codegen.domain.models.CgStaticFieldAccess
import org.utbot.framework.codegen.domain.models.CgStaticRunnable
import org.utbot.framework.codegen.domain.models.CgStaticsRegion
import org.utbot.framework.codegen.domain.models.CgTestMethod
import org.utbot.framework.codegen.domain.models.CgTestMethodCluster
import org.utbot.framework.codegen.domain.models.CgThisInstance
import org.utbot.framework.codegen.domain.models.CgThrowStatement
import org.utbot.framework.codegen.domain.models.CgTripleSlashMultilineComment
import org.utbot.framework.codegen.domain.models.CgTryCatch
import org.utbot.framework.codegen.domain.models.CgUtilMethod
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.domain.models.CgWhileLoop
import org.utbot.framework.codegen.tree.ututils.UtilClassKind
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.TypeParameters
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.byteClassId
import org.utbot.framework.plugin.api.util.charClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.floatClassId
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.framework.plugin.api.util.isRefType
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.framework.plugin.api.util.shortClassId

abstract class CgAbstractRenderer(
    val context: CgRendererContext,
    val printer: CgPrinter = CgPrinterImpl()
) : CgVisitor<Unit>,
    CgPrinter by printer {

    protected abstract val statementEnding: String

    protected abstract val logicalAnd: String
    protected abstract val logicalOr: String

    protected open val regionStart: String = "///region"
    protected open val regionEnd: String = "///endregion"
    protected var isInterrupted = false

    protected abstract val langPackage: String

    // We may render array elements in initializer in one line or in separate lines:
    // items count in one line depends on the element type.
    protected fun arrayElementsInLine(elementType: ClassId): Int {
        if (elementType.isRefType) return 10
        if (elementType.isArray) return 1
        return when (elementType) {
            intClassId, byteClassId, longClassId, charClassId -> 8
            booleanClassId, shortClassId, doubleClassId, floatClassId -> 6
            else -> error("Non primitive value of type $elementType is unexpected in array initializer")
        }
    }

    /**
     * Returns true if one can call methods of this class without specifying a caller (for example if ClassId represents this instance)
     */
    protected abstract val ClassId.methodsAreAccessibleAsTopLevel: Boolean

    private val MethodId.accessibleByName: Boolean
        get() = (context.shouldOptimizeImports && this in context.importedStaticMethods) || classId.methodsAreAccessibleAsTopLevel

    override fun visit(element: CgElement) {
        val error =
            "CgRenderer has reached the top of Cg elements hierarchy and did not find a method for ${element::class}"
        throw IllegalArgumentException(error)
    }

    override fun visit(element: CgClassFile) {
        renderClassPackage(element.declaredClass)
        renderClassFileImports(element)
        element.declaredClass.accept(this)
    }

    /**
     * Render the region only if it is not empty.
     */
    override fun visit(element: CgStaticsRegion) {
        element.render()
    }

    /**
     * Render the region only if it is not empty.
     */
    override fun visit(element: CgNestedClassesRegion<*>) {
        element.render()
    }

    /**
     * Render the region only if it is not empty.
     */
    override fun visit(element: CgSimpleRegion<*>) {
        element.render()
    }

    /**
     * Render the cluster only if it is not empty.
     */
    override fun visit(element: CgTestMethodCluster) {
        element.render()
    }

    /**
     * Render the cluster only if it is not empty.
     */
    override fun visit(element: CgMethodsCluster) {
        // We print the next line after all contained regions to prevent gluing of region ends
        element.render(printLineAfterContentEnd = true)
    }

    /**
     * Renders the region with a specific rendering for [CgTestMethodCluster.description]
     */
    private fun CgRegion<*>.render(printLineAfterContentEnd: Boolean = false) {
        if (content.isEmpty() || isInterrupted) return

        print(regionStart)
        header?.let { print(" $it") }
        println()

        if (this is CgTestMethodCluster) description?.accept(this@CgAbstractRenderer)

        var isLimitExceeded = false
        for (method in content) {
            if (printer.printedLength > UtSettings.maxTestFileSize) {
                isLimitExceeded = true
                break
            }
            println()
            method.accept(this@CgAbstractRenderer)
        }

        if (printLineAfterContentEnd) println()

        println(regionEnd)

        if (isLimitExceeded && !isInterrupted) {
            visit(CgSingleLineComment("Abrupt generation termination: file size exceeds configured limit (${FileUtil.byteCountToDisplaySize(UtSettings.maxTestFileSize.toLong())})"))
            visit(CgSingleLineComment("The limit can be configured in '{HOME_DIR}/.utbot/settings.properties' with 'maxTestsFileSize' property"))
            isInterrupted = true
        }
    }

    override fun visit(element: CgAuxiliaryClass) {
        val auxiliaryClassText = element.getText(context)
        auxiliaryClassText.split("\n")
            .forEach { line -> println(line) }
    }

    override fun visit(element: CgUtilMethod) {
        val utilMethodText = element.getText(context)
        utilMethodText.split("\n")
            .forEach { line -> println(line) }
    }

    // Methods

    override fun visit(element: CgMethod) {
        // TODO introduce CgBlock
        print(" ")
        visit(element.statements, printNextLine = true)
    }

    override fun visit(element: CgTestMethod) {
        renderMethodDocumentation(element)
        for (annotation in element.annotations) {
            annotation.accept(this)
        }
        renderMethodSignature(element)
        visit(element as CgMethod)
    }

    override fun visit(element: CgErrorTestMethod) {
        renderMethodDocumentation(element)
        renderMethodSignature(element)
        visit(element as CgMethod)
    }

    override fun visit(element: CgParameterizedTestDataProviderMethod) {
        for (annotation in element.annotations) {
            annotation.accept(this)
        }
        renderMethodSignature(element)
        visit(element as CgMethod)
    }

    // Annotations

    override fun visit(element: CgCommentedAnnotation) {
        print("//")
        element.annotation.accept(this)
    }

    override fun visit(element: CgSingleArgAnnotation) {
        print("@${element.classId.asString()}")
        print("(")
        element.argument.accept(this)
        println(")")
    }

    override fun visit(element: CgMultipleArgsAnnotation) {
        print("@${element.classId.asString()}")
        if (element.arguments.isNotEmpty()) {
            print("(")
            element.arguments.renderSeparated()
            print(")")
        }
        println()
    }

    override fun visit(element: CgNamedAnnotationArgument) {
        print(element.name)
        print(" = ")
        element.value.accept(this)
    }

    // Comments

    override fun visit(element: CgComment) {
        visit(element as CgElement)
    }

    override fun visit(element: CgSingleLineComment) {
        println("// ${element.comment}")
    }

    override fun visit(element: CgAbstractMultilineComment) {
        visit(element as CgElement)
    }

    override fun visit(element: CgTripleSlashMultilineComment) {
        for (line in element.lines) {
            println("/// $line")
        }
    }

    override fun visit(element: CgMultilineComment) {
        val lines = element.lines
        if (lines.isEmpty()) return

        if (lines.size == 1) {
            println("/* ${lines.first()} */")
            return
        }

        // print lines saving indentation
        print("/* ")
        println(lines.first())
        lines.subList(1, lines.lastIndex).forEach { println(it) }
        print(lines.last())
        println(" */")
    }

    override fun visit(element: CgDocumentationComment) {
        if (element.lines.all { it.isEmpty() }) return

        println("/**")
        element.lines.forEach { it.accept(this) }
        println(" */")
    }
    override fun visit(element: CgDocPreTagStatement) {
        if (element.content.all { it.isEmpty() }) return
        println("<pre>")
        for (stmt in element.content) stmt.accept(this)
        println("</pre>")
    }

    override fun visit(element: CgCustomTagStatement) {
        if (element.statements.all { it.isEmpty() }) return

        element.statements.forEach { it.accept(this) }
    }

    override fun visit(element: CgDocCodeStmt) {
        if (element.isEmpty()) return

        val text = element.stmt
            .replace("\n", "\n * ")
            //remove multiline comment symbols to avoid comment in comment effect
            .replace("/*", "")
            .replace("*/", "")
        print("{@code $text }")
    }
    override fun visit(element: CgDocRegularStmt){
        if (element.isEmpty()) return

        print(element.stmt.replace("\n", "\n * "))
    }
    override fun visit(element: CgDocRegularLineStmt){
        if (element.isEmpty()) return

        // It is better to avoid using \n in print, using println is preferred.
        // Mixing println's and print's with '\n' BREAKS indention.
        // See [https://stackoverflow.com/questions/6685665/system-out-println-vs-n-in-java].
        println(" * " + element.stmt)
    }
    override fun visit(element: CgDocClassLinkStmt) {
        if (element.isEmpty()) return

        print(element.className)
    }
    override fun visit(element: CgDocMethodLinkStmt){
        if (element.isEmpty()) return

        print("${element.className}::${element.methodName}") //todo make it as link {@link org.utbot.examples.online.Loops#whileLoop(int) }
    }

    /**
     * Renders any block of code with curly braces.
     *
     * NOTE: [printNextLine] has default false value in [CgVisitor]
     *
     * NOTE: due to JVM restrictions for methods size
     * [in 65536 bytes](https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.3)
     * we comment long blocks
     */
    override fun visit(block: List<CgStatement>, printNextLine: Boolean) {
        println("{")

        val isBlockTooLarge = workaround(LONG_CODE_FRAGMENTS) { block.size > LARGE_CODE_BLOCK_SIZE }

        if (isBlockTooLarge) {
            print("/*")
            println(" This block of code is ${block.size} lines long and could lead to compilation error")
        }

        withIndent {
            for (statement in block) {
                statement.accept(this)
            }
        }

        if (isBlockTooLarge) println("*/")

        print("}")

        if (printNextLine) println()
    }

    // Return statement

    override fun visit(element: CgReturnStatement) {
        print("return ")
        element.expression.accept(this)
        println(statementEnding)
    }

    // Array element access

    override fun visit(element: CgArrayElementAccess) {
        element.array.accept(this)
        print("[")
        element.index.accept(this)
        print("]")
    }

    // Spread operator

    override fun visit(element: CgSpread) {
        element.array.accept(this)
    }

    // Loop conditions

    override fun visit(element: CgComparison) {
        return visit(element as CgElement)
    }

    override fun visit(element: CgLessThan) {
        element.left.accept(this)
        print(" < ")
        element.right.accept(this)
    }

    override fun visit(element: CgGreaterThan) {
        element.left.accept(this)
        print(" > ")
        element.right.accept(this)
    }

    // Increment and decrement

    override fun visit(element: CgIncrement) {
        print("${element.variable.name}++")
    }

    override fun visit(element: CgDecrement) {
        print("${element.variable.name}--")
    }

    // isInstance check

    override fun visit(element: CgIsInstance) {
        element.classExpression.accept(this)
        print(".isInstance(")
        element.value.accept(this)
        print(")")
    }

    // Try-catch

    override fun visit(element: CgTryCatch) {
        print("try ")
        // TODO: SAT-1329 actually Kotlin does not support try-with-resources so we have to use "use" method here
        element.resources?.let {
            println("(")
            withIndent {
                for (resource in element.resources) {
                    resource.accept(this)
                }
            }
            print(") ")
        }
        // TODO introduce CgBlock
        visit(element.statements)
        for ((exception, statements) in element.handlers) {
            print(" catch (")
            renderExceptionCatchVariable(exception)
            print(") ")
            // TODO introduce CgBlock
            visit(statements, printNextLine = element.finally == null)
        }
        element.finally?.let {
            print(" finally ")
            // TODO introduce CgBlock
            visit(element.finally, printNextLine = true)
        }
    }

    abstract override fun visit(element: CgErrorWrapper)

    //Simple block

    abstract override fun visit(element: CgInnerBlock)

    // Loops

    override fun visit(element: CgLoop) {
        return visit(element as CgElement)
    }

    override fun visit(element: CgForLoop) {
        renderForLoopVarControl(element)
        print(") ")
        // TODO introduce CgBlock
        visit(element.statements)
        println()
    }

    override fun visit(element: CgForEachLoop) {}

    override fun visit(element: CgWhileLoop) {
        print("while (")
        element.condition.accept(this)
        print(") ")
        // TODO introduce CgBlock
        visit(element.statements)
        println()
    }

    override fun visit(element: CgDoWhileLoop) {
        print("do ")
        // TODO introduce CgBlock
        visit(element.statements)
        print(" while (")
        element.condition.accept(this)
        println(");")
    }

    // Control statements
    override fun visit(element: CgBreakStatement) {
        println("break$statementEnding")
    }

    override fun visit(element: CgContinueStatement) {
        println("continue$statementEnding")
    }

    // Variable declaration

    override fun visit(element: CgDeclaration) {
        renderDeclarationLeftPart(element)
        element.initializer?.let {
            print(" = ")
            it.accept(this)
        }
        println(statementEnding)
    }

    // Variable assignment

    override fun visit(element: CgAssignment) {
        element.lValue.accept(this)
        print(" = ")
        element.rValue.accept(this)
        println(statementEnding)
    }

    // Expressions

    override fun visit(element: CgExpression) {
        visit(element as CgElement)
    }

    // This instance

    override fun visit(element: CgThisInstance) {
        print("this")
    }

    // Variables

    override fun visit(element: CgVariable) {
        print(element.name.escapeNamePossibleKeyword())
    }

    // Method parameters

    abstract override fun visit(element: CgParameterDeclaration)

    // Primitive and String literals

    override fun visit(element: CgLiteral) {
        print(element.toStringConstant())
    }

    protected fun CgLiteral.toStringConstant(asRawString: Boolean = false) =
        with(this.value) {
            when (this) {
                is Byte -> toStringConstant()
                is Char -> toStringConstant()
                is Short -> toStringConstant()
                is Int -> toStringConstant()
                is Long -> toStringConstant()
                is Float -> toStringConstant()
                is Double -> toStringConstant()
                is Boolean -> toStringConstant()
                // String is "\"" + "str" + "\"", RawString is "str"
                is String -> if (asRawString) "$this".escapeCharacters() else toStringConstant()
                else -> "$this"
            }
        }

    // Non-static runnable like this::toString or (new Object())::toString etc
    override fun visit(element: CgNonStaticRunnable) {
        // TODO we need braces for expressions like (new Object())::toString but not for this::toString
        print("(")
        element.referenceExpression.accept(this)
        print(")::")
        print(element.methodId.name)
    }

    // Static runnable like Random::nextRandomInt etc
    override fun visit(element: CgStaticRunnable) {
        print(element.classId.asString())
        print("::")
        print(element.methodId.name)
    }

    // Enum constant

    override fun visit(element: CgEnumConstantAccess) {
        print(element.enumClass.asString())
        print(".")
        print(element.name)
    }

    // Property access

    override fun visit(element: CgAbstractFieldAccess) {
        visit(element as CgElement)
    }

    override fun visit(element: CgFieldAccess) {
        element.caller.accept(this)
        print(".")
        print(element.fieldId.name)
    }

    override fun visit(element: CgStaticFieldAccess) {
        if (!element.declaringClass.methodsAreAccessibleAsTopLevel) {
            print(element.declaringClass.asString())
            print(".")
        }
        print(element.fieldName)
    }

    // Conditional statement

    override fun visit(element: CgIfStatement) {
        print("if (")
        element.condition.accept(this)
        print(") ")
        // TODO introduce CgBlock
        visit(element.trueBranch)
        element.falseBranch?.let {
            print(" else ")
            // TODO introduce CgBlock
            visit(element.falseBranch)
        }
        println()
    }

    // Binary logical operators

    override fun visit(element: CgLogicalAnd) {
        element.left.accept(this)
        print(" $logicalAnd ")
        element.right.accept(this)
    }

    override fun visit(element: CgLogicalOr) {
        element.left.accept(this)
        print(" $logicalOr ")
        element.right.accept(this)
    }

    // Executable calls

    override fun visit(element: CgStatementExecutableCall) {
        element.call.accept(this)
        println(statementEnding)
    }

    override fun visit(element: CgExecutableCall) {
        visit(element as CgElement)
    }

    // TODO: consider the case of generic functions
    // TODO: write tests for all cases of method call rendering (with or without caller, etc.)
    override fun visit(element: CgMethodCall) {
        val caller = element.caller
        if (caller != null) {
            // 'this' can be omitted, otherwise render caller
            if (caller !is CgThisInstance) {
                // TODO: we need parentheses for calls like (-1).inv(), do something smarter here
                if (caller !is CgVariable) print("(")
                caller.accept(this)
                if (caller !is CgVariable) print(")")
                renderAccess(caller)
            }
        } else {
            // for static methods render declaring class only if required
            if (!element.executableId.accessibleByName) {
                val method = element.executableId
                print(method.classId.asString())
                print(".")
            }
        }
        print(element.executableId.name.escapeNamePossibleKeyword())

        renderTypeParameters(element.typeParameters)
        renderExecutableCallArguments(element)
    }

    // Throw statement

    override fun visit(element: CgThrowStatement) {
        print("throw ")
        element.exception.accept(this)
        println(statementEnding)
    }

    override fun visit(element: CgEmptyLine) {
        println()
    }

    override fun toString(): String = printer.toString()

    protected abstract fun renderRegularImport(regularImport: RegularImport)
    protected abstract fun renderStaticImport(staticImport: StaticImport)

    //we render parameters in method signature on one line or on separate lines depending their amount
    protected val maxParametersAmountInOneLine = 3

    protected abstract fun renderMethodSignature(element: CgTestMethod)
    protected abstract fun renderMethodSignature(element: CgErrorTestMethod)
    protected abstract fun renderMethodSignature(element: CgParameterizedTestDataProviderMethod)

    protected abstract fun renderForLoopVarControl(element: CgForLoop)

    protected abstract fun renderDeclarationLeftPart(element: CgDeclaration)

    protected abstract fun toStringConstantImpl(byte: Byte): String
    protected abstract fun toStringConstantImpl(short: Short): String
    protected abstract fun toStringConstantImpl(int: Int): String
    protected abstract fun toStringConstantImpl(long: Long): String
    protected abstract fun toStringConstantImpl(float: Float): String

    protected abstract fun renderAccess(caller: CgExpression)
    protected abstract fun renderTypeParameters(typeParameters: TypeParameters)
    protected abstract fun renderExecutableCallArguments(executableCall: CgExecutableCall)

    protected abstract fun renderExceptionCatchVariable(exception: CgVariable)

    protected fun getEscapedImportRendering(import: Import): String =
        import.qualifiedName
            .split(".")
            .joinToString(".") { it.escapeNamePossibleKeyword() }

    protected fun List<String>.printSeparated(newLines: Boolean = false) {
        for ((index, element) in this.withIndex()) {
            print(element)
            when {
                index < lastIndex -> {
                    print(",")
                    if (newLines) println() else print(" ")
                }
                index == lastIndex -> if (newLines) println()
            }
        }
    }

    protected fun List<CgElement>.renderSeparated(newLines: Boolean = false) {
        for ((index, element) in this.withIndex()) {
            element.accept(this@CgAbstractRenderer)
            when {
                index < lastIndex -> {
                    print(",")
                    if (newLines) println() else print(" ")
                }
                index == lastIndex -> if (newLines) println()
            }
        }
    }

    protected fun List<CgExpression>.renderElements(elementsInLine: Int) {
        val length = this.size
        if (length <= elementsInLine) { // one-line array
            for (i in 0 until length) {
                val expr = this[i]
                expr.accept(this@CgAbstractRenderer)
                if (i != length - 1) {
                    print(", ")
                }
            }
        } else { // multiline array
            println() // line break after `int[] x = {`
            withIndent {
                for (i in 0 until length) {
                    val expr = this[i]
                    expr.accept(this@CgAbstractRenderer)

                    if (i == length - 1) {
                        println()
                    } else if (i % elementsInLine == elementsInLine - 1) {
                        println(",")
                    } else {
                        print(", ")
                    }
                }
            }
        }
    }

    protected inline fun withIndent(block: () -> Unit) {
        try {
            pushIndent()
            block()
        } finally {
            popIndent()
        }
    }

    protected open fun isAccessibleBySimpleNameImpl(classId: ClassId): Boolean =
        classId in context.importedClasses ||
                classId.simpleName !in context.importedClasses.map { it.simpleName } && classId.packageName == context.classPackageName

    protected abstract fun escapeNamePossibleKeywordImpl(s: String): String

    protected fun String.escapeNamePossibleKeyword(): String = escapeNamePossibleKeywordImpl(this)

    protected fun ClassId.asString(): String {
        if (!context.shouldOptimizeImports) return canonicalName

        // use simpleNameWithEnclosings instead of simpleName to consider nested classes case
        return if (this.isAccessibleBySimpleName()) simpleNameWithEnclosingClasses else canonicalName
    }

    private fun renderClassPackage(element: CgClass) {
        if (element.packageName.isNotEmpty()) {
            println("package ${element.packageName}${statementEnding}")
            println()
        }
    }

    protected open fun renderClassFileImports(element: CgClassFile) {
        val regularImports = element.imports.filterIsInstance<RegularImport>()
        val staticImports = element.imports.filterIsInstance<StaticImport>()

        for (import in regularImports) {
            renderRegularImport(import)
        }
        if (regularImports.isNotEmpty()) {
            println()
        }

        for (import in staticImports) {
            renderStaticImport(import)
        }
        if (staticImports.isNotEmpty()) {
            println()
        }
    }

    protected abstract fun renderClassVisibility(classId: ClassId)

    protected abstract fun renderClassModality(aClass: CgClass)

    protected fun renderMethodDocumentation(element: CgMethod) {
        element.documentation.accept(this)
    }

    private fun Byte.toStringConstant() = when {
        this == Byte.MAX_VALUE -> "${langPackage}.Byte.MAX_VALUE"
        this == Byte.MIN_VALUE -> "${langPackage}.Byte.MIN_VALUE"
        else -> toStringConstantImpl(this)
    }

    private fun Char.toStringConstant() = when (this) {
        '\'' -> "'\\''"
        else -> "'" + StringEscapeUtils.escapeJava("$this") + "'"
    }

    private fun Short.toStringConstant() = when (this) {
        Short.MAX_VALUE -> "${langPackage}.Short.MAX_VALUE"
        Short.MIN_VALUE -> "${langPackage}.Short.MIN_VALUE"
        else -> toStringConstantImpl(this)
    }

    private fun Int.toStringConstant(): String = toStringConstantImpl(this)

    private fun Long.toStringConstant() = when {
        this == Long.MAX_VALUE -> "${langPackage}.Long.MAX_VALUE"
        this == Long.MIN_VALUE -> "${langPackage}.Long.MIN_VALUE"
        else -> toStringConstantImpl(this)
    }

    private fun Float.toStringConstant() = when {
        isNaN() -> "${langPackage}.Float.NaN"
        this == Float.POSITIVE_INFINITY -> "${langPackage}.Float.POSITIVE_INFINITY"
        this == Float.NEGATIVE_INFINITY -> "${langPackage}.Float.NEGATIVE_INFINITY"
        else -> toStringConstantImpl(this)
    }

    private fun Double.toStringConstant() = when {
        isNaN() -> "${langPackage}.Double.NaN"
        this == Double.POSITIVE_INFINITY -> "${langPackage}.Double.POSITIVE_INFINITY"
        this == Double.NEGATIVE_INFINITY -> "${langPackage}.Double.NEGATIVE_INFINITY"
        else -> "$this"
    }

    private fun Boolean.toStringConstant() =
        if (this) "true" else "false"

    protected fun String.toStringConstant(): String = "\"" + escapeCharacters() + "\""

    protected abstract fun String.escapeCharacters(): String

    private fun ClassId.isAccessibleBySimpleName(): Boolean = isAccessibleBySimpleNameImpl(this)

    companion object {
        fun makeRenderer(
            context: CgContext,
            printer: CgPrinter = CgPrinterImpl()
        ): CgAbstractRenderer {
            val rendererContext = CgRendererContext.fromCgContext(context)
            return makeRenderer(rendererContext, printer)
        }

        fun makeRenderer(
            utilClassKind: UtilClassKind,
            codegenLanguage: CodegenLanguage,
            printer: CgPrinter = CgPrinterImpl()
        ): CgAbstractRenderer {
            val rendererContext = CgRendererContext.fromUtilClassKind(utilClassKind, codegenLanguage)
            return makeRenderer(rendererContext, printer)
        }

        private fun makeRenderer(context: CgRendererContext, printer: CgPrinter): CgAbstractRenderer {
            return context.cgLanguageAssistant.cgRenderer(context, printer)
        }

        /**
         * @see [LONG_CODE_FRAGMENTS]
         */
        private const val LARGE_CODE_BLOCK_SIZE: Int = 1000
    }
}
