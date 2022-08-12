package org.utbot.python.code

import io.github.danielnaczo.python3parser.model.Identifier
import io.github.danielnaczo.python3parser.model.expr.Expression
import io.github.danielnaczo.python3parser.model.expr.atoms.Atom
import io.github.danielnaczo.python3parser.model.expr.atoms.Name
import io.github.danielnaczo.python3parser.model.expr.atoms.Str
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.Attribute
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.arguments.Arguments
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.arguments.Keyword
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.Add
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.comparisons.Eq
import io.github.danielnaczo.python3parser.model.mods.Module
import io.github.danielnaczo.python3parser.model.stmts.Body
import io.github.danielnaczo.python3parser.model.stmts.Statement
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.If
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
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.Assert
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.assignStmts.Assign
import io.github.danielnaczo.python3parser.visitors.prettyprint.IndentationPrettyPrint
import io.github.danielnaczo.python3parser.visitors.prettyprint.ModulePrettyPrintVisitor
import org.utbot.framework.plugin.api.*
import org.utbot.python.*


object PythonCodeGenerator {
    private fun toString(module: Module): String {
        val modulePrettyPrintVisitor = ModulePrettyPrintVisitor()
        return modulePrettyPrintVisitor.visitModule(module, IndentationPrettyPrint(0))
    }

    private fun createOutputBlock(outputName: String, outputFilename: String, outputFileAlias: String): With {
        return With(
            listOf(
                WithItem(
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
            )
            ),
            Body(listOf(
                Assign(
                    listOf(Name("out")),
                    Name("dict()")
                ),
                Assign(
                    listOf(Name("out['output']")),
                    Atom(Name("repr"), listOf(createArguments(listOf(Name(outputName)))))
                ),
                If(
                    Name("$outputName != None"),
                    Body(listOf(Assign(
                        listOf(Name("out['type']")),
                        Add(
                            Name("type($outputName).__module__"),
                            Add(
                                Str("."),
                                Atom(
                                    Atom(
                                        Name("type"),
                                        listOf(createArguments(listOf(Name(outputName))))
                                    ),
                                    listOf(Attribute(Identifier("__name__")))
                                ),
                            )
                        )
                    ))),
                    Body(listOf(Name("out['type'] = 'types.NoneType'")))
                ),
                Atom(
                    Name("print"),
                    listOf(
                        createArguments(
                            listOf(Name("json.dumps(out)")),
                            listOf(
                                Keyword(Name("file"), Name(outputFileAlias)),
                                Keyword(Name("end"), Str(""))
                            ),
                        )
                    )
                )
            ))
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
        directoriesForSysPath: List<String>,
        additionalModules: List<String> = emptyList(),
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

        val additionalImport = additionalModules
            .asSequence()
            .map { it.split("[", "]", ",", "|") }
            .flatten()
            .map { it.trim() }
            .mapNotNull {
                if (it.contains(".")) {
                    val module = it.split(".").dropLast(1).joinToString(".")
                    Import(listOf(Alias(module)))
                } else {
                    null
                }
            }
            .toSet().toList()

        val mathImport = ImportFrom("math", listOf(Alias("*")))
        val typingImport = ImportFrom("typing", listOf(Alias("*")))
        val import = ImportFrom(functionPath, listOf(Alias("*")))
        return listOf(systemImport) + systemCalls + additionalImport + listOf(typingImport, mathImport, import)
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

    fun generateRunFunctionCode(
        method: PythonMethod,
        methodArguments: List<UtModel>,
        outputFilename: String,
        errorFilename: String,
        directoriesForSysPath: List<String>,
        moduleToImport: String,
        additionalModules: List<String> = emptyList()
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

        return toString(
            Module(
                importStatements + listOf(testFunction, runFunction)
            )
        )
    }

    fun generateMypyCheckCode(
        method: PythonMethod,
        methodAnnotations: Map<String, NormalizedPythonAnnotation>,
        directoriesForSysPath: List<String>,
        moduleToImport: String
    ): String {
        val importStatements = generateImportFunctionCode(
            moduleToImport,
            directoriesForSysPath,
            methodAnnotations.values.map { it.name }.toSet().toList(),
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