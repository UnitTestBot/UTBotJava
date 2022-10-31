package org.utbot.python.framework.codegen.model.constructor.visitor

import org.apache.commons.text.StringEscapeUtils
import org.utbot.common.WorkaroundReason
import org.utbot.common.workaround
import org.utbot.framework.codegen.PythonImport
import org.utbot.framework.codegen.PythonSysPathImport
import org.utbot.framework.codegen.RegularImport
import org.utbot.framework.codegen.StaticImport
import org.utbot.framework.codegen.model.tree.*
import org.utbot.framework.codegen.model.util.CgPrinter
import org.utbot.framework.codegen.model.util.CgPrinterImpl
import org.utbot.framework.codegen.model.visitor.CgAbstractRenderer
import org.utbot.framework.codegen.model.visitor.CgRendererContext
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.TypeParameters
import org.utbot.framework.plugin.api.WildcardTypeParameter
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.pythonBuiltinsModuleName
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.framework.codegen.model.tree.*

internal class CgPythonRenderer(context: CgRendererContext, printer: CgPrinter = CgPrinterImpl()) :
    CgAbstractRenderer(context, printer), CgPythonVisitor<Unit> {
    override val regionStart: String = "# region"
    override val regionEnd: String = "# endregion"

    override val statementEnding: String = ""

    override val logicalAnd: String
        get() = "and"

    override val logicalOr: String
        get() = "or"

    override val langPackage: String = "python"

    override val ClassId.methodsAreAccessibleAsTopLevel: Boolean
        get() = false

    override fun visit(element: CgTestClassFile) {
        renderClassFileImports(element)

        println()
        println()

        element.declaredClass.accept(this)
    }

    override fun visit(element: AbstractCgClass<*>) {
        print("class ")
        print(element.simpleName)
        if (element.superclass != null) {
            print("(${element.superclass!!.asString()})")
        }
        println(":")
        withIndent { element.body.accept(this) }
        println("")
    }

    override fun visit(element: CgCommentedAnnotation) {
        print("#")
        element.annotation.accept(this)
    }

    override fun visit(element: CgSingleArgAnnotation) {
        print("")
    }

    override fun visit(element: CgMultipleArgsAnnotation) {
        print("")
    }

    override fun visit(element: CgSingleLineComment) {
        println("# ${element.comment}")
    }

    override fun visit(element: CgAbstractMultilineComment) {
        visit(element as CgElement)
    }

    override fun visit(element: CgTripleSlashMultilineComment) {
        element.lines.forEach { line ->
            println("# $line")
        }
    }

    override fun visit(element: CgMultilineComment) {
        val lines = element.lines
        if (lines.isEmpty()) return

        if (lines.size == 1) {
            print("# ${lines.first()}")
            return
        }

        // print lines saving indentation
        print("\"\"\"")
        println(lines.first())
        lines.subList(1, lines.lastIndex).forEach { println(it) }
        print(lines.last())
        println("\"\"\"")
    }

    override fun visit(element: CgDocumentationComment) {
        if (element.lines.all { it.isEmpty() }) return

        println("\"\"\"")
        for (line in element.lines) line.accept(this)
        println("\"\"\"")
    }

    override fun visit(element: CgErrorWrapper) {
        element.expression.accept(this)
    }

    override fun visit(element: CgTestClassBody) {
        // render regions for test methods
        for ((i, region) in (element.testMethodRegions + element.nestedClassRegions).withIndex()) {
            if (i != 0) println()

            region.accept(this)
        }

        if (element.staticDeclarationRegions.isEmpty()) {
            return
        }
    }

    override fun visit(element: CgTryCatch) {
        println("try")
        // TODO introduce CgBlock
        visit(element.statements)
        for ((exception, statements) in element.handlers) {
            print("except")
            renderExceptionCatchVariable(exception)
            println("")
            // TODO introduce CgBlock
            visit(statements, printNextLine = element.finally == null)
        }
        element.finally?.let {
            print("finally")
            // TODO introduce CgBlock
            visit(element.finally!!, printNextLine = true)
        }
    }

    override fun visit(element: CgArrayAnnotationArgument) {
        throw UnsupportedOperationException()
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
        element.expression.accept(this)
    }

    override fun visit(element: CgAllocateArray) {
        print("[None] * ${element.size}")
    }

    override fun visit(element: CgAllocateInitializedArray) {
        print(" [")
        element.initializer.accept(this)
        print(" ]")
    }

    override fun visit(element: CgArrayInitializer) {
        val elementType = element.elementType
        val elementsInLine = arrayElementsInLine(elementType)

        print("[")
        element.values.renderElements(elementsInLine)
        print("]")
    }

    override fun visit(element: CgSwitchCaseLabel) {
        throw UnsupportedOperationException()
    }

    override fun visit(element: CgSwitchCase) {
        throw UnsupportedOperationException()
    }

    override fun visit(element: CgParameterDeclaration) {
        print(element.name.escapeNamePossibleKeyword())
        if (element.type.name != "")
            print(": ")
        print(element.type.name)
    }

    override fun visit(element: CgGetLength) {
        print("len(")
        element.variable.accept(this)
        print(")")
    }

    override fun visit(element: CgGetJavaClass) {
        throw UnsupportedOperationException()
    }

    override fun visit(element: CgGetKotlinClass) {
        throw UnsupportedOperationException()
    }

    override fun visit(element: CgConstructorCall) {
        print(element.executableId.classId.name)
        renderExecutableCallArguments(element)
    }

    override fun renderRegularImport(regularImport: RegularImport) {
        val escapedImport = getEscapedImportRendering(regularImport)
        println("import $escapedImport")
    }

    override fun renderStaticImport(staticImport: StaticImport) {
        throw UnsupportedOperationException()
    }

    private fun renderClassFileImports(element: CgTestClassFile) {
        element.imports
            .toSet()
            .filterIsInstance<PythonImport>()
            .sortedBy { it.order }
            .forEach { renderPythonImport(it) }
    }

    private fun renderPythonImport(pythonImport: PythonImport) {
        if (pythonImport is PythonSysPathImport) {
            println("sys.path.append('${pythonImport.sysPath}')")
        } else if (pythonImport.moduleName == null) {
            println("import ${pythonImport.importName}")
        } else {
            println("from ${pythonImport.moduleName} import ${pythonImport.importName}")
        }
    }

    override fun renderMethodSignature(element: CgTestMethod) {
        print("def ")
        print(element.name)

        print("(")
        val newLinesNeeded = element.parameters.size > maxParametersAmountInOneLine
        val selfParameter = CgThisInstance(pythonAnyClassId)
        (listOf(selfParameter) + element.parameters).renderSeparated(newLinesNeeded)
        print(")")
    }

    override fun renderMethodSignature(element: CgErrorTestMethod) {
        print("def ")
        print(element.name)
        print("(")
        val selfParameter = CgThisInstance(pythonAnyClassId)
        listOf(selfParameter).renderSeparated()
        print(")")
    }

    override fun visit(element: CgErrorTestMethod) {
        renderMethodDocumentation(element)
        renderMethodSignature(element)
        visit(element as CgMethod)
        println("pass")
    }

    override fun renderMethodSignature(element: CgParameterizedTestDataProviderMethod) {
        val returnType = element.returnType.canonicalName
        println("def ${element.name}() -> $returnType: pass")
    }

    override fun visit(element: CgInnerBlock) {
        withIndent {
            for (statement in element.statements) {
                statement.accept(this)
            }
        }
    }

    override fun renderForLoopVarControl(element: CgForLoop) {
        print("for ")
        visit(element.condition)
        print(" in ")
        element.initialization.accept(this@CgPythonRenderer)
        println(":")
    }

    override fun renderDeclarationLeftPart(element: CgDeclaration) {
        visit(element.variable)
    }

    override fun toStringConstantImpl(byte: Byte): String {
        return "b'$byte'"
    }

    override fun toStringConstantImpl(short: Short): String {
        return "$short"
    }

    override fun toStringConstantImpl(int: Int): String {
        return "$int"
    }

    override fun toStringConstantImpl(long: Long): String {
        return "$long"
    }

    override fun toStringConstantImpl(float: Float): String {
        return "$float"
    }

    override fun renderAccess(caller: CgExpression) {
        print(".")
    }

    override fun renderTypeParameters(typeParameters: TypeParameters) {
        if (typeParameters.parameters.isNotEmpty()) {
            print("[")
            if (typeParameters is WildcardTypeParameter) {
                print("typing.Any")
            } else {
                print(typeParameters.parameters.joinToString { it.name })
            }
            print("]")
        }
    }

    override fun renderExecutableCallArguments(executableCall: CgExecutableCall) {
        print("(")
        executableCall.arguments.renderSeparated()
        print(")")
    }

    override fun renderExceptionCatchVariable(exception: CgVariable) {
        print(exception.name.escapeNamePossibleKeyword())
    }

    override fun escapeNamePossibleKeywordImpl(s: String): String = s
    override fun renderClassVisibility(classId: ClassId) {
        throw UnsupportedOperationException()
    }

    override fun renderClassModality(aClass: AbstractCgClass<*>) {
        throw UnsupportedOperationException()
    }

    override fun visit(block: List<CgStatement>, printNextLine: Boolean) {
        println(":")

        val isBlockTooLarge = workaround(WorkaroundReason.LONG_CODE_FRAGMENTS) { block.size > 120 }

        withIndent {
            if (isBlockTooLarge) {
                print("\"\"\"")
                println(" This block of code is ${block.size} lines long and could lead to compilation error")
            }

            for (statement in block) {
                statement.accept(this)
            }

            if (isBlockTooLarge) println("\"\"\"")
        }

        if (printNextLine) println()
    }

    override fun visit(element: CgThisInstance) {
        print("self")
    }

    override fun visit(element: CgMethod) {
        visit(element.statements, printNextLine = false)
    }

    override fun visit(element: CgMethodCall) {
        if (element.caller == null) {
            val module = (element.executableId.classId as PythonClassId).moduleName
            if (module != pythonBuiltinsModuleName) {
                print("$module.")
            }
        } else {
            element.caller!!.accept(this)
            print(".")
        }
        print(element.executableId.name)

        renderTypeParameters(element.typeParameters)
        renderExecutableCallArguments(element)
    }

    override fun visit(element: CgPythonRepr) {
        print(element.content)
    }

    override fun visit(element: CgPythonIndex) {
        visit(element.obj)
        print("[")
        element.index.accept(this)
        print("]")
    }

    override fun visit(element: CgPythonFunctionCall) {
        print(element.name)
        print("(")
        val newLinesNeeded = element.parameters.size > maxParametersAmountInOneLine
        element.parameters.renderSeparated(newLinesNeeded)
        print(")")
    }

    override fun visit(element: CgPythonAssertEquals) {
        print("${element.keyword} ")
        element.expression.accept(this)
        println()
    }

    override fun visit(element: CgPythonRange) {
        print("range(")
        listOf(element.start, element.stop, element.step).renderSeparated()
        print(")")
    }

    override fun visit(element: CgPythonList) {
        print("[")
        element.elements.renderSeparated()
        print("]")
    }

    override fun visit(element: CgPythonTuple) {
        print("(")
        element.elements.renderSeparated()
        if (element.elements.size == 1) {
            print(",")
        }
        print(")")
    }

    override fun visit(element: CgPythonSet) {
        if (element.elements.isEmpty())
            print("set()")
        else {
            print("{")
            element.elements.toList().renderSeparated()
            print("}")
        }
    }

    override fun visit(element: CgPythonDict) {
        print("{")
        element.elements.map { (key, value) ->
            key.accept(this)
            print(": ")
            value.accept(this)
            print(", ")
        }
        print("}")
    }

    override fun visit(element: CgForEachLoop) {
        print("for ")
        element.condition.accept(this)
        print(" in ")
        element.iterable.accept(this)
        println(":")
        withIndent { element.statements.forEach { it.accept(this) } }
    }

    override fun visit(element: CgLiteral) {
        print(element.value.toString())
    }

    override fun String.escapeCharacters(): String =
        StringEscapeUtils
            .escapeJava(this)
            .replace("'", "\\'")
            .replace("\\f", "\\u000C")
            .replace("\\xxx", "\\\u0058\u0058\u0058")
}