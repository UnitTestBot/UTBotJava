package org.utbot.cli.language.python.sbft

import org.utbot.python.framework.codegen.model.PythonImport
import org.utbot.python.framework.codegen.model.PythonSysPathImport
import java.lang.StringBuilder

fun renderPythonImport(pythonImport: PythonImport) : String {
    val importBuilder = StringBuilder()
    if (pythonImport is PythonSysPathImport) {
        importBuilder.append("sys.path.append(r'${pythonImport.sysPath}')")
    } else if (pythonImport.moduleName == null) {
        importBuilder.append("import ${pythonImport.importName}")
    } else {
        importBuilder.append("from ${pythonImport.moduleName} import ${pythonImport.importName}")
    }
    if (pythonImport.alias != null) {
        importBuilder.append(" as ${pythonImport.alias}")
    }
    return importBuilder.toString()
}
