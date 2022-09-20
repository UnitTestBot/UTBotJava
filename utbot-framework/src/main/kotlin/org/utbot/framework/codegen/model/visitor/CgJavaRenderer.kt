package org.utbot.framework.codegen.model.visitor

import org.apache.commons.text.StringEscapeUtils
import org.utbot.framework.codegen.RegularImport
import org.utbot.framework.codegen.StaticImport
import org.utbot.framework.codegen.model.tree.AbstractCgClass
import org.utbot.framework.codegen.model.tree.CgAllocateArray
import org.utbot.framework.codegen.model.tree.CgAllocateInitializedArray
import org.utbot.framework.codegen.model.tree.CgAnonymousFunction
import org.utbot.framework.codegen.model.tree.CgArrayAnnotationArgument
import org.utbot.framework.codegen.model.tree.CgArrayInitializer
import org.utbot.framework.codegen.model.tree.CgBreakStatement
import org.utbot.framework.codegen.model.tree.CgConstructorCall
import org.utbot.framework.codegen.model.tree.CgDeclaration
import org.utbot.framework.codegen.model.tree.CgEqualTo
import org.utbot.framework.codegen.model.tree.CgErrorTestMethod
import org.utbot.framework.codegen.model.tree.CgErrorWrapper
import org.utbot.framework.codegen.model.tree.CgExecutableCall
import org.utbot.framework.codegen.model.tree.CgExpression
import org.utbot.framework.codegen.model.tree.CgForLoop
import org.utbot.framework.codegen.model.tree.CgGetJavaClass
import org.utbot.framework.codegen.model.tree.CgGetKotlinClass
import org.utbot.framework.codegen.model.tree.CgGetLength
import org.utbot.framework.codegen.model.tree.CgInnerBlock
import org.utbot.framework.codegen.model.tree.CgMethod
import org.utbot.framework.codegen.model.tree.CgNotNullAssertion
import org.utbot.framework.codegen.model.tree.CgParameterDeclaration
import org.utbot.framework.codegen.model.tree.CgParameterizedTestDataProviderMethod
import org.utbot.framework.codegen.model.tree.CgRegularClass
import org.utbot.framework.codegen.model.tree.CgReturnStatement
import org.utbot.framework.codegen.model.tree.CgStatement
import org.utbot.framework.codegen.model.tree.CgStatementExecutableCall
import org.utbot.framework.codegen.model.tree.CgSwitchCase
import org.utbot.framework.codegen.model.tree.CgSwitchCaseLabel
import org.utbot.framework.codegen.model.tree.CgTestClass
import org.utbot.framework.codegen.model.tree.CgTestClassBody
import org.utbot.framework.codegen.model.tree.CgTestMethod
import org.utbot.framework.codegen.model.tree.CgTypeCast
import org.utbot.framework.codegen.model.tree.CgVariable
import org.utbot.framework.codegen.model.util.CgPrinter
import org.utbot.framework.codegen.model.util.CgPrinterImpl
import org.utbot.framework.codegen.model.util.nullLiteral
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.TypeParameters
import org.utbot.framework.plugin.api.util.wrapperByPrimitive

internal class CgJavaRenderer(context: CgRendererContext, printer: CgPrinter = CgPrinterImpl()) :
    CgAbstractRenderer(context, printer) {

    override val statementEnding: String = ";"

    override val logicalAnd: String
        get() = "&&"

    override val logicalOr: String
        get() = "||"

    override val language: CodegenLanguage = CodegenLanguage.JAVA

    override val langPackage: String = "java.lang"

    override fun visit(element: AbstractCgClass<*>) {
        for (annotation in element.annotations) {
            annotation.accept(this)
        }

        renderClassVisibility(element.id)
        renderClassModality(element)
        if (element.isStatic) {
            print("static ")
        }
        print("class ")
        print(element.simpleName)

        val superclass = element.superclass
        if (superclass != null) {
            print(" extends ${superclass.asString()}")
        }
        if (element.interfaces.isNotEmpty()) {
            print(" implements ")
            element.interfaces.map { it.asString() }.printSeparated()
        }
        println(" {")
        withIndent { element.body.accept(this) }
        println("}")
    }

    override fun visit(element: CgTestClassBody) {
        // render regions for test methods and utils
        val allRegions = element.testMethodRegions + element.nestedClassRegions + element.staticDeclarationRegions
        for ((i, region) in allRegions.withIndex()) {
            if (i != 0) println()

            region.accept(this)
        }
    }

    override fun visit(element: CgArrayAnnotationArgument) {
        print("{")
        element.values.renderSeparated()
        print("}")
    }

    override fun visit(element: CgAnonymousFunction) {
        print("(")
        element.parameters.renderSeparated()
        print(") -> ")
        // TODO introduce CgBlock

        val expression = element.body.singleExpressionOrNull()
        // expression lambda can be rendered without curly braces
        if (expression != null) {
            expression.accept(this)

            return
        }

        visit(element.body)
    }

    override fun visit(element: CgEqualTo) {
        element.left.accept(this)
        print(" == ")
        element.right.accept(this)
    }

    override fun visit(element: CgTypeCast) {
        val expr = element.expression
        val wrappedTargetType = wrapperByPrimitive.getOrDefault(element.targetType, element.targetType)
        val exprTypeIsSimilar = expr.type == element.targetType || expr.type == wrappedTargetType

        // cast for null is mandatory in case of ambiguity - for example, readObject(Object) and readObject(Map)
        if (exprTypeIsSimilar && expr != nullLiteral()) {
            element.expression.accept(this)
            return
        }

        print("(")
        print("(")
        print(wrappedTargetType.asString())
        print(") ")
        element.expression.accept(this)
        print(")")
    }

    override fun visit(element: CgErrorWrapper) {
        element.expression.accept(this)
    }

    // Not-null assertion

    override fun visit(element: CgNotNullAssertion) {
        element.expression.accept(this)
    }

    override fun visit(element: CgParameterDeclaration) {
        if (element.isVararg) {
            print(element.type.elementClassId!!.asString())
            print("...")
        } else {
            print(element.type.asString())
        }
        print(" ")
        print(element.name.escapeNamePossibleKeyword())
    }

    override fun visit(element: CgGetJavaClass) {
        // TODO: check how it works on ref types, primitives, ref arrays, primitive arrays, etc.
        print(element.classId.asString())
        print(".class")
    }

    override fun visit(element: CgGetKotlinClass) {
        // For now we assume that we never need KClass in the generated Java test classes.
        // If it changes, this error may be removed.
        error("KClass attempted to be used in the Java test class")
    }

    override fun visit(element: CgAllocateArray) {
        // TODO: Arsen strongly required to rewrite later
        val typeName = element.type.canonicalName.substringBefore("[")
        val otherDimensions = element.type.canonicalName.substringAfter("]")
        print("new $typeName[${element.size}]$otherDimensions")
    }

    override fun visit(element: CgAllocateInitializedArray) {
        // TODO: same as in visit(CgAllocateArray): we should rewrite the typeName and otherDimensions variables declaration
        // to avoid using substringBefore() and substringAfter() directly
        val typeName = element.type.canonicalName.substringBefore("[")
        val otherDimensions = element.type.canonicalName.substringAfter("]")
        // we can't specify the size of the first dimension when using initializer,
        // as opposed to CgAllocateArray where there is no initializer
        print("new $typeName[]$otherDimensions")
        element.initializer.accept(this)
    }

    override fun visit(element: CgArrayInitializer) {
        val elementsInLine = arrayElementsInLine(element.elementType)

        print("{")
        element.values.renderElements(elementsInLine)
        print("}")
    }

    override fun visit(element: CgGetLength) {
        element.variable.accept(this)
        print(".length")
    }

    override fun visit(element: CgConstructorCall) {
        print("new ")
        print(element.executableId.classId.asString())
        renderExecutableCallArguments(element)
    }

    override fun renderRegularImport(regularImport: RegularImport) {
        val escapedImport = getEscapedImportRendering(regularImport)
        println("import $escapedImport$statementEnding")
    }

    override fun renderStaticImport(staticImport: StaticImport) {
        val escapedImport = getEscapedImportRendering(staticImport)
        println("import static $escapedImport$statementEnding")
    }

    override fun renderMethodSignature(element: CgTestMethod) {
        // test methods always have void return type
        print("public void ")
        print(element.name)

        print("(")
        val newLinesNeeded = element.parameters.size > maxParametersAmountInOneLine
        element.parameters.renderSeparated(newLinesNeeded)
        print(")")

        renderExceptions(element)
    }

    override fun renderMethodSignature(element: CgErrorTestMethod) {
        // error test methods always have void return type
        println("public void ${element.name}()")
    }

    override fun renderMethodSignature(element: CgParameterizedTestDataProviderMethod) {
        //we do not have a good string representation for two-dimensional array, so this strange if-else is required
        val returnType =
            if (element.returnType.simpleName == "Object[][]") "java.lang.Object[][]" else "${element.returnType}"
        print("public static $returnType ${element.name}()")
        renderExceptions(element)
    }

    override fun visit(element: CgInnerBlock) {
        println("{")
        withIndent {
            for (statement in element.statements) {
                statement.accept(this)
            }
        }
        println("}")
    }

    override fun renderForLoopVarControl(element: CgForLoop) {
        print("for (")
        // TODO: rewrite in the future
        with(element.initialization) {
            print(variableType.asString())
            print(" ")
            visit(variable)
            initializer?.let {
                print(" = ")
                it.accept(this@CgJavaRenderer)
            }
            print("$statementEnding ")
        }
        element.condition.accept(this)
        print("$statementEnding ")
        element.update.accept(this)
    }

    override fun renderDeclarationLeftPart(element: CgDeclaration) {
        print(element.variableType.asString())
        print(" ")
        visit(element.variable)
    }

    override fun renderAccess(caller: CgExpression) {
        print(".")
    }

    override fun visit(element: CgSwitchCaseLabel) {
        if (element.label != null) {
            print("case ")
            element.label.accept(this)
        } else {
            print("default")
        }
        println(":")
        withIndent {
            for (statement in element.statements) {
                statement.accept(this)
            }
            // break statement in the end
            CgBreakStatement.accept(this)
        }
    }

    override fun visit(element: CgSwitchCase) {
        print("switch (")
        element.value.accept(this)
        println(") {")
        withIndent {
            for (caseLabel in element.labels) {
                caseLabel.accept(this)
            }
            element.defaultLabel?.accept(this)
        }
        println("}")
    }

    override fun toStringConstantImpl(byte: Byte): String = "(byte) $byte"

    override fun toStringConstantImpl(short: Short): String = "(short) $short"

    override fun toStringConstantImpl(long: Long): String = "${long}L"

    override fun toStringConstantImpl(float: Float): String = "${float}f"

    override fun toStringConstantImpl(int: Int): String = when (int) {
        Int.MAX_VALUE -> "Integer.MAX_VALUE"
        Int.MIN_VALUE -> "Integer.MIN_VALUE"
        else -> "$int"
    }

    override fun String.escapeCharacters(): String = StringEscapeUtils.escapeJava(this)

    override fun renderExecutableCallArguments(executableCall: CgExecutableCall) {
        print("(")
        executableCall.arguments.renderSeparated()
        print(")")
    }

    override fun renderTypeParameters(typeParameters: TypeParameters) {}

    override fun renderExceptionCatchVariable(exception: CgVariable) {
        print("${exception.type} ${exception.name.escapeNamePossibleKeyword()}")
    }

    override fun isAccessibleBySimpleNameImpl(classId: ClassId): Boolean =
        super.isAccessibleBySimpleNameImpl(classId) || classId.packageName == "java.lang"

    override fun escapeNamePossibleKeywordImpl(s: String): String = s

    override fun renderClassVisibility(classId: ClassId) {
        when {
            classId.isPublic -> print("public ")
            classId.isProtected -> print("protected ")
            classId.isPrivate -> print("private ")
        }
    }

    override fun renderClassModality(aClass: AbstractCgClass<*>) {
        when (aClass) {
            is CgTestClass -> Unit
            is CgRegularClass -> if (aClass.id.isFinal) print("final ")
        }
    }

    private fun renderExceptions(method: CgMethod) {
        method.exceptions.takeIf { it.isNotEmpty() }?.let { exceptions ->
            print(" throws ")
            print(exceptions.joinToString(separator = ", ", postfix = " ") { it.asString() })
        }
    }

    /**
     * Returns containing [CgExpression] if [this] represents return statement or one executable call, and null otherwise.
     */
    private fun List<CgStatement>.singleExpressionOrNull(): CgExpression? =
        singleOrNull().let {
            when (it) {
                is CgReturnStatement -> it.expression
                is CgStatementExecutableCall -> it.call
                else -> null
            }
        }
}