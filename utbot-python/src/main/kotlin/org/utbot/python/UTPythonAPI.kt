package org.utbot.python

import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.functionStmts.FunctionDef
import io.github.danielnaczo.python3parser.model.mods.Module
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.python.code.textToModule
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.typing.MypyAnnotations
import org.utbot.python.utils.moduleToString

data class PythonArgument(val name: String, val annotation: String?)

open class PythonMethod(
    val name: String,
    val returnAnnotation: String?,
    val arguments: List<PythonArgument>,
    val moduleFilename: String,
    val containingPythonClassId: PythonClassId?,
    val codeAsString: String
) {
    lateinit var type: FunctionType
    fun methodSignature(): String = "$name(" + arguments.joinToString(", ") {
        "${it.name}: ${it.annotation ?: pythonAnyClassId.name}"
    } + ")"
    open val oldAst: FunctionDef by lazy {
        textToModule(codeAsString).functionDefs.first()
    }
}

data class PythonTestSet(
    val method: PythonMethod,
    val executions: List<UtExecution>,
    val errors: List<UtError>,
    val mypyReport: List<MypyAnnotations.MypyReportLine>,
    val classId: PythonClassId? = null,
)