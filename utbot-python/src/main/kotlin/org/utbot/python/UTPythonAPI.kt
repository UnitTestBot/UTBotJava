package org.utbot.python

import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.functionStmts.FunctionDef
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.*
import org.utbot.python.typing.MypyAnnotations

data class PythonArgument(val name: String, val annotation: String?)

interface PythonMethod {
    val name: String
    val returnAnnotation: String?
    val arguments: List<PythonArgument>
    val moduleFilename: String
    fun asString(): String
    fun ast(): FunctionDef
    val containingPythonClassId: PythonClassId?
}

sealed class PythonResult(val parameters: List<UtModel>, val types: List<String>)

data class PythonTestSet(
    val method: PythonMethod,
    val executions: List<UtExecution>,
    val errors: List<UtError>,
    val mypyReport: List<MypyAnnotations.MypyReportLine>,
    val classId: PythonClassId? = null,
)