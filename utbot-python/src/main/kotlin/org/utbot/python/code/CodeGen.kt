package org.utbot.python.code

import io.github.danielnaczo.python3parser.model.Identifier
import io.github.danielnaczo.python3parser.model.expr.Expression
import io.github.danielnaczo.python3parser.model.expr.atoms.Atom
import io.github.danielnaczo.python3parser.model.expr.atoms.Name
import io.github.danielnaczo.python3parser.model.expr.atoms.Str
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.Attribute
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.arguments.Arguments
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.arguments.Keyword
import io.github.danielnaczo.python3parser.model.expr.datastructures.ListExpr
import io.github.danielnaczo.python3parser.model.expr.datastructures.Tuple
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.Add
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
import org.utbot.framework.plugin.api.python.NormalizedPythonAnnotation
import org.utbot.framework.plugin.api.python.pythonAnyClassId
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
            Alias("inspect"),
            Alias("builtins"),
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

    private fun generateFunctionCallForTopLevelFunction(method: PythonMethod): Atom {
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

    private fun generateMethodCall(method: PythonMethod): Atom {
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
        additionalModules: Set<String> = emptySet(),
        fileForOutputName: String
    ): String {

        val importStatements = generateImportFunctionCode(
            moduleToImport,
            directoriesForSysPath,
            additionalModules + setOf("coverage")
        )

        val testFunctionName = "__run_${method.name}"
        val testFunction = FunctionDef(testFunctionName)

        val parameters = methodArguments.zip(method.arguments).map { (model, argument) ->
            Assign(
                listOf(Name(argument.name)),
                Name(model.toString())
            )
        }

        val resultName = Name("__result")
        val startName = Name("__start")
        val endName = Name("__end")
        val sourcesName = Name("__sources")
        val stmtsName = Name("__stmts")
        val stmtsFilteredName = Name("__stmts_filtered")
        val stmtsFilteredWithDefName = Name("__stmts_filtered_with_def")
        val missedName = Name("__missed")
        val missedFilteredName = Name("__missed_filtered")
        val coverageName = Name("__cov")
        val fullpathName = Name("__fullpath")
        val statusName = Name("__status")
        val exceptionName = Name("__exception")
        val serialisedName = Name("__serialized")
        val fileName = Name("__out_file")

        val fullpath = Assign(
            listOf(fullpathName),
            Str(method.moduleFilename)
        )

        val functionCall =
            if (method.containingPythonClassId == null)
                generateFunctionCallForTopLevelFunction(method)
            else
                generateMethodCall(method)

        val fullFunctionName = Name((
            listOf((functionCall.atomElement as Name).id.name) + functionCall.trailers.mapNotNull {
                if (it is Attribute) {
                    it.attr.name
                } else {
                    null
                }
            }).joinToString(".")
        )

        val coverage = Assign(
            listOf(coverageName),
            Name("coverage.Coverage(data_suffix=True)")
        )
        val startCoverage = Atom(
            coverageName,
            listOf(Attribute(Identifier("start")), createArguments())
        )

        val resultSuccess = Assign(
            listOf(resultName),
            functionCall
        )

        val statusSuccess = Assign(
            listOf(statusName),
            Str("\"" + successStatus + "\"")
        )

        val resultError = Assign(
            listOf(resultName),
            exceptionName
        )

        val statusError = Assign(
            listOf(statusName),
            Str("\"" + failStatus + "\"")
        )

        val stopCoverage = Atom(
            coverageName,
            listOf(Attribute(Identifier("stop")), createArguments())
        )
        val sourcesAndStart = Assign(
            listOf(Tuple(listOf(sourcesName, startName))),
            Atom(
                Name("inspect.getsourcelines"),
                listOf(createArguments(listOf(fullFunctionName)))
            )
        )
        val end = Assign(
            listOf(endName),
            Add(
                startName,
                Atom(Name("len"), listOf(createArguments(listOf(sourcesName))))
            )
        )
        val covAnalysis = Assign(
            listOf(Tuple(listOf(
                Name("_"),
                stmtsName,
                Name("_"),
                missedName,
                Name("_")
            ))),
            Atom(
                coverageName,
                listOf(
                    Attribute(Identifier("analysis2")),
                    createArguments(listOf(fullpathName))
                )
            )
        )
        val clean = Atom(
            coverageName,
            listOf(Attribute(Identifier("erase")), createArguments())
        )
        val stmtsFiltered = Assign(
            listOf(stmtsFilteredName),
            Atom(
                Name(getLinesName),
                listOf(createArguments(listOf(startName, endName, stmtsName)))
            )
        )
        val stmtsFilteredWithDef = Assign(
            listOf(stmtsFilteredWithDefName),
            Add(
                ListExpr(listOf(startName)),
                stmtsFilteredName
            )
        )
        val missedFiltered = Assign(
            listOf(missedFilteredName),
            Atom(
                Name(getLinesName),
                listOf(createArguments(listOf(startName, endName, missedName)))
            )
        )

        val serialize = Assign(
            listOf(serialisedName),
            Atom(
                Name("_PythonTreeSerializer().dumps"),
                listOf(createArguments(listOf(resultName)))
            )
        )

        val jsonDumps = Atom(
            Name("json"),
            listOf(
                Attribute(Identifier("dumps")),
                createArguments(listOf(serialisedName))
            )
        )

        val printStmt = With(
            listOf(
                WithItem(Name("open(\"$fileForOutputName\", \"w\")"), fileName)
            ),
            Atom(
                fileName,
                listOf(
                    Attribute(Identifier("write")),
                    createArguments(
                        listOf(
                            Atom(
                                Name("\"\\n\""),
                                listOf(
                                    Attribute(Identifier("join")),
                                    createArguments(listOf(
                                        ListExpr(
                                            listOf(
                                                Atom(Name("str"), listOf(createArguments(listOf(statusName)))),
                                                Atom(Name("str"), listOf(createArguments(listOf(jsonDumps)))),
                                                Atom(Name("str"), listOf(createArguments(listOf(stmtsFilteredWithDefName)))),
                                                Atom(Name("str"), listOf(createArguments(listOf(missedFilteredName))))
                                            )
                                        )
                                    ))
                                )
                            )
                        )
                    )
                )
            )
        )

        val tryBody = Body(listOf(
            resultSuccess,
            statusSuccess
        ))
        val suppressedBlock = With(
            listOf(WithItem(Atom(
                Name(getStdoutSuppressName),
                listOf(createArguments())
            ))),
            tryBody
        )
        val failBody = Body(listOf(
            resultError,
            statusError
        ))
        val tryHandler = ExceptHandler("Exception", exceptionName.id.name)
        val tryBlock = Try(suppressedBlock, listOf(tryHandler), listOf(failBody))

        (parameters + listOf(
            fullpath,
            coverage,
            startCoverage
        )).forEach { testFunction.addStatement(it) }

        testFunction.addStatement(tryBlock)

        listOf(
            stopCoverage,
            sourcesAndStart,
            end,
            covAnalysis,
            clean,
            stmtsFiltered,
            stmtsFilteredWithDef,
            missedFiltered,
            serialize,
            printStmt
        ).forEach { testFunction.addStatement(it) }

        val runFunction = Atom(
            Name(testFunctionName),
            listOf(createArguments())
        )

        return listOf(
            getStdoutSuppress,
            pythonTreeSerializerCode,
            getLines,
            toString(
                Module(
                    importStatements + listOf(testFunction, runFunction)
                )
            )
        ).joinToString("\n\n")
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

    private const val getLinesName: String = "__get_lines"
    private val getLines: String = """
        def ${this.getLinesName}(start, end, lines):
            return list(filter(lambda x: start < x < end, lines))
    """.trimIndent()

    private const val getStdoutSuppressName: String = "__suppress_stdout"
    private val getStdoutSuppress: String = """
        import os
        from contextlib import contextmanager
        @contextmanager
        def ${this.getStdoutSuppressName}():
            with open(os.devnull, "w") as devnull:
                old_stdout = sys.stdout
                sys.stdout = devnull
                try:
                    yield
                finally:
                    sys.stdout = old_stdout
    """.trimIndent()
}
