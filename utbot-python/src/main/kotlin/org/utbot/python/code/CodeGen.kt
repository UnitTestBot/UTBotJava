package org.utbot.python.code

import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.python.PythonMethod
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.codegen.PythonCgLanguageAssistant
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.inference.constructors.FakeClassStorage


object PythonCodeGenerator {
    fun generateRunFunctionCode(
        method: PythonMethod,
        methodArguments: List<UtModel>,
        directoriesForSysPath: Set<String>,
        moduleToImport: String,
        additionalModules: Set<String> = emptySet(),
        fileForOutputName: String,
        coverageDatabasePath: String,
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
                fileForOutputName,
                coverageDatabasePath,
            )
        }
    }

    fun generateMypyCheckCode(
        method: PythonMethod,
        methodAnnotations: Map<String, Type>,
        directoriesForSysPath: Set<String>,
        moduleToImport: String,
        namesInModule: Collection<String>,
        fakeClassStorage: FakeClassStorage
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
                fakeClassStorage
            )
        }
    }
}
