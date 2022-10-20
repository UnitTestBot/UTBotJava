package org.utbot.python

import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.functionStmts.FunctionDef
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.*
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.pythonAnyClassId
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

class PythonCoverage(
    coveredInstructions: List<Instruction>,
    val missedInstructions: List<Instruction>
): Coverage(coveredInstructions)