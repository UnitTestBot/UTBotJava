package org.utbot.python

import org.parsers.python.ast.Block
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.typing.MypyAnnotations

data class PythonArgument(val name: String, val annotation: String?)

open class PythonMethod(
    val name: String,
    var returnAnnotation: String?,
    var arguments: List<PythonArgument>,
    val moduleFilename: String,
    val containingPythonClassId: PythonClassId?,
    var codeAsString: String
) {
    lateinit var type: FunctionType
    lateinit var newAst: Block
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