package org.utbot.python.code

import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.python.PythonMethod
import org.utbot.python.framework.api.python.NormalizedPythonAnnotation
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.codegen.PythonCgLanguageAssistant


object PythonCodeGenerator {
    const val successStatus = "success"
    const val failStatus = "fail"

    fun generateRunFunctionCode(
        method: PythonMethod,
        methodArguments: List<UtModel>,
        directoriesForSysPath: Set<String>,
        moduleToImport: String,
        additionalModules: Set<String> = emptySet(),
        fileForOutputName: String
    ): String {
        val context = UtContext(this::class.java.classLoader)
        withUtContext(context) {
            val codegen = org.utbot.python.framework.codegen.model.PythonCodeGenerator(
                PythonClassId("TopLevelFunction"),
                paramNames = emptyMap<ExecutableId, List<String>>().toMutableMap(),
                testFramework = PythonCgLanguageAssistant.getLanguageTestFrameworkManager().testFrameworks[0],
                testClassPackageName = "",
            )
            return codegen.generateFunctionCall(
                method,
                methodArguments,
                directoriesForSysPath,
                moduleToImport,
                additionalModules,
                fileForOutputName
            )
        }
    }

    fun generateMypyCheckCode(
        method: PythonMethod,
        methodAnnotations: Map<String, NormalizedPythonAnnotation>,
        directoriesForSysPath: Set<String>,
        moduleToImport: String
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
                moduleToImport
            )
        }
    }
}
