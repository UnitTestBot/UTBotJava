package org.utbot.python

import io.github.danielnaczo.python3parser.Python3Lexer
import io.github.danielnaczo.python3parser.Python3Parser
import io.github.danielnaczo.python3parser.model.AST
import io.github.danielnaczo.python3parser.model.expr.Expression
import io.github.danielnaczo.python3parser.model.expr.atoms.Atom
import io.github.danielnaczo.python3parser.model.expr.atoms.Name
import io.github.danielnaczo.python3parser.model.expr.atoms.Num
import io.github.danielnaczo.python3parser.model.expr.atoms.Str
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.arguments.Arguments
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.arguments.Keyword
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.comparisons.Eq
import io.github.danielnaczo.python3parser.model.mods.Module
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.ClassDef
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.functionStmts.FunctionDef
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.functionStmts.parameters.Parameter
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.Assert
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.assignStmts.Assign
import io.github.danielnaczo.python3parser.visitors.ast.ModuleVisitor
import io.github.danielnaczo.python3parser.visitors.modifier.ModifierVisitor
import io.github.danielnaczo.python3parser.visitors.prettyprint.IndentationPrettyPrint
import io.github.danielnaczo.python3parser.visitors.prettyprint.ModulePrettyPrintVisitor
import org.antlr.v4.runtime.CharStreams.fromString
import org.antlr.v4.runtime.CommonTokenStream
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.PythonIntModel
import org.utbot.framework.plugin.api.PythonStrModel
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.fuzzer.FuzzedConcreteValue
import java.io.File
import java.util.*
import java.util.function.Consumer
import javax.xml.bind.DatatypeConverter.parseLong


class PythonCode(private val body: Module) {
    fun getToplevelFunctions(): List<PythonMethodBody> =
        body.statements.mapNotNull { statement ->
            (statement as? FunctionDef)?.let { functionDef: FunctionDef ->
                PythonMethodBody(functionDef)
            }
        }

    fun getToplevelClasses(): List<PythonClass> =
        body.statements.mapNotNull { statement ->
            (statement as? ClassDef)?.let { classDef: ClassDef ->
                PythonClass(classDef)
            }
        }

    companion object {
        fun getFromString(code: String): PythonCode {
            val lexer = Python3Lexer(fromString(code))
            val tokens = CommonTokenStream(lexer)
            val parser = Python3Parser(tokens)
            val moduleVisitor = ModuleVisitor()
            val ast = moduleVisitor.visit(parser.file_input()) as Module

            return PythonCode(ast)
        }
    }
}

class PythonClass(private val ast: ClassDef) {
    val name: String
        get() = ast.name.name

    val methods: List<PythonMethodBody>
        get() = ast.functionDefs.map { PythonMethodBody(it) }
}

class PythonMethodBody(private val ast: FunctionDef): PythonMethod {
    override val name: String
        get() = ast.name.name

    private val returnTypeAsString: String?
        get() = annotationToString(ast.returns)

    override val returnType: ClassId?
        get() = returnTypeAsString?.let { typeAsStringToClassId(it) }

    // TODO: consider cases of default and named arguments
    private val getParams: List<Parameter> =
        if (ast.parameters.isPresent) ast.parameters.get().params else emptyList()

    override val arguments: List<PythonArgument>
        get() = getParams.map { param ->
            PythonArgument(
                param.parameterName.name,
                annotationToString(param.annotation)?.let { typeAsStringToClassId(it) }
            )
        }

    override fun asString(): String {
        val modulePrettyPrintVisitor = ModulePrettyPrintVisitor()
        return modulePrettyPrintVisitor.visitModule(Module(listOf(ast)), IndentationPrettyPrint(0))
    }

    override fun getConcreteValues(): List<FuzzedConcreteValue> {
        val visitor = ConcreteValuesVisitor()
        val res = mutableListOf<FuzzedConcreteValue>()
        visitor.visitFunctionDef(ast, res)
        return res
    }

    private class ConcreteValuesVisitor: ModifierVisitor<MutableList<FuzzedConcreteValue>>() {
        override fun visitNum(num: Num, res: MutableList<FuzzedConcreteValue>): AST {
            res += (FuzzedConcreteValue(longClassId, parseLong(num.n)))
            return super.visitNum(num, res)
        }

        override fun visitStr(str: Str, res: MutableList<FuzzedConcreteValue>): AST {
            res += FuzzedConcreteValue(
                stringClassId,
                str.s.removeSurrounding("\"", "\"").removeSurrounding("'", "'")
            )
            return super.visitStr(str, res)
        }
    }

    companion object {
        fun typeAsStringToClassId(typeAsString: String): ClassId = ClassId(typeAsString)

        fun annotationToString(annotation: Optional<Expression>): String? =
            if (annotation.isPresent) (annotation.get() as? Name)?.id?.name else null
    }
}

object PythonCodeGenerator {
    fun generateTestCode(testCase: PythonTestCase): List<String> {
        return testCase.executions.mapIndexed { index, utExecution ->
            generateTestCode(testCase.method, utExecution, index)
        }
    }

    fun generateTestCode(method: PythonMethod, execution: UtExecution, number: Int): String {
        val testFunctionName = "${execution.testMethodName?.camelToSnakeCase() ?: "test"}_$number"
        val testFunction = FunctionDef(testFunctionName)

        val parameters = execution.stateBefore.parameters.zip(method.arguments).map { (model, argument) ->
            Assign(
                listOf(Name("${argument.name}_")),
                Name(model.toString())
            )
        }
        parameters.forEach {
            testFunction.addStatement(it)
        }

        val actualName = "actual"
        val keywords = method.arguments.map {
            Keyword(Name(it.name), Name("${it.name}_"))
        }
        val functionCall = Assign(
            listOf(Name(actualName)),
            Atom(
                Name(method.name),
                listOf(
                    Arguments(emptyList(), keywords, emptyList(), emptyList())
                )
            )
        )
        testFunction.addStatement(functionCall)

        val correctResultName = "correct_result"
        val correctResult = Assign(
            listOf(Name(correctResultName)),
            Name(execution.result.toString())
        )
        testFunction.addStatement(correctResult)

        val assertLine = Assert(
            Eq(
                Name(correctResultName),
                Name(actualName)
            )
        )
        testFunction.addStatement(assertLine)

        return PythonMethodBody(testFunction).asString()
    }

    fun saveToFile(filePath: String, code: List<String>) {
        val file = File(filePath)
        file.writeText(code.joinToString("\n\n\n"))
        file.createNewFile()
    }
}

fun String.camelToSnakeCase(): String {
    val camelRegex = "(?<=[a-zA-Z])[A-Z]".toRegex()
    return camelRegex.replace(this) {
        "_${it.value}"
    }.toLowerCase()
}
