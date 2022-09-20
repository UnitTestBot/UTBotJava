package org.utbot.framework.codegen.model.visitor

import org.apache.commons.text.StringEscapeUtils
import org.utbot.common.WorkaroundReason.LONG_CODE_FRAGMENTS
import org.utbot.common.workaround
import org.utbot.framework.codegen.Import
import org.utbot.framework.codegen.RegularImport
import org.utbot.framework.codegen.StaticImport
import org.utbot.framework.codegen.model.UtilClassKind
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.tree.AbstractCgClass
import org.utbot.framework.codegen.model.tree.AbstractCgClassBody
import org.utbot.framework.codegen.model.tree.AbstractCgClassFile
import org.utbot.framework.codegen.model.tree.CgAbstractFieldAccess
import org.utbot.framework.codegen.model.tree.CgAbstractMultilineComment
import org.utbot.framework.codegen.model.tree.CgArrayElementAccess
import org.utbot.framework.codegen.model.tree.CgAssignment
import org.utbot.framework.codegen.model.tree.CgAuxiliaryClass
import org.utbot.framework.codegen.model.tree.CgBreakStatement
import org.utbot.framework.codegen.model.tree.CgComment
import org.utbot.framework.codegen.model.tree.CgCommentedAnnotation
import org.utbot.framework.codegen.model.tree.CgComparison
import org.utbot.framework.codegen.model.tree.CgContinueStatement
import org.utbot.framework.codegen.model.tree.CgCustomTagStatement
import org.utbot.framework.codegen.model.tree.CgDeclaration
import org.utbot.framework.codegen.model.tree.CgDecrement
import org.utbot.framework.codegen.model.tree.CgDoWhileLoop
import org.utbot.framework.codegen.model.tree.CgDocClassLinkStmt
import org.utbot.framework.codegen.model.tree.CgDocCodeStmt
import org.utbot.framework.codegen.model.tree.CgDocMethodLinkStmt
import org.utbot.framework.codegen.model.tree.CgDocPreTagStatement
import org.utbot.framework.codegen.model.tree.CgDocRegularStmt
import org.utbot.framework.codegen.model.tree.CgDocumentationComment
import org.utbot.framework.codegen.model.tree.CgElement
import org.utbot.framework.codegen.model.tree.CgEmptyLine
import org.utbot.framework.codegen.model.tree.CgEnumConstantAccess
import org.utbot.framework.codegen.model.tree.CgErrorTestMethod
import org.utbot.framework.codegen.model.tree.CgErrorWrapper
import org.utbot.framework.codegen.model.tree.CgExecutableCall
import org.utbot.framework.codegen.model.tree.CgExecutableUnderTestCluster
import org.utbot.framework.codegen.model.tree.CgExpression
import org.utbot.framework.codegen.model.tree.CgFieldAccess
import org.utbot.framework.codegen.model.tree.CgForLoop
import org.utbot.framework.codegen.model.tree.CgGreaterThan
import org.utbot.framework.codegen.model.tree.CgIfStatement
import org.utbot.framework.codegen.model.tree.CgIncrement
import org.utbot.framework.codegen.model.tree.CgInnerBlock
import org.utbot.framework.codegen.model.tree.CgIsInstance
import org.utbot.framework.codegen.model.tree.CgLessThan
import org.utbot.framework.codegen.model.tree.CgLiteral
import org.utbot.framework.codegen.model.tree.CgLogicalAnd
import org.utbot.framework.codegen.model.tree.CgLogicalOr
import org.utbot.framework.codegen.model.tree.CgLoop
import org.utbot.framework.codegen.model.tree.CgMethod
import org.utbot.framework.codegen.model.tree.CgMethodCall
import org.utbot.framework.codegen.model.tree.CgMultilineComment
import org.utbot.framework.codegen.model.tree.CgMultipleArgsAnnotation
import org.utbot.framework.codegen.model.tree.CgNamedAnnotationArgument
import org.utbot.framework.codegen.model.tree.CgNonStaticRunnable
import org.utbot.framework.codegen.model.tree.CgParameterDeclaration
import org.utbot.framework.codegen.model.tree.CgParameterizedTestDataProviderMethod
import org.utbot.framework.codegen.model.tree.CgRegion
import org.utbot.framework.codegen.model.tree.CgRegularClass
import org.utbot.framework.codegen.model.tree.CgRegularClassBody
import org.utbot.framework.codegen.model.tree.CgRegularClassFile
import org.utbot.framework.codegen.model.tree.CgReturnStatement
import org.utbot.framework.codegen.model.tree.CgSimpleRegion
import org.utbot.framework.codegen.model.tree.CgSingleArgAnnotation
import org.utbot.framework.codegen.model.tree.CgSingleLineComment
import org.utbot.framework.codegen.model.tree.CgSpread
import org.utbot.framework.codegen.model.tree.CgStatement
import org.utbot.framework.codegen.model.tree.CgStatementExecutableCall
import org.utbot.framework.codegen.model.tree.CgStaticFieldAccess
import org.utbot.framework.codegen.model.tree.CgStaticRunnable
import org.utbot.framework.codegen.model.tree.CgStaticsRegion
import org.utbot.framework.codegen.model.tree.CgTestClass
import org.utbot.framework.codegen.model.tree.CgTestClassFile
import org.utbot.framework.codegen.model.tree.CgTestMethod
import org.utbot.framework.codegen.model.tree.CgTestMethodCluster
import org.utbot.framework.codegen.model.tree.CgThisInstance
import org.utbot.framework.codegen.model.tree.CgThrowStatement
import org.utbot.framework.codegen.model.tree.CgTripleSlashMultilineComment
import org.utbot.framework.codegen.model.tree.CgTryCatch
import org.utbot.framework.codegen.model.tree.CgUtilMethod
import org.utbot.framework.codegen.model.tree.CgVariable
import org.utbot.framework.codegen.model.tree.CgWhileLoop
import org.utbot.framework.codegen.model.util.CgPrinter
import org.utbot.framework.codegen.model.util.CgPrinterImpl
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

internal abstract class CgAbstractRenderer(
    val context: CgRendererContext,
    val printer: CgPrinter = CgPrinterImpl()
) : CgVisitor<Unit>,
    CgPrinter by printer {

    protected abstract val statementEnding: String

    protected abstract val logicalAnd: String
    protected abstract val logicalOr: String

    protected val regionStart: String = "///region"
    protected val regionEnd: String = "///endregion"

    protected abstract val language: CodegenLanguage

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

    private val MethodId.accessibleByName: Boolean
        get() = (context.shouldOptimizeImports && this in context.importedStaticMethods) || classId == context.generatedClass

    override fun visit(element: CgElement) {
        val error =
            "CgRenderer has reached the top of Cg elements hierarchy and did not find a method for ${element::class}"
        throw IllegalArgumentException(error)
    }

    override fun visit(element: AbstractCgClassFile<*>) {
        renderClassPackage(element.declaredClass)
        renderClassFileImports(element)
        element.declaredClass.accept(this)
    }

    override fun visit(element: CgRegularClassFile) {
        visit(element as AbstractCgClassFile<*>)
    }

    override fun visit(element: CgTestClassFile) {
        visit(element as AbstractCgClassFile<*>)
    }

    override fun visit(element: CgRegularClass) {
        visit(element as AbstractCgClass<*>)
    }

    override fun visit(element: CgTestClass) {
        visit(element as AbstractCgClass<*>)
    }

    override fun visit(element: AbstractCgClassBody) {
        visit(element as CgElement)
    }

    override fun visit(element: CgRegularClassBody) {
        val content = element.content
        for ((index, item) in content.withIndex()) {
            item.accept(this)
            println()
            if (index < content.lastIndex) {
                println()
            }
        }
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
    override fun visit(element: CgExecutableUnderTestCluster) {
        // We print the next line after all contained regions to prevent gluing of region ends
        element.render(printLineAfterContentEnd = true)
    }

    /**
     * Renders the region with a specific rendering for [CgTestMethodCluster.description]
     */
    private fun CgRegion<*>.render(printLineAfterContentEnd: Boolean = false) {
        if (content.isEmpty()) return

        print(regionStart)
        header?.let { print(" $it") }
        println()

        if (this is CgTestMethodCluster) description?.accept(this@CgAbstractRenderer)

        for (method in content) {
            println()
            method.accept(this@CgAbstractRenderer)
        }

        if (printLineAfterContentEnd) println()

        println(regionEnd)
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
            print("/* ${lines.first()} */")
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
        for (line in element.lines) line.accept(this)
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

        for (stmt in element.statements) {
            stmt.accept(this)
        }
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
        val value = with(element.value) {
            when (this) {
                is Byte -> toStringConstant()
                is Char -> toStringConstant()
                is Short -> toStringConstant()
                is Int -> toStringConstant()
                is Long -> toStringConstant()
                is Float -> toStringConstant()
                is Double -> toStringConstant()
                is Boolean -> toStringConstant()
                is String -> toStringConstant()
                else -> "$this"
            }
        }
        print(value)
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
        print(element.declaringClass.asString())
        print(".")
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
                caller.accept(this)
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
        classId in context.importedClasses || classId.packageName == context.classPackageName

    protected abstract fun escapeNamePossibleKeywordImpl(s: String): String

    protected fun String.escapeNamePossibleKeyword(): String = escapeNamePossibleKeywordImpl(this)

    protected fun ClassId.asString(): String {
        if (!context.shouldOptimizeImports) return canonicalName

        // use simpleNameWithEnclosings instead of simpleName to consider nested classes case
        return if (this.isAccessibleBySimpleName()) simpleNameWithEnclosings else canonicalName
    }

    private fun renderClassPackage(element: AbstractCgClass<*>) {
        if (element.packageName.isNotEmpty()) {
            println("package ${element.packageName}${statementEnding}")
            println()
        }
    }

    private fun renderClassFileImports(element: AbstractCgClassFile<*>) {
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

    protected abstract fun renderClassModality(aClass: AbstractCgClass<*>)

    private fun renderMethodDocumentation(element: CgMethod) {
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

    private fun String.toStringConstant(): String = "\"" + escapeCharacters() + "\""

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
            return when (context.codegenLanguage) {
                CodegenLanguage.JAVA -> CgJavaRenderer(context, printer)
                CodegenLanguage.KOTLIN -> CgKotlinRenderer(context, printer)
            }
        }

        /**
         * @see [LONG_CODE_FRAGMENTS]
         */
        private const val LARGE_CODE_BLOCK_SIZE: Int = 1000
    }
}
