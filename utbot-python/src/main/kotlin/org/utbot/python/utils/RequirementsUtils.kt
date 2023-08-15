package org.utbot.python.utils

import org.utbot.python.newtyping.mypy.MypyInfoBuild

object RequirementsUtils {
    private val utbotMypyRunnerVersion =
        MypyInfoBuild::class.java.getResource("/utbot_mypy_runner_version")!!.readText()
    private val useLocalPythonPackages =  // "true" must be set only for debugging
        this::class.java.getResource("/use_local_python_packages")?.readText()?.toBoolean() ?: false
    private val localMypyRunnerPath =
        MypyInfoBuild::class.java.getResource("/local_mypy_path")?.readText()
    private val findLinks: List<String> =  // for pip
        if (useLocalPythonPackages) listOf(localMypyRunnerPath!!) else emptyList()
    val requirements: List<String> = listOf(
        "mypy==1.0.0",
        "utbot-executor==1.4.36",
        "utbot-mypy-runner==$utbotMypyRunnerVersion",
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
            environmentVariables = mapOf("PIP_FIND_LINKS" to findLinks.joinToString(" "))
        )
    }
}
