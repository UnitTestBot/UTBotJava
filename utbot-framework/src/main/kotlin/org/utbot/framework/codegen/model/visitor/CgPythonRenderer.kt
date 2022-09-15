package org.utbot.framework.codegen.model.visitor

import org.apache.commons.text.StringEscapeUtils
import org.utbot.common.WorkaroundReason
import org.utbot.common.workaround
import org.utbot.framework.codegen.*
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.tree.*
import org.utbot.framework.codegen.model.util.CgPrinter
import org.utbot.framework.codegen.model.util.CgPrinterImpl
import org.utbot.framework.plugin.api.*

internal class CgPythonRenderer(context: CgContext, printer: CgPrinter = CgPrinterImpl()) :
    CgAbstractRenderer(context, printer) {
    override val regionStart: String = "# region"
    override val regionEnd: String = "# endregion"

    override val statementEnding: String = ""

    override val logicalAnd: String
        get() = "and"

    override val logicalOr: String
        get() = "or"

    override val language: CodegenLanguage = CodegenLanguage.PYTHON

    override val langPackage: String = "python"

    override fun visit(element: CgTestClassFile) {
        renderClassFileImports(element)

        println()
        println()

        element.testClass.accept(this)
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
            visit(element.finally, printNextLine = true)
        }
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
        TODO("Not yet implemented")
    }

    override fun visit(element: CgGetKotlinClass) {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

     override fun renderClassFileImports(element: CgTestClassFile) {
        element.imports
            .toSet()
            .filterIsInstance<PythonImport>()
            .sortedBy { it.order }
            .forEach { renderPythonImport(it) }
    }

    override fun renderPythonImport(pythonImport: PythonImport) {
        if (pythonImport is PythonSysPathImport) {
            println("sys.path.append('${pythonImport.sysPath}')")
        }
        else if (pythonImport.moduleName == null) {
            println("import ${pythonImport.importName}")
        }
        else {
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
        print("for ")
        visit(element.condition)
        print(" in ")
        element.initialization.accept(this@CgPythonRenderer)
        println(":")
    }

    override fun renderDeclarationLeftPart(element: CgDeclaration) {
        visit(element.variable)
//        print(": ")
//        print(element.variableType.asString())
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
        visit(element.statements, printNextLine = true)
    }

    override fun visit(element: CgMethodCall) {
        if (element.caller == null) {
            val module = (element.executableId.classId as PythonClassId).moduleName
            if (module != pythonBuiltinsModuleName) {
                print("$module.")
            }
        } else {
            element.caller.accept(this)
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

    override fun visit(element: CgPythonSysPath) {
        println("sys.path.append('${element.newPath}')")
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