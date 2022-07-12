package org.utbot.python

import io.github.danielnaczo.python3parser.Python3Lexer
import io.github.danielnaczo.python3parser.Python3Parser
import io.github.danielnaczo.python3parser.model.expr.Expression
import io.github.danielnaczo.python3parser.model.expr.atoms.Name
import io.github.danielnaczo.python3parser.model.mods.Module
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.functionStmts.FunctionDef
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.functionStmts.parameters.Parameter
import io.github.danielnaczo.python3parser.visitors.ast.ModuleVisitor
import org.antlr.v4.runtime.CharStreams.fromString
import org.antlr.v4.runtime.CommonTokenStream
import org.utbot.framework.plugin.api.ClassId
import java.util.*

class PythonCode(private val body: Module) {

    fun getToplevelFunctions(): List<PythonMethodBody> =
        body.statements.mapNotNull { statement ->
            (statement as? FunctionDef)?.let { functionDef: FunctionDef ->
                PythonMethodBody(functionDef)
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

class PythonMethodBody(private val ast: FunctionDef): PythonMethod {
    override val name: String
        get() = ast.name.name

    private val returnTypeAsString: String?
        get() = annotationToString(ast.returns)

    override val returnType: ClassId?
        get() = returnTypeAsString?.let { typeAsStringToClassId(it) }

    // TODO: consider cases of default and named arguments
    /* private */ val getParams: List<Parameter> =
        if (ast.parameters.isPresent) ast.parameters.get().params else emptyList()

    override val arguments: List<PythonArgument>
        get() = getParams.map { param ->
            PythonArgument(
                param.parameterName.name,
                annotationToString(param.annotation)?.let { typeAsStringToClassId(it) }
            )
        }

    companion object {
        fun typeAsStringToClassId(typeAsString: String): ClassId? = TODO("Not yet implemented")

        fun annotationToString(annotation: Optional<Expression>): String? =
            if (annotation.isPresent) (annotation.get() as? Name)?.id?.name else null
    }
}