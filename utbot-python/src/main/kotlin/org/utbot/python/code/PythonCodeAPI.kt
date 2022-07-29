package org.utbot.python.code

import io.github.danielnaczo.python3parser.Python3Lexer
import io.github.danielnaczo.python3parser.Python3Parser
import io.github.danielnaczo.python3parser.model.AST
import io.github.danielnaczo.python3parser.model.expr.Expression
import io.github.danielnaczo.python3parser.model.expr.atoms.Name
import io.github.danielnaczo.python3parser.model.expr.atoms.Num
import io.github.danielnaczo.python3parser.model.expr.atoms.Str
import io.github.danielnaczo.python3parser.model.mods.Module
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.ClassDef
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.functionStmts.FunctionDef
import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.functionStmts.parameters.Parameter
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
import org.utbot.python.*
import java.math.BigInteger
import java.util.*
import javax.xml.bind.DatatypeConverter.parseLong


class PythonCode(private val body: Module, val filename: String? = null) {
    fun getToplevelFunctions(): List<PythonMethodBody> =
        body.statements.mapNotNull { statement ->
            (statement as? FunctionDef)?.let { functionDef: FunctionDef ->
                PythonMethodBody(functionDef)
            }
        }

    fun getToplevelClasses(): List<PythonClass> =
        body.statements.mapNotNull { statement ->
            (statement as? ClassDef)?.let { classDef: ClassDef ->
                PythonClass(classDef, filename)
            }
        }

    companion object {
        fun getFromString(code: String, filename: String? = null): PythonCode {
            val lexer = Python3Lexer(fromString(code))
            val tokens = CommonTokenStream(lexer)
            val parser = Python3Parser(tokens)
            val moduleVisitor = ModuleVisitor()
            val ast = moduleVisitor.visit(parser.file_input()) as Module

            return PythonCode(ast, filename)
        }
    }
}

class PythonClass(private val ast: ClassDef, val filename: String? = null) {
    val name: String
        get() = ast.name.name

    val methods: List<PythonMethodBody>
        get() = ast.functionDefs.map { PythonMethodBody(it) }

    val initFunction: PythonMethodBody?
        get() = ast.functionDefs.find { it.name.name == "__init__" } ?.let { PythonMethodBody(it) }
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
                typeAsStringToClassId( annotationToString(param.annotation) ?: "Any" )
            )
        }

    override fun asString(): String {
        val modulePrettyPrintVisitor = ModulePrettyPrintVisitor()
        return modulePrettyPrintVisitor.visitModule(Module(listOf(ast)), IndentationPrettyPrint(0))
    }

    override fun ast(): FunctionDef {
        return ast
    }

    companion object {
        fun typeAsStringToClassId(typeAsString: String): ClassId = ClassId(typeAsString)

        fun annotationToString(annotation: Optional<Expression>): String? =
            if (annotation.isPresent) (annotation.get() as? Name)?.id?.name else null
    }
}
