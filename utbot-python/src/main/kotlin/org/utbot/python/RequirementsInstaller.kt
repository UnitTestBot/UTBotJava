package org.utbot.python

interface RequirementsInstaller {
    fun checkRequirements(pythonPath: String, requirements: List<String>): Boolean
    fun installRequirements(pythonPath: String, requirements: List<String>)
}