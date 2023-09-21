package org.utbot.python.framework.external

import org.utbot.python.utils.RequirementsInstaller
import org.utbot.python.utils.RequirementsUtils

class RequirementsInstaller : RequirementsInstaller {
    override fun checkRequirements(pythonPath: String, requirements: List<String>): Boolean {
        return RequirementsUtils.requirementsAreInstalled(pythonPath, requirements)
    }

    override fun installRequirements(pythonPath: String, requirements: List<String>) {
        val result = RequirementsUtils.installRequirements(pythonPath, requirements)
        if (result.exitValue != 0) {
            System.err.println(result.stderr)
            error("Failed to install requirements: ${requirements.joinToString()}.")
        }
    }
}
