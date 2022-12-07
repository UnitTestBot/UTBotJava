package org.utbot.python.framework.codegen.model.constructor.visitor

import org.apache.commons.text.StringEscapeUtils
import org.utbot.common.WorkaroundReason
import org.utbot.common.workaround
import org.utbot.python.framework.codegen.model.PythonImport
import org.utbot.python.framework.codegen.model.PythonSysPathImport
import org.utbot.framework.codegen.domain.RegularImport
import org.utbot.framework.codegen.domain.StaticImport
import org.utbot.framework.codegen.domain.models.CgAbstractMultilineComment
import org.utbot.framework.codegen.domain.models.CgAllocateArray
import org.utbot.framework.codegen.domain.models.CgAllocateInitializedArray
import org.utbot.framework.codegen.domain.models.CgAnonymousFunction
import org.utbot.framework.codegen.domain.models.CgArrayAnnotationArgument
import org.utbot.framework.codegen.domain.models.CgArrayInitializer
import org.utbot.framework.codegen.domain.models.CgClass
import org.utbot.framework.codegen.domain.models.CgClassBody
import org.utbot.framework.codegen.domain.models.CgClassFile
import org.utbot.framework.codegen.domain.models.CgCommentedAnnotation
import org.utbot.framework.codegen.domain.models.CgConstructorCall
import org.utbot.framework.codegen.domain.models.CgDeclaration
import org.utbot.framework.codegen.domain.models.CgDocumentationComment
import org.utbot.framework.codegen.domain.models.CgElement
import org.utbot.framework.codegen.domain.models.CgEqualTo
import org.utbot.framework.codegen.domain.models.CgErrorTestMethod
import org.utbot.framework.codegen.domain.models.CgErrorWrapper
import org.utbot.framework.codegen.domain.models.CgExecutableCall
import org.utbot.framework.codegen.domain.models.CgExpression
import org.utbot.framework.codegen.domain.models.CgForEachLoop
import org.utbot.framework.codegen.domain.models.CgForLoop
import org.utbot.framework.codegen.domain.models.CgFormattedString
import org.utbot.framework.codegen.domain.models.CgGetJavaClass
import org.utbot.framework.codegen.domain.models.CgGetKotlinClass
import org.utbot.framework.codegen.domain.models.CgGetLength
import org.utbot.framework.codegen.domain.models.CgInnerBlock
import org.utbot.framework.codegen.domain.models.CgLiteral
import org.utbot.framework.codegen.domain.models.CgMethod
import org.utbot.framework.codegen.domain.models.CgMethodCall
import org.utbot.framework.codegen.domain.models.CgMultilineComment
import org.utbot.framework.codegen.domain.models.CgMultipleArgsAnnotation
import org.utbot.framework.codegen.domain.models.CgNotNullAssertion
import org.utbot.framework.codegen.domain.models.CgParameterDeclaration
import org.utbot.framework.codegen.domain.models.CgParameterizedTestDataProviderMethod
import org.utbot.framework.codegen.domain.models.CgSingleArgAnnotation
import org.utbot.framework.codegen.domain.models.CgSingleLineComment
import org.utbot.framework.codegen.domain.models.CgStatement
import org.utbot.framework.codegen.domain.models.CgSwitchCase
import org.utbot.framework.codegen.domain.models.CgSwitchCaseLabel
import org.utbot.framework.codegen.domain.models.CgTestMethod
import org.utbot.framework.codegen.domain.models.CgThisInstance
import org.utbot.framework.codegen.domain.models.CgTripleSlashMultilineComment
import org.utbot.framework.codegen.domain.models.CgTryCatch
import org.utbot.framework.codegen.domain.models.CgTypeCast
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.renderer.CgPrinter
import org.utbot.framework.codegen.renderer.CgPrinterImpl
import org.utbot.framework.codegen.renderer.CgAbstractRenderer
import org.utbot.framework.codegen.renderer.CgRendererContext
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.TypeParameters
import org.utbot.framework.plugin.api.WildcardTypeParameter
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.pythonBuiltinsModuleName
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.framework.codegen.model.tree.*

internal class CgPythonRenderer(
    context: CgRendererContext,
    printer: CgPrinter = CgPrinterImpl()
) :
    CgAbstractRenderer(context, printer),
    CgPythonVisitor<Unit> {

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

    override fun visit(element: CgClassFile) {
        renderClassFileImports(element)

        println()
        println()

        element.declaredClass.accept(this)
    }

    override fun visit(element: CgClass) {
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

    override fun renderClassFileImports(element: CgClassFile) {
        element.imports
            .filterIsInstance<PythonImport>()
            .sortedBy { it.order }
            .forEach { renderPythonImport(it) }
    }

    fun renderPythonImport(pythonImport: PythonImport) {
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

    override fun renderClassModality(aClass: CgClass) {
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

    override fun visit(element: CgPythonTree) {
        when(val tree = element.tree) {
            is PythonTree.PrimitiveNode -> {
                print(tree.repr)
            }
            is PythonTree.ListNode -> {
                print("[")
                element.getChildren().renderSeparated()
                print("]")
            }
            is PythonTree.TupleNode -> {
                print("tuple([")
                element.getChildren().renderSeparated()
                print("])")
            }
            is PythonTree.SetNode -> {
                print("{")
                element.getChildren().renderSeparated()
                print("}")
            }
            is PythonTree.DictNode -> {
                print("{")
                element.getDictChildren().map {
                    it.key.accept(this)
                    print(": ")
                    it.value.accept(this)
                    print(", ")
                }
                print("}")
            }
            is PythonTree.ReduceNode -> {
                TODO()
            }
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

    override fun visit(element: CgFormattedString) {
        throw NotImplementedError("String interpolation is not supported in Python renderer")
    }

    override fun String.escapeCharacters(): String =
        StringEscapeUtils
            .escapeJava(this)
            .replace("'", "\\'")
            .replace("\\f", "\\u000C")
            .replace("\\xxx", "\\\u0058\u0058\u0058")
}