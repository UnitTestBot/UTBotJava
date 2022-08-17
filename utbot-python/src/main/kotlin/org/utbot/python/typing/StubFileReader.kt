package org.utbot.python.typing

import org.utbot.python.utils.FileManager
import org.utbot.python.utils.runCommand

object StubFileReader {
    val scriptPath = "/typeshed_stub.py"

    private fun installDependency(pythonPath: String) {
        runCommand(listOf(pythonPath, "-m", "pip", "install", "astor"))
        runCommand(listOf(pythonPath, "-m", "pip", "install", "typeshed_client"))
    }

    fun getStubInfo(
        modules: List<String>,
        pythonPath: String,
    ): String {
        val scriptContent = StubFileFinder::class.java.getResource(scriptPath)?.readText() ?: error("Didn't find $scriptPath")
        val scriptFile = FileManager.createTemporaryFile(scriptContent, tag="stub_file_reader")

        installDependency(pythonPath)
        val command =
            listOf(
                pythonPath,
                scriptFile.absolutePath,
            ) + modules
        val result = runCommand(command)
        return result.stdout
    }
}