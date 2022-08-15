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
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.assignStmts.AnnAssign
import io.github.danielnaczo.python3parser.visitors.ast.ModuleVisitor
import io.github.danielnaczo.python3parser.visitors.modifier.ModifierVisitor
import io.github.danielnaczo.python3parser.visitors.prettyprint.IndentationPrettyPrint
import io.github.danielnaczo.python3parser.visitors.prettyprint.ModulePrettyPrintVisitor
import org.antlr.v4.runtime.CharStreams.fromString
import org.antlr.v4.runtime.CommonTokenStream
import org.utbot.framework.plugin.api.*
import org.utbot.python.*
import java.util.*


class PythonCode(private val body: Module, val filename: String? = null, val pythonModule: String? = null) {
    fun getToplevelFunctions(): List<PythonMethodBody> =
        body.functionDefs.mapNotNull { functionDef ->
                PythonMethodBody(functionDef, filename ?: "")
            }

    fun getToplevelClasses(): List<PythonClass> =
        body.classDefs.mapNotNull { classDef ->
                PythonClass(classDef, filename, pythonModule)
            }

    companion object {
        fun getFromString(code: String, filename: String? = null, pythonModule: String? = null): PythonCode {
            val ast = textToModule(code)
            return PythonCode(ast, filename, pythonModule)
        }
    }
}

class PythonClass(private val ast: ClassDef, val filename: String? = null, val pythonModule: String? = null) {
    val name: String
        get() = ast.name.name

    val methods: List<PythonMethodBody>
        get() = ast.functionDefs.map { PythonMethodBody(it, filename ?: "", pythonClassId) }

    val pythonClassId: PythonClassId?
        get() = pythonModule?.let { PythonClassId("$it.$name") }

    val initSignature: List<PythonArgument>?
        get() {
            val ordinary = ast.functionDefs.find { it.name.name == "__init__" } ?.let {
                PythonMethodBody(it, filename ?: "")
            }
            if (ordinary != null) {
                return ordinary.arguments.drop(1) // drop 'self' parameter
            }
            if (ast.decorators.any { it.name.name == "dataclass" }) {
                return topLevelFields.map {
                    PythonArgument(
                        (it.target as? Name)?.id?.name ?: return null,
                        astToString(it.annotation).trim()
                    )
                }
            }
            return null
        }

    val topLevelFields: List<AnnAssign>
        get() = (ast.body as? Body)?.statements?.mapNotNull { it as? AnnAssign } ?: emptyList()
}

class PythonMethodBody(
    private val ast: FunctionDef,
    override val moduleFilename: String = "",
    override val containingPythonClassId: PythonClassId? = null
): PythonMethod {
    override val name: String
        get() = ast.name.name

    override val returnAnnotation: String?
        get() = annotationToString(ast.returns)

    // TODO: consider cases of default and named arguments
    private val getParams: List<Parameter> =
        if (ast.parameters.isPresent) ast.parameters.get().params else emptyList()

    override val arguments: List<PythonArgument>
        get() = getParams.map { param ->
            PythonArgument(
                param.parameterName.name,
                annotationToString(param.annotation)
            )
        }

    override fun asString(): String {
        return astToString(ast)
    }

    override fun ast(): FunctionDef {
        return ast
    }

    companion object {
        fun typeAsStringToClassId(typeAsString: String): PythonClassId = PythonClassId(typeAsString)

        fun annotationToString(annotation: Optional<Expression>): String? =
            if (annotation.isPresent) astToString(annotation.get()).trim() else null
    }
}

fun astToString(stmt: Statement): String {
    val modulePrettyPrintVisitor = ModulePrettyPrintVisitor()
    return modulePrettyPrintVisitor.visitModule(Module(listOf(stmt)), IndentationPrettyPrint(0))
}

fun textToModule(code: String): Module {
    val lexer = Python3Lexer(fromString(code))
    val tokens = CommonTokenStream(lexer)
    val parser = Python3Parser(tokens)
    val moduleVisitor = ModuleVisitor()
    return moduleVisitor.visit(parser.file_input()) as Module
}

object AnnotationProcessor {
    fun getTypesFromAnnotation(annotation: NormalizedPythonAnnotation): Set<PythonClassId> {
        val annotationAST = textToModule(annotation.name)
        val visitor = Visitor()
        val result = mutableSetOf<PythonClassId>()
        visitor.visitModule(annotationAST, result)
        return result
    }

    private class Visitor: ModifierVisitor<MutableSet<PythonClassId>>() {
        override fun visitAtom(atom: Atom, param: MutableSet<PythonClassId>): AST {
            parse(
                nameWithPrefixFromAtom(apply()),
                onError = null,
                atom
            ) { it } ?.let { typeName ->
                if (typeName != pythonAnyClassId.name)
                    param.add(PythonClassId(typeName))
            }

            return super.visitAtom(atom, param)
        }
    }
}