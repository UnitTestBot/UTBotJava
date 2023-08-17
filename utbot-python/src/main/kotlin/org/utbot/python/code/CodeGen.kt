package org.utbot.python.code

import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.python.PythonMethod
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.codegen.PythonCgLanguageAssistant
import org.utbot.python.newtyping.general.UtType


object PythonCodeGenerator {

    fun generateMypyCheckCode(
        method: PythonMethod,
        methodAnnotations: Map<String, UtType>,
        directoriesForSysPath: Set<String>,
        moduleToImport: String,
        namesInModule: Collection<String>,
        additionalVars: String
    ): String {
        val context = UtContext(this::class.java.classLoader)
        withUtContext(context) {
            val codegen = org.utbot.python.framework.codegen.model.PythonCodeGenerator(
                PythonClassId("TopLevelFunction"),
                paramNames = emptyMap<ExecutableId, List<String>>().toMutableMap(),
                testFramework = PythonCgLanguageAssistant.getLanguageTestFrameworkManager().testFrameworks[0],
                testClassPackageName = "",
            )
            return codegen.generateMypyCheckCode(
                method,
                methodAnnotations,
                directoriesForSysPath,
                moduleToImport,
                namesInModule,
                additionalVars
            )
        }
    }
}
