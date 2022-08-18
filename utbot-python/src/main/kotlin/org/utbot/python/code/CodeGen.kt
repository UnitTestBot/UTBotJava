package org.utbot.python.code

import io.github.danielnaczo.python3parser.model.Identifier
import io.github.danielnaczo.python3parser.model.expr.Expression
import io.github.danielnaczo.python3parser.model.expr.atoms.Atom
import io.github.danielnaczo.python3parser.model.expr.atoms.Name
import io.github.danielnaczo.python3parser.model.expr.atoms.Str
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.Attribute
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.arguments.Arguments
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.arguments.Keyword
import io.github.danielnaczo.python3parser.model.mods.Module
import io.github.danielnaczo.python3parser.model.stmts.Body
import io.github.danielnaczo.python3parser.model.stmts.Statement
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.functionStmts.FunctionDef
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.functionStmts.parameters.Parameter
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.functionStmts.parameters.Parameters
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.tryExceptStmts.ExceptHandler
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.tryExceptStmts.Try
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.withStmts.With
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.withStmts.WithItem
import io.github.danielnaczo.python3parser.model.stmts.importStmts.Alias
import io.github.danielnaczo.python3parser.model.stmts.importStmts.Import
import io.github.danielnaczo.python3parser.model.stmts.importStmts.ImportFrom
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.assignStmts.Assign
import io.github.danielnaczo.python3parser.visitors.prettyprint.IndentationPrettyPrint
import io.github.danielnaczo.python3parser.visitors.prettyprint.ModulePrettyPrintVisitor
import org.utbot.framework.plugin.api.*
import org.utbot.python.*
import org.utbot.python.code.AnnotationProcessor.getModulesFromAnnotation


object PythonCodeGenerator {
    private val pythonTreeSerializerCode = PythonCodeGenerator::class.java.getResource("/python_tree_serializer.py")
        ?.readText(Charsets.UTF_8)
        ?: error("Didn't find preprocessed_values.json")

    private fun toString(module: Module): String {
        val modulePrettyPrintVisitor = ModulePrettyPrintVisitor()
        return modulePrettyPrintVisitor.visitModule(module, IndentationPrettyPrint(0))
    }

    private fun createOutputBlock(outputName: String, status: String): List<Statement> {
        return listOf(
            Assign(
                listOf(Name("out")),
                Atom(
                    Name(
                        "_PythonTreeSerializer().dumps"
                    ),
                    listOf(createArguments(listOf(Name(outputName))))
                )
            ),
            Atom(
                Name("print"),
                listOf(
                    createArguments(
                        listOf(Str("'$status'"), Name("json.dumps(out)")),
                        listOf(
                            Keyword(Name("end"), Str("''")),
                            Keyword(Name("sep"), Str("'\\n'"))
                        )
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

    private fun generateImportFunctionCode(
        functionPath: String,
        directoriesForSysPath: Set<String>,
        additionalModules: Set<String> = emptySet(),
    ): List<Statement> {
        val systemImport = Import(listOf(
            Alias("sys"),
            Alias("typing"),
            Alias("json"),
            Alias("builtins"),
            Alias("inspect"),
        ))
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

        val additionalImport = additionalModules.map { Import(listOf(Alias(it))) }
        val import = ImportFrom(functionPath, listOf(Alias("*")))
        return listOf(systemImport) + systemCalls + additionalImport + listOf(import)
    }

    private fun generateFunctionCallForTopLevelFunction(method: PythonMethod): Expression {
        val keywords = method.arguments.map {
            Keyword(Name(it.name), Name(it.name))
        }
        return Atom(
            Name(method.name),
            listOf(
                createArguments(emptyList(), keywords)
            )
        )
    }

    private fun generateMethodCall(method: PythonMethod): Expression {
        assert(method.containingPythonClassId != null)
        val keywords = method.arguments.drop(1).map {
            Keyword(Name(it.name), Name(it.name))
        }
        return Atom(
            Name(method.arguments[0].name),
            listOf(
                Attribute(Identifier(method.name)),
                createArguments(emptyList(), keywords)
            )
        )
    }

    const val successStatus = "success"
    const val failStatus = "fail"

    fun generateRunFunctionCode(
        method: PythonMethod,
        methodArguments: List<UtModel>,
        directoriesForSysPath: Set<String>,
        moduleToImport: String,
        additionalModules: Set<String> = emptySet()
    ): String {

        val importStatements = generateImportFunctionCode(
            moduleToImport,
            directoriesForSysPath,
            additionalModules
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
        val functionCall =
            Assign(
                listOf(Name(resultName)),
                if (method.containingPythonClassId == null)
                    generateFunctionCallForTopLevelFunction(method)
                else
                    generateMethodCall(method)
            )

        val okOutputBlock = createOutputBlock(
            resultName,
            successStatus
        )

        val exceptionName = "e"
        val failOutputBlock = createOutputBlock(
            exceptionName,
            failStatus
        )

        val tryBody = Body(parameters + listOf(functionCall) + okOutputBlock)
        val tryHandler = ExceptHandler("Exception", exceptionName)
        val tryBlock = Try(tryBody, listOf(tryHandler), listOf(Body(failOutputBlock)))

        testFunction.addStatement(
            tryBlock
        )

        val runFunction = Atom(
            Name(testFunctionName),
            listOf(createArguments())
        )

        return pythonTreeSerializerCode + "\n\n\n" + toString(
            Module(
                importStatements + listOf(testFunction, runFunction)
            )
        )
    }

    fun generateMypyCheckCode(
        method: PythonMethod,
        methodAnnotations: Map<String, NormalizedPythonAnnotation>,
        directoriesForSysPath: Set<String>,
        moduleToImport: String
    ): String {
        val importStatements = generateImportFunctionCode(
            moduleToImport,
            directoriesForSysPath,
            methodAnnotations.values.flatMap { annotation ->
                getModulesFromAnnotation(annotation)
            }.toSet(),
        )

        val parameters = Parameters(
            method.arguments.map { argument ->
                Parameter("${argument.name}: ${methodAnnotations[argument.name] ?: pythonAnyClassId.name}")
            },
        )

        val testFunctionName = "__mypy_check_${method.name}"
        val testFunction = FunctionDef(
            testFunctionName,
            parameters,
            method.ast().body
        )

        return toString(
            Module(
                importStatements + listOf(testFunction)
            )
        )
    }
}