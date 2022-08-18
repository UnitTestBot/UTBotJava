package org.utbot.python.utils

object RequirementsUtils {
    val requirements: List<String> =
        RequirementsUtils::class.java.getResource("/requirements.txt")
            ?.readText()
            ?.split('\n')
            ?.filter { it.isNotEmpty() }
            ?: error("Didn't find /requirements.txt")

    private val requirementsScriptContent: String =
        RequirementsUtils::class.java.getResource("/check_requirements.py")
            ?.readText()
            ?: error("Didn't find /check_requirements.py")

    fun requirementsAreInstalled(pythonPath: String): Boolean {
        val requirementsScript = FileManager.createTemporaryFile(requirementsScriptContent, tag = "requirements")
        val result = runCommand(
            listOf(
                pythonPath,
                requirementsScript.path
            ) + requirements
        )
        return result.exitValue == 0
    }

    fun installRequirements(pythonPath: String): CmdResult {
        val result = runCommand(
            listOf(
                pythonPath,
                "-m",
                "pip",
                "install"
            ) + requirements
        )
        return result
    }
}