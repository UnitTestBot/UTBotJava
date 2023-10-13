package org.utbot.python.utils

interface RequirementsInstaller {
    fun checkRequirements(pythonPath: String, requirements: List<String>): Boolean
    fun installRequirements(pythonPath: String, requirements: List<String>)

    companion object {
        fun checkRequirements(requirementsInstaller: RequirementsInstaller, pythonPath: String, additionalRequirements: List<String>): Boolean {
            val requirements = RequirementsUtils.requirements + additionalRequirements
            if (!requirementsInstaller.checkRequirements(pythonPath, requirements)) {
                requirementsInstaller.installRequirements(pythonPath, requirements)
                return requirementsInstaller.checkRequirements(pythonPath, requirements)
            }
            return true
        }

    }
}