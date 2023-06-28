package org.utbot.cli.language.python

import mu.KLogger
import org.utbot.python.RequirementsInstaller
import org.utbot.python.utils.RequirementsUtils

class CliRequirementsInstaller(
    private val installRequirementsIfMissing: Boolean,
    private val logger: KLogger,
) : RequirementsInstaller {
    override fun checkRequirements(pythonPath: String, requirements: List<String>): Boolean {
        return RequirementsUtils.requirementsAreInstalled(pythonPath, requirements)
    }

    override fun installRequirements(pythonPath: String, requirements: List<String>) {
        if (installRequirementsIfMissing) {
            val result = RequirementsUtils.installRequirements(pythonPath, requirements)
            if (result.exitValue != 0) {
                System.err.println(result.stderr)
                logger.error("Failed to install requirements.")
            }
        } else {
            logger.error("Missing some requirements. Please add --install-requirements flag or install them manually.")
        }
        logger.info("Requirements: ${requirements.joinToString()}")
    }
}