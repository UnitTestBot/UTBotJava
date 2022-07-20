package org.utbot.python

import io.github.danielnaczo.python3parser.Python3Lexer
import io.github.danielnaczo.python3parser.Python3Parser
import io.github.danielnaczo.python3parser.model.AST
import io.github.danielnaczo.python3parser.model.Identifier
import io.github.danielnaczo.python3parser.model.expr.Expression
import io.github.danielnaczo.python3parser.model.expr.atoms.Atom
import io.github.danielnaczo.python3parser.model.expr.atoms.Name
import io.github.danielnaczo.python3parser.model.expr.atoms.Num
import io.github.danielnaczo.python3parser.model.expr.atoms.Str
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.Attribute
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.arguments.Arguments
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.arguments.Keyword
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.comparisons.Eq
import io.github.danielnaczo.python3parser.model.mods.Module
import io.github.danielnaczo.python3parser.model.stmts.Body
import io.github.danielnaczo.python3parser.model.stmts.Statement
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.ClassDef
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.functionStmts.FunctionDef
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.functionStmts.parameters.Parameter
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.tryExceptStmts.ExceptHandler
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.tryExceptStmts.Try
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.withStmts.With
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.withStmts.WithItem
import io.github.danielnaczo.python3parser.model.stmts.importStmts.Alias
import io.github.danielnaczo.python3parser.model.stmts.importStmts.Import
import io.github.danielnaczo.python3parser.model.stmts.importStmts.ImportFrom
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.Assert
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.assignStmts.Assign
import io.github.danielnaczo.python3parser.visitors.ast.ModuleVisitor
import io.github.danielnaczo.python3parser.visitors.modifier.ModifierVisitor
import io.github.danielnaczo.python3parser.visitors.prettyprint.IndentationPrettyPrint
import io.github.danielnaczo.python3parser.visitors.prettyprint.ModulePrettyPrintVisitor
import org.antlr.v4.runtime.CharStreams.fromString
import org.antlr.v4.runtime.CommonTokenStream
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.fuzzer.FuzzedConcreteValue
import java.io.File
import java.nio.file.Path
import java.util.*
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
    private fun toString(module: Module): String {
        val modulePrettyPrintVisitor = ModulePrettyPrintVisitor()
        return modulePrettyPrintVisitor.visitModule(module, IndentationPrettyPrint(0))
    }

    fun generateTestCode(testCase: PythonTestCase, directoriesForSysPath: List<String>, moduleToImport: String): String {
        val importFunction = generateImportFunctionCode(
            moduleToImport,
            directoriesForSysPath
        )
        val testCaseCodes = testCase.executions.mapIndexed { index, utExecution ->
            generateTestCode(testCase.method, utExecution, index)
        }
        return toString(Module(importFunction)) + testCaseCodes.joinToString("")
    }

    fun generateTestCode(method: PythonMethod, execution: UtExecution, number: Int): String {
        val testFunctionName = "${execution.testMethodName?.camelToSnakeCase() ?: "test"}_$number"
        val testFunction = FunctionDef(testFunctionName)

        val parameters = execution.stateBefore.parameters.zip(method.arguments).map { (model, argument) ->
            Assign(
                listOf(Name(argument.name)),
                Name(model.toString())
            )
        }
        parameters.forEach {
            testFunction.addStatement(it)
        }

        val actualName = "actual"
        val keywords = method.arguments.map {
            Keyword(Name(it.name), Name(it.name))
        }
        val functionCall = Assign(
            listOf(Name(actualName)),
            Atom(
                Name(method.name),
                listOf(createArguments(emptyList(), keywords))
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

    private fun createOutputBlock(outputName: String, outputFilename: String, outputFileAlias: String): With {
        return With(
            listOf(WithItem(
                Atom(
                    Name("open"),
                    listOf(
                        createArguments(
                            listOf(
                                Str(outputFilename),
                                Str("w")
                            )
                        )
                    )
                ),
                Name(outputFileAlias)
            )),
            Atom(
                Name("print"),
                listOf(
                    createArguments(
                        listOf(Atom(Name(outputName), listOf(Attribute(Identifier("__repr__")), createArguments()))),
                        listOf(
                            Keyword(Name("file"), Name(outputFileAlias)),
                            Keyword(Name("end"), Str(""))
                        ),
                    )
                )
            )
        )
    }

    private fun createArguments(
        args: List<Expression> = emptyList(),
        keywords: List<Keyword> = emptyList(),
        starredArgs: List<Expression> = emptyList(),
        doubleStarredArgs: List<Keyword> = emptyList()
    ): Arguments {
        return Arguments(args, keywords, starredArgs, doubleStarredArgs)
    }

    private fun generateImportFunctionCode(functionPath: String, directoriesForSysPath: List<String>): List<Statement> {
        val systemImport = Import(listOf(Alias("sys")))
        val systemCalls = directoriesForSysPath.map { path ->
            Atom(
                Name("sys.path.append"),
                listOf(
                    createArguments(
                        listOf(Str(path))
                    )
                )
            )
        }
        val import = ImportFrom(functionPath, listOf(Alias("*")))
        return listOf(systemImport) + systemCalls + listOf(import)
    }

    fun generateRunFunctionCode(
        method: PythonMethod,
        methodArguments: List<UtModel>,
        outputFilename: String,
        errorFilename: String,
        codeFilename: String,
        directoriesForSysPath: List<String>,
        moduleToImport: String
    ): File {

        val importStatements = generateImportFunctionCode(
            moduleToImport,
            directoriesForSysPath
        )

        val testFunctionName = "__run_${method.name}"
        val testFunction = FunctionDef(testFunctionName)

        val parameters = methodArguments.zip(method.arguments).map { (model, argument) ->
            Assign(
                listOf(Name(argument.name)),
                Name(model.toString())
            )
        }
        val resultName = "result"
        val keywords = method.arguments.map {
            Keyword(Name(it.name), Name(it.name))
        }
        val functionCall = Assign(
            listOf(Name(resultName)),
            Atom(
                Name(method.name),
                listOf(
                    createArguments(emptyList(), keywords)
                )
            )
        )

        val outputFileAlias = "fout"
        val withOpenResultFile = createOutputBlock(
            resultName,
            outputFilename,
            outputFileAlias
        )

        val errorFileAlias = "ferr"
        val exceptionName = "e"

        val withOpenErrorFile = createOutputBlock(
            exceptionName,
            errorFilename,
            errorFileAlias
        )

        val tryBody = Body(parameters + listOf(functionCall, withOpenResultFile))
        val tryHandler = ExceptHandler("Exception", exceptionName)
        val tryBlock = Try(tryBody, listOf(tryHandler), listOf(withOpenErrorFile))

        testFunction.addStatement(
            tryBlock
        )

        val runFunction = Atom(
            Name(testFunctionName),
            listOf(createArguments())
        )

        val functionCode = toString(
            Module(
                importStatements + listOf(testFunction, runFunction)
            )
        )

        return saveToFile(codeFilename, functionCode)
    }

    fun saveToFile(filePath: String, code: String): File {
        val file = File(filePath)
        file.writeText(code)
        file.createNewFile()
        return file
    }
}

fun String.camelToSnakeCase(): String {
    val camelRegex = "(?<=[a-zA-Z])[A-Z]".toRegex()
    return camelRegex.replace(this) {
        "_${it.value}"
    }.toLowerCase()
}

sealed class EvaluationResult()
data class EvaluationSuccess(val output: String, val isException: Boolean): EvaluationResult()
class EvaluationError: EvaluationResult()

object PythonEvaluation {
    fun evaluate(
        method: PythonMethod,
        methodArguments: List<UtModel>,
        testSourceRoot: String,
        directoriesForSysPath: List<String>,
        moduleToImport: String,
        pythonPath: String
    ): EvaluationResult {
        createDirectory(testSourceRoot)

        val outputFilename = "$testSourceRoot/__output_utbot_run_${method.name}.txt"
        val errorFilename = "$testSourceRoot/__error_utbot_run_${method.name}.txt"
        val codeFilename = "$testSourceRoot/__test_utbot_run_${method.name}.py"

        val file = PythonCodeGenerator.generateRunFunctionCode(
            method,
            methodArguments,
            outputFilename,
            errorFilename,
            codeFilename,
            directoriesForSysPath,
            moduleToImport
        )

        val process = Runtime.getRuntime().exec("$pythonPath $codeFilename")
        process.waitFor()
        val exitCode = process.exitValue()
        if (exitCode != 0)
            return EvaluationError()

        var output = ""
        var isSuccess = false

        val resultFile = File(outputFilename)
        if (resultFile.exists()) {
            output = resultFile.readText()
            resultFile.delete()
            isSuccess = true
        } else {
            val errorFile = File(errorFilename)
            if (errorFile.exists()) {
                output = errorFile.readText()
                errorFile.delete()
            }
        }

        file.delete()
        return EvaluationSuccess(output, !isSuccess)
    }

    private fun createDirectory(path: String) {
        File(path).mkdir()
    }
}
