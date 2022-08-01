package org.utbot.framework.codegen.model.visitor

import org.apache.commons.text.StringEscapeUtils
import org.utbot.framework.codegen.RegularImport
import org.utbot.framework.codegen.StaticImport
import org.utbot.framework.codegen.isLanguageKeyword
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.tree.*
import org.utbot.framework.codegen.model.util.CgPrinter
import org.utbot.framework.codegen.model.util.CgPrinterImpl
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.TypeParameters

internal class CgPythonRenderer(context: CgContext, printer: CgPrinter = CgPrinterImpl()) :
    CgAbstractRenderer(context, printer) {
    override val statementEnding: String = ""

    override val logicalAnd: String
        get() = "and"

    override val logicalOr: String
        get() = "or"

    override val language: CodegenLanguage = CodegenLanguage.PYTHON

    override val langPackage: String = "python"

    override fun visit(element: CgErrorWrapper) {
        element.expression.accept(this)
    }

    override fun visit(element: CgTestClass) {
        print("class ")
        print(element.simpleName)
        if (element.superclass != null) {
            print("(${element.superclass.asString()})")
        }
        println(":")
        withIndent { element.body.accept(this) }
        println("")
    }

    override fun visit(element: CgArrayAnnotationArgument) {
        TODO("Not yet implemented")
    }

    override fun visit(element: CgAnonymousFunction) {
        print("lambda ")
        element.parameters.renderSeparated()
        print(": ")

        visit(element.body)
    }

    override fun visit(element: CgEqualTo) {
        element.left.accept(this)
        print(" == ")
        element.right.accept(this)
    }

    override fun visit(element: CgTypeCast) {
        TODO("Not yet implemented")
    }

    override fun visit(element: CgNotNullAssertion) {
        TODO("Not yet implemented")
    }

    override fun visit(element: CgAllocateArray) {
        TODO("Not yet implemented")
    }

    override fun visit(element: CgAllocateInitializedArray) {
        TODO("Not yet implemented")
        /*
        val arrayModel = element.model
        val elementsInLine = arrayElementsInLine(arrayModel.constModel)

        print("[")
        arrayModel.renderElements(element.size, elementsInLine)
        print("]")
         */
    }

    override fun visit(element: CgArrayInitializer) {
        TODO("Not yet implemented")
    }

    override fun visit(element: CgSwitchCaseLabel) {
        TODO("Not yet implemented")
    }

    override fun visit(element: CgSwitchCase) {
        TODO("Not yet implemented")
    }

    override fun visit(element: CgParameterDeclaration) {
        print(element.name.escapeNamePossibleKeyword())
        print(": ")
        print(element.type.asString())
    }

    override fun visit(element: CgGetLength) {
        print("len(")
        element.variable.accept(this)
        print(")")
    }

    override fun visit(element: CgGetJavaClass) {
        TODO("Not yet implemented")
    }

    override fun visit(element: CgGetKotlinClass) {
        TODO("Not yet implemented")
    }

    override fun visit(element: CgConstructorCall) {
        print(element.executableId.classId.asString())
        renderExecutableCallArguments(element)
    }

    override fun renderRegularImport(regularImport: RegularImport) {
        val escapedImport = getEscapedImportRendering(regularImport)  // ???
        print("import $escapedImport")
    }

    override fun renderStaticImport(staticImport: StaticImport) {
        TODO("Not yet implemented")
    }

    override fun renderMethodSignature(element: CgTestMethod) {
        print("def ")
        print(element.name)

        print("(")
        val newLinesNeeded = element.parameters.size > maxParametersAmountInOneLine
        element.parameters.renderSeparated(newLinesNeeded)
        print(")")
    }

    override fun renderMethodSignature(element: CgErrorTestMethod) {
        print("def ${element.name}()")
    }

    override fun renderMethodSignature(element: CgParameterizedTestDataProviderMethod) {
        TODO("Not yet implemented")
    }

    override fun visit(element: CgInnerBlock) {
        withIndent {
            for (statement in element.statements) {
                statement.accept(this)
            }
        }
    }

    override fun renderForLoopVarControl(element: CgForLoop) {
        println("for ??? in ???:")
    }

    override fun renderDeclarationLeftPart(element: CgDeclaration) {
        visit(element.variable)
        print(": ")
        print(element.variableType.asString())
    }

    override fun toStringConstantImpl(byte: Byte): String {
        return "str($byte)"
    }

    override fun toStringConstantImpl(short: Short): String {
        return "str($short)"
    }

    override fun toStringConstantImpl(int: Int): String {
        return "str($int)"
    }

    override fun toStringConstantImpl(long: Long): String {
        return "str($long)"
    }

    override fun toStringConstantImpl(float: Float): String {
        return "str($float)"
    }

    override fun renderAccess(caller: CgExpression) {
        print(".")
    }

    override fun renderTypeParameters(typeParameters: TypeParameters) {
        TODO("Not yet implemented")
    }

    override fun renderExecutableCallArguments(executableCall: CgExecutableCall) {
        print("(")
        executableCall.arguments.renderSeparated()
        print(")")
    }

    override fun renderExceptionCatchVariable(exception: CgVariable) {
        TODO("Not yet implemented")
    }

    override fun escapeNamePossibleKeywordImpl(s: String): String =
        if (isLanguageKeyword(s, context.codegenLanguage)) "`$s`" else s

    override fun String.escapeCharacters(): String =
        StringEscapeUtils.escapeJava(this)
        .replace("$", "\\$")
        .replace("\\f", "\\u000C")
        .replace("\\xxx", "\\\u0058\u0058\u0058")
}