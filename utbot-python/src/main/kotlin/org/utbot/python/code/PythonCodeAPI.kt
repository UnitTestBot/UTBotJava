package org.utbot.python.code

import io.github.danielnaczo.python3parser.Python3Lexer
import io.github.danielnaczo.python3parser.Python3Parser
import io.github.danielnaczo.python3parser.model.AST
import io.github.danielnaczo.python3parser.model.expr.Expression
import io.github.danielnaczo.python3parser.model.expr.atoms.Atom
import io.github.danielnaczo.python3parser.model.expr.atoms.Name
import io.github.danielnaczo.python3parser.model.mods.Module
import io.github.danielnaczo.python3parser.model.stmts.Body
import io.github.danielnaczo.python3parser.model.stmts.Statement
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.ClassDef
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.functionStmts.FunctionDef
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.functionStmts.parameters.Parameter
import io.github.danielnaczo.python3parser.model.stmts.importStmts.Import
import io.github.danielnaczo.python3parser.model.stmts.importStmts.ImportFrom
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.assignStmts.AnnAssign
import io.github.danielnaczo.python3parser.visitors.ast.ModuleVisitor
import io.github.danielnaczo.python3parser.visitors.modifier.ModifierVisitor
import io.github.danielnaczo.python3parser.visitors.prettyprint.IndentationPrettyPrint
import io.github.danielnaczo.python3parser.visitors.prettyprint.ModulePrettyPrintVisitor
import mu.KotlinLogging
import org.antlr.v4.runtime.CharStreams.fromString
import org.antlr.v4.runtime.CommonTokenStream
import org.utbot.python.PythonArgument
import org.utbot.python.PythonMethod
import org.utbot.python.framework.api.python.NormalizedPythonAnnotation
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.utils.moduleOfType
import org.utbot.python.utils.moduleToString
import java.util.*

private val logger = KotlinLogging.logger {}

class PythonCode(
    private val body: Module,
    private val filename: String,
    private val pythonModule: String? = null
) {
    fun getToplevelFunctions(): List<PythonMethodBody> =
        body.functionDefs.mapNotNull { functionDef ->
            PythonMethodBody(functionDef, filename)
        }

    fun getToplevelClasses(): List<PythonClass> =
        body.classDefs?.mapNotNull { classDef ->
            PythonClass(classDef, filename, pythonModule)
        } ?: emptyList()

    fun getToplevelModules(): List<PythonModule> =
        body.statements?.flatMap { statement ->
            when (statement) {
                is Import -> statement.names.map { it.name.name }
                is ImportFrom -> {
                    try {
                        listOf(statement.module.get().name)
                    } catch (e: NoSuchElementException) {
                        emptyList()
                    }
                }

                else -> emptyList()
            }
        }?.toSet()?.map {
            PythonModule(it)
        } ?: emptyList()

    companion object {
        fun getFromString(code: String, filename: String, pythonModule: String? = null): PythonCode? {
            logger.debug("Parsing file $filename")
            return try {
                val ast = textToModule(code)
                if (ast.statements == null) null else PythonCode(ast, filename, pythonModule)
            } catch (_: Exception) {
                null
            }
        }
    }
}

data class PythonModule(
    val name: String,
)

class PythonClass(
    private val ast: ClassDef,
    val filename: String? = null,
    private val pythonModule: String? = null
) {
    val name: String
        get() = ast.name.name

    val methods: List<PythonMethodBody>
        get() = ast.functionDefs?.map {
            PythonMethodBody(it, filename ?: "", pythonClassId)
        } ?: emptyList()

    val pythonClassId: PythonClassId?
        get() = pythonModule?.let { PythonClassId("$it.$name") }

    val initSignature: List<PythonArgument>?
        get() {
            val ordinary = ast.functionDefs
                ?.find { it.name.name == "__init__" }
                ?.let { PythonMethodBody(it, filename ?: "") }

            if (ordinary != null) {
                return ordinary.arguments.drop(1) // drop 'self' parameter
            }

            val isDataclass = ast.decorators.any { it.name.name == "dataclass" }

            if (isDataclass) {
                return topLevelFields.map {
                    PythonArgument(
                        (it.target as? Name)?.id?.name ?: return null,
                        astToString(it.annotation).trim()
                    )
                }
            }

            val noOneArgument = ast.decorators.isEmpty() && (ast.arguments == null || !ast.arguments.isPresent)
            if (noOneArgument) {
                return emptyList()
            }

            return null
        }

    val topLevelFields: List<AnnAssign>
        get() = (ast.body as? Body)?.statements?.mapNotNull { it as? AnnAssign } ?: emptyList()
}

class PythonMethodBody(
    ast: FunctionDef,
    moduleFilename: String = "",
    containingPythonClassId: PythonClassId? = null
) : PythonMethod(
    name = ast.name.name,
    returnAnnotation = annotationToString(ast.returns),
    arguments = getArguments(ast),
    moduleFilename,
    containingPythonClassId,
    codeAsString = moduleToString(Module(listOf(ast.body)))
) {

    override val oldAst: FunctionDef = ast

    companion object {
        fun annotationToString(annotation: Optional<Expression>): String? =
            if (annotation.isPresent) astToString(annotation.get()).trim() else null

        private fun getParams(ast: FunctionDef): List<Parameter> =
            if (ast.parameters.isPresent)
                ast.parameters.get().params ?: emptyList()
            else
                emptyList()

        fun getArguments(ast: FunctionDef): List<PythonArgument> =
            getParams(ast).map { param ->
            PythonArgument(
                param.parameterName.name,
                annotationToString(param.annotation)
            )
        }
    }
}

fun astToString(stmt: Statement): String {
    val modulePrettyPrintVisitor = ModulePrettyPrintVisitor()
    return modulePrettyPrintVisitor.visitModule(Module(listOf(stmt)), IndentationPrettyPrint(0))
}

fun textToModule(code: String): Module {
    val lexer = Python3Lexer(fromString(code + "\n"))
    val tokens = CommonTokenStream(lexer)
    val parser = Python3Parser(tokens)
    val moduleVisitor = ModuleVisitor()
    return moduleVisitor.visit(parser.file_input()) as Module
}

object AnnotationProcessor {
    fun getModulesFromAnnotation(annotation: NormalizedPythonAnnotation): Set<String> {
        val annotationAST = textToModule(annotation.name)
        val visitor = Visitor()
        val result = mutableSetOf<String>()
        visitor.visitModule(annotationAST, result)
        return result
    }

    private class Visitor : ModifierVisitor<MutableSet<String>>() {
        override fun visitAtom(atom: Atom, param: MutableSet<String>): AST {
            parse(
                nameWithPrefixFromAtom(apply()),
                onError = null,
                atom
            ) { it }?.let { typeName ->
                moduleOfType(typeName)?.let { param.add(it) }
            }

            return super.visitAtom(atom, param)
        }
    }
}