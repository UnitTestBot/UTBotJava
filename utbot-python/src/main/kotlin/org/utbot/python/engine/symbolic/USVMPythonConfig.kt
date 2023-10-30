package org.utbot.python.engine.symbolic

data class USVMPythonConfig(
    val distributionPath: String,
    val mypyBuildDir: String,
)

sealed class USVMPythonCallableConfig

data class USVMPythonFunctionConfig(
    val module: String,
    val name: String,
): USVMPythonCallableConfig()

data class USVMPythonRunConfig(
    val callableConfig: USVMPythonCallableConfig,
    val timeoutMs: Long,
    val timeoutPerRunMs: Long,
)

