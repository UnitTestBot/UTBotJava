package framework.codegen.model.constructor.visitor

import org.apache.commons.text.StringEscapeUtils
import org.utbot.framework.codegen.RegularImport
import org.utbot.framework.codegen.StaticImport
import org.utbot.framework.codegen.isLanguageKeyword
import org.utbot.framework.codegen.model.tree.*
import org.utbot.framework.codegen.model.util.CgPrinter
import org.utbot.framework.codegen.model.util.CgPrinterImpl
import org.utbot.framework.codegen.model.visitor.CgAbstractRenderer
import org.utbot.framework.codegen.model.visitor.CgRendererContext
import org.utbot.framework.plugin.api.BuiltinMethodId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.TypeParameters
import org.utbot.framework.plugin.api.util.isStatic
import settings.JsTestGenerationSettings.fileUnderTestAliases

internal class CgJsRenderer(context: CgRendererContext, printer: CgPrinter = CgPrinterImpl()) :
    CgAbstractRenderer(context, printer) {

    override val statementEnding: String = ""

    override val logicalAnd: String
        get() = "&&"

    override val logicalOr: String
        get() = "||"

    override val langPackage: String = "js"

    override val ClassId.methodsAreAccessibleAsTopLevel: Boolean
        get() = false

    override fun visit(element: CgErrorWrapper) {
        element.expression.accept(this)
        print("alert(\"${element.message}\")")
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

    override fun visit(element: CgParameterDeclaration) {
        if (element.isVararg) {
            print("...")
        }
        print(element.name.escapeNamePossibleKeyword())
    }

    override fun visit(element: CgLiteral) {
        val value = with(element.value) {
            when (this) {
                is Double -> toStringConstant()
                is String -> "\"" + escapeCharacters() + "\""
                else -> "$this"
            }
        }
        print(value)
    }

    private fun Double.toStringConstant() = when {
        isNaN() -> "Number.NaN"
        this == Double.POSITIVE_INFINITY -> "Number.POSITIVE_INFINITY"
        this == Double.NEGATIVE_INFINITY -> "Number.NEGATIVE_INFINITY"
        else -> "$this"
    }

    override fun visit(element: CgStaticsRegion) {
        if (element.content.isEmpty()) return

        print(regionStart)
        element.header?.let { print(" $it") }
        println()

        withIndent {
            for (item in element.content) {
                println()
                item.accept(this)
            }
        }

        println(regionEnd)
    }


    override fun visit(element: CgClass) {
        element.body.accept(this)
    }

    override fun visit(element: CgFieldAccess) {
        element.caller.accept(this)
        renderAccess(element.caller)
        print(element.fieldId.name)
    }

    override fun visit(element: CgArrayElementAccess) {
        element.array.accept(this)
        print("[")
        element.index.accept(this)
        print("]")
    }

    override fun visit(element: CgArrayAnnotationArgument) {
        throw UnsupportedOperationException()
    }

    override fun visit(element: CgAnonymousFunction) {
        print("function (")
        element.parameters.renderSeparated(true)
        println(") {")
        // cannot use visit(element.body) here because { was already printed
        withIndent {
            for (statement in element.body) {
                statement.accept(this)
            }
        }
        print("}")
    }

    override fun visit(element: CgEqualTo) {
        element.left.accept(this)
        print(" == ")
        element.right.accept(this)
    }

    // TODO SEVERE
    override fun visit(element: CgTypeCast) {
        element.expression.accept(this)
//        throw Exception("TypeCast not yet implemented")
    }

    override fun visit(element: CgSpread) {
        print("...")
        element.array.accept(this)
    }

    override fun visit(element: CgNotNullAssertion) {
        throw UnsupportedOperationException("JavaScript does not support not null assertions")
    }

    override fun visit(element: CgAllocateArray) {
        print("new Array(${element.size})")
    }

    override fun visit(element: CgAllocateInitializedArray) {
        print("[")
        element.initializer.accept(this)
        print("]")
    }

    // TODO SEVERE: I am unsure about this
    override fun visit(element: CgArrayInitializer) {
        val elementType = element.elementType
        val elementsInLine = arrayElementsInLine(elementType)

        element.values.renderElements(elementsInLine)
    }

    override fun visit(element: CgClassFile) {
        element.imports.filterIsInstance<RegularImport>().forEach {
            renderRegularImport(it)
        }
        println()
        element.declaredClass.accept(this)
    }

    override fun visit(element: CgSwitchCaseLabel) {
        if (element.label != null) {
            print("case ")
            element.label!!.accept(this)
        } else {
            print("default")
        }
        println(": ")
        visit(element.statements, printNextLine = true)
    }

    @Suppress("DuplicatedCode")
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

    override fun visit(element: CgGetLength) {
        element.variable.accept(this)
        print(".size")
    }

    override fun visit(element: CgGetJavaClass) {
        throw UnsupportedOperationException("No Java classes in JavaScript")
    }

    override fun visit(element: CgGetKotlinClass) {
        throw UnsupportedOperationException("No Kotlin classes in JavaScript")
    }

    override fun visit(element: CgConstructorCall) {
        print("new $fileUnderTestAliases.${element.executableId.classId.name}")
        print("(")
        element.arguments.renderSeparated()
        print(")")
    }

    override fun renderRegularImport(regularImport: RegularImport) {
        println("const ${regularImport.packageName} = require(\"${regularImport.className}\")")
    }

    override fun renderStaticImport(staticImport: StaticImport) {
        throw Exception("Not implemented yet")
    }

    override fun renderMethodSignature(element: CgTestMethod) {
        println("it(\"${element.name}\", function ()")
    }

    override fun renderMethodSignature(element: CgErrorTestMethod) {
        println("it(\"${element.name}\", function ()")

    }

    override fun visit(element: CgMethod) {
        super.visit(element)
        if (element is CgTestMethod || element is CgErrorTestMethod) {
            println(")")
        }
    }

    override fun visit(element: CgErrorTestMethod) {
        renderMethodSignature(element)
        visit(element as CgMethod)
    }

    override fun visit(element: CgThrowStatement) {
        // TODO: Should we render throw statement right here?
    }

    override fun visit(element: CgClassBody) {
        // render regions for test methods
        for ((i, region) in (element.methodRegions + element.nestedClassRegions).withIndex()) {
            if (i != 0) println()

            region.accept(this)
        }

        if (element.staticDeclarationRegions.isEmpty()) {
            return
        }
    }

    override fun renderMethodSignature(element: CgParameterizedTestDataProviderMethod) {
        throw UnsupportedOperationException()
    }

    override fun visit(element: CgNamedAnnotationArgument) {

    }

    override fun visit(element: CgMultipleArgsAnnotation) {

    }

    override fun visit(element: CgMethodCall) {
        val caller = element.caller
        if (caller != null) {
            caller.accept(this)
            renderAccess(caller)
        } else {
            val method = element.executableId
            if (method is BuiltinMethodId) {

            } else if (method.isStatic) {
                val line = if (method.classId.toString() == "undefined") "" else "${method.classId}."
                print("$fileUnderTestAliases.$line")
            } else {
                print("$fileUnderTestAliases.")
            }
        }
        print(element.executableId.name.escapeNamePossibleKeyword())
        renderTypeParameters(element.typeParameters)
        if (element.type.name == "error") {
            print("(")
            element.arguments[0].accept(this@CgJsRenderer)
            print(", ")
            print("Error, ")
            element.arguments[2].accept(this@CgJsRenderer)
            print(")")
        } else {
            renderExecutableCallArguments(element)
        }
    }

    //TODO MINOR: check
    override fun renderForLoopVarControl(element: CgForLoop) {
        print("for (")
        with(element.initialization) {
            print("let ")
            visit(variable)
            print(" = ")
            initializer?.accept(this@CgJsRenderer)
            print("; ")
            visit(element.condition)
            print("; ")
            print(element.update)
        }
    }

    override fun renderDeclarationLeftPart(element: CgDeclaration) {
        if (element.isMutable) print("var ") else print("let ")
        visit(element.variable)
    }

    override fun toStringConstantImpl(byte: Byte) = "$byte"

    override fun toStringConstantImpl(short: Short) = "$short"

    override fun toStringConstantImpl(int: Int) = "$int"

    override fun toStringConstantImpl(long: Long) = "$long"

    override fun toStringConstantImpl(float: Float) = "$float"

    override fun renderAccess(caller: CgExpression) {
        print(".")
    }

    override fun renderTypeParameters(typeParameters: TypeParameters) {
        //TODO MINOR: check
    }

    override fun renderExecutableCallArguments(executableCall: CgExecutableCall) {
        print("(")
        executableCall.arguments.renderSeparated()
        print(")")
    }

    //TODO SEVERE: check
    override fun renderExceptionCatchVariable(exception: CgVariable) {
        print("${exception.name.escapeNamePossibleKeyword()}: ${exception.type}")
    }

    override fun escapeNamePossibleKeywordImpl(s: String): String =
        if (isLanguageKeyword(s, context.cgLanguageAssistant)) "`$s`" else s

    override fun renderClassVisibility(classId: ClassId) {
        TODO("Not yet implemented")
    }

    override fun renderClassModality(aClass: CgClass) {
        TODO("Not yet implemented")
    }

    //TODO MINOR: check
    override fun String.escapeCharacters(): String =
        StringEscapeUtils.escapeJava(this)
            .replace("$", "\\$")
            .replace("\\f", "\\u000C")
            .replace("\\xxx", "\\\u0058\u0058\u0058")
}