package org.utbot.python.utils

import org.utbot.python.UtbotExecutor
import org.utbot.python.newtyping.mypy.MypyInfoBuild

object RequirementsUtils {
    private val utbotMypyRunnerVersion =
        MypyInfoBuild::class.java.getResource("/utbot_mypy_runner_version")!!.readText()
    private val utbotExecutorVersion =
        UtbotExecutor::class.java.getResource("/utbot_executor_version")!!.readText()
    private val useLocalPythonPackages =  // "true" must be set only for debugging
        this::class.java.getResource("/local_pip_setup/use_local_python_packages")?.readText()?.toBoolean() ?: false
    private val localMypyRunnerPath =
        this::class.java.getResource("/local_pip_setup/local_utbot_mypy_path")?.readText()
    private val localExecutorPath =
        this::class.java.getResource("/local_pip_setup/local_utbot_executor_path")?.readText()
    private val pipFindLinks: List<String> =
        if (useLocalPythonPackages) listOfNotNull(localMypyRunnerPath, localExecutorPath) else emptyList()
    val requirements: List<String> = listOf(
        "utbot-mypy-runner==$utbotMypyRunnerVersion",
        "utbot-executor==$utbotExecutorVersion",
    )

    private val requirementsScriptContent: String =
        RequirementsUtils::class.java.getResource("/check_requirements.py")
            ?.readText()
            ?: error("Didn't find /check_requirements.py")

    fun requirementsAreInstalled(pythonPath: String): Boolean {
        return requirementsAreInstalled(pythonPath, requirements)
    }

    fun requirementsAreInstalled(pythonPath: String, requirementList: List<String>): Boolean {
        val requirementsScript =
            TemporaryFileManager.createTemporaryFile(requirementsScriptContent, tag = "requirements")
        val result = runCommand(
            listOf(
                pythonPath,
                requirementsScript.path
            ) + requirementList
        )
        requirementsScript.delete()
        return result.exitValue == 0
    }

    fun installRequirements(pythonPath: String): CmdResult {
        return installRequirements(pythonPath, requirements)
    }

    fun installRequirements(pythonPath: String, moduleNames: List<String>): CmdResult {
        return runCommand(
            listOf(
                pythonPath,
                "-m",
                "pip",
                "install"
            ) + moduleNames,
            environmentVariables = mapOf("PIP_FIND_LINKS" to pipFindLinks.joinToString(" "))
        )
    }
}
