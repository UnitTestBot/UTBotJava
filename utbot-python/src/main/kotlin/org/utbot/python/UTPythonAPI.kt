package org.utbot.python

import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.functionStmts.FunctionDef
import io.github.danielnaczo.python3parser.model.mods.Module
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.typing.MypyAnnotations
import org.utbot.python.utils.moduleToString

data class PythonArgument(val name: String, val annotation: String?)

interface PythonMethod {
    val name: String
    val returnAnnotation: String?
    val arguments: List<PythonArgument>
    val moduleFilename: String
    fun asString(): String
    fun ast(): FunctionDef
    val containingPythonClassId: PythonClassId?
    fun codeLines(): List<String> = moduleToString(Module(listOf(ast().body))).split('\n')
    fun methodSignature(): String = "$name(" + arguments.joinToString(", ") {
        "${it.name}: ${it.annotation ?: pythonAnyClassId.name}"
    } + ")"
}

data class PythonTestSet(
    val method: PythonMethod,
    val executions: List<UtExecution>,
    val errors: List<UtError>,
    val mypyReport: List<MypyAnnotations.MypyReportLine>,
    val classId: PythonClassId? = null,
)