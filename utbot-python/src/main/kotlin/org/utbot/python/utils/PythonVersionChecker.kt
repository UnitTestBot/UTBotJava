package org.utbot.python.utils

object PythonVersionChecker {
    private val minimalPythonVersion = Triple(3, 10, 0)

    fun checkPythonVersion(pythonPath: String): Boolean {
        try {
            val version = runCommand(listOf(
                pythonPath,
                "-c",
                "\"import sys; print('.'.join(map(str, sys.version_info[:3])))\""
            ))
            if (version.exitValue == 0) {
                val (major, minor, patch) = version.stdout.split(".")
                return (major.toInt() >= minimalPythonVersion.first && minor.toInt() >= minimalPythonVersion.second && patch.toInt() >= minimalPythonVersion.third)
            }
            return false
        } catch (_: Exception) {
            return false
        }
    }
}