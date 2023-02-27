package org.utbot.framework.codegen.renderer

import org.apache.commons.text.StringEscapeUtils
import org.utbot.framework.codegen.domain.RegularImport
import org.utbot.framework.codegen.domain.StaticImport
import org.utbot.framework.codegen.domain.models.CgAllocateArray
import org.utbot.framework.codegen.domain.models.CgAllocateInitializedArray
import org.utbot.framework.codegen.domain.models.CgAnonymousFunction
import org.utbot.framework.codegen.domain.models.CgArrayAnnotationArgument
import org.utbot.framework.codegen.domain.models.CgArrayInitializer
import org.utbot.framework.codegen.domain.models.CgBreakStatement
import org.utbot.framework.codegen.domain.models.CgConstructorCall
import org.utbot.framework.codegen.domain.models.CgDeclaration
import org.utbot.framework.codegen.domain.models.CgEqualTo
import org.utbot.framework.codegen.domain.models.CgErrorTestMethod
import org.utbot.framework.codegen.domain.models.CgErrorWrapper
import org.utbot.framework.codegen.domain.models.CgExecutableCall
import org.utbot.framework.codegen.domain.models.CgExpression
import org.utbot.framework.codegen.domain.models.CgForLoop
import org.utbot.framework.codegen.domain.models.CgGetJavaClass
import org.utbot.framework.codegen.domain.models.CgGetKotlinClass
import org.utbot.framework.codegen.domain.models.CgGetLength
import org.utbot.framework.codegen.domain.models.CgInnerBlock
import org.utbot.framework.codegen.domain.models.CgMethod
import org.utbot.framework.codegen.domain.models.CgNotNullAssertion
import org.utbot.framework.codegen.domain.models.CgParameterDeclaration
import org.utbot.framework.codegen.domain.models.CgParameterizedTestDataProviderMethod
import org.utbot.framework.codegen.domain.models.CgReturnStatement
import org.utbot.framework.codegen.domain.models.CgStatement
import org.utbot.framework.codegen.domain.models.CgStatementExecutableCall
import org.utbot.framework.codegen.domain.models.CgSwitchCase
import org.utbot.framework.codegen.domain.models.CgSwitchCaseLabel
import org.utbot.framework.codegen.domain.models.CgClass
import org.utbot.framework.codegen.domain.models.CgClassBody
import org.utbot.framework.codegen.domain.models.CgFormattedString
import org.utbot.framework.codegen.domain.models.CgFrameworkUtilMethod
import org.utbot.framework.codegen.domain.models.CgLiteral
import org.utbot.framework.codegen.domain.models.CgTestMethod
import org.utbot.framework.codegen.domain.models.CgTypeCast
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.tree.VisibilityModifier
import org.utbot.framework.codegen.util.nullLiteral
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.TypeParameters
import org.utbot.framework.plugin.api.util.isFinal
import org.utbot.framework.plugin.api.util.wrapperByPrimitive

internal class CgJavaRenderer(context: CgRendererContext, printer: CgPrinter = CgPrinterImpl()) :
    CgAbstractRenderer(context, printer) {

    override val statementEnding: String = ";"

    override val logicalAnd: String
        get() = "&&"

    override val logicalOr: String
        get() = "||"

    override val langPackage: String = "java.lang"

    override val ClassId.methodsAreAccessibleAsTopLevel: Boolean
        get() = this == context.generatedClass

    override fun visit(element: CgClass) {
        element.documentation?.accept(this)

        for (annotation in element.annotations) {
            annotation.accept(this)
        }

        renderVisibility(element.visibility)
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

    override fun visit(element: CgClassBody) {
        // render class fields
        for (field in element.fields) {
            field.accept(this)
            println()
        }

        // render regions for test methods and utils
        val allRegions = element.methodRegions + element.nestedClassRegions + element.staticDeclarationRegions
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
        renderVisibility(element.visibility)
        // test methods always have void return type
        print("void ")
        print(element.name)

        print("(")
        val newLinesNeeded = element.parameters.size > maxParametersAmountInOneLine
        element.parameters.renderSeparated(newLinesNeeded)
        print(")")

        renderExceptions(element)
    }

    override fun renderMethodSignature(element: CgErrorTestMethod) {
        renderVisibility(element.visibility)
        // error test methods always have void return type
        println("void ${element.name}()")
    }

    override fun renderMethodSignature(element: CgParameterizedTestDataProviderMethod) {
        //we do not have a good string representation for two-dimensional array, so this strange if-else is required
        val returnType =
            if (element.returnType.simpleName == "Object[][]") "java.lang.Object[][]" else "${element.returnType}"

        renderVisibility(element.visibility)
        print("static $returnType ${element.name}()")
        renderExceptions(element)
    }

    override fun renderMethodSignature(element: CgFrameworkUtilMethod) {
        renderVisibility(element.visibility)
        // framework util methods always have void return type
        print("void ")
        print(element.name)
        print("()")

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

            if (element.addBreakStatementToEnd) {
                CgBreakStatement.accept(this)
            }
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

    override fun visit(element: CgFormattedString) {
        val nonLiteralElements = element.array.filterNot { it is CgLiteral }

        print("String.format(")
        val constructedMsg = buildString {
            element.array.forEachIndexed { index, cgElement ->
                if (cgElement is CgLiteral) append(
                    cgElement.toStringConstant(asRawString = true)
                ) else append("%s")
                if (index < element.array.lastIndex) append(" ")
            }
        }

        print(constructedMsg.toStringConstant())

        // Comma to separate msg from variables
        if (nonLiteralElements.isNotEmpty()) print(", ")
        nonLiteralElements.renderSeparated(newLines = false)
        print(")")
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

    override fun renderVisibility(modifier: VisibilityModifier) {
        when (modifier) {
            VisibilityModifier.PUBLIC -> print("public ")
            VisibilityModifier.PRIVATE -> print("private ")
            VisibilityModifier.PROTECTED -> print("protected ")
            VisibilityModifier.INTERNAL -> print("internal ")
            VisibilityModifier.PACKAGEPRIVATE -> Unit
            else -> error("Java: unexpected visibility modifier -- $modifier")
        }
    }

    override fun renderClassModality(aClass: CgClass) {
        if (aClass.id.isFinal) print("final ")
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