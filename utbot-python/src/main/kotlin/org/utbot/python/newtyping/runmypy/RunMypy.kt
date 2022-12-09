package org.utbot.python.newtyping.runmypy

import org.utbot.python.newtyping.MypyAnnotationStorage
import org.utbot.python.newtyping.readMypyAnnotationStorage
import org.utbot.python.utils.TemporaryFileManager
import org.utbot.python.utils.runCommand
import java.io.File

fun readMypyAnnotationStorageAndInitialErrors(
    pythonPath: String,
    sourcePath: String,
    configFile: File
): Pair<MypyAnnotationStorage, String> {
    val fileForAnnotationStorage = TemporaryFileManager.assignTemporaryFile(tag = "annotations.json")
    val fileForMypyStdout = TemporaryFileManager.assignTemporaryFile(tag = "mypy.out")
    val fileForMypyStderr = TemporaryFileManager.assignTemporaryFile(tag = "mypy.err")
    val result = runCommand(
        listOf(
            pythonPath,
            "-m",
            "utbot_mypy_runner",
            "--config",
            configFile.absolutePath,
            "--sources",
            sourcePath,
            "--annotations_out",
            fileForAnnotationStorage.absolutePath,
            "--mypy_stdout",
            fileForMypyStdout.absolutePath,
            "--mypy_stderr",
            fileForMypyStderr.absolutePath
        )
    )
    val stderr = fileForMypyStderr.readText()
    if (result.exitValue != 0)
        error("Something went wrong in initial mypy run. Stderr: $stderr")
    return Pair(
        readMypyAnnotationStorage(fileForAnnotationStorage.readText()),
        fileForMypyStdout.readText()
    )
}

fun checkWithDMypy(pythonPath: String, fileWithCodePath: String, configFile: File): String {
    val result = runCommand(
        listOf(
            pythonPath,
            "-m",
            "mypy.dmypy",
            "run",
            "--",
            fileWithCodePath,
            "--config-file",
            configFile.path
        )
    )
    return result.stdout
}

private const val configFilename = "config.ini"

private fun setConfigFile(): File {
    val file = TemporaryFileManager.assignTemporaryFile(configFilename)
    val configContent = """
            [mypy]
            namespace_packages = True
            explicit_package_bases = True
            show_absolute_path = True
            cache_fine_grained = True
            """.trimIndent()
    TemporaryFileManager.writeToAssignedFile(file, configContent)
    return file
}

fun main() {
    TemporaryFileManager.setup()
    val configFile = setConfigFile()
    println(
        readMypyAnnotationStorageAndInitialErrors(
            "python3",
            "/home/tochilinak/Documents/projects/utbot/UTBotJava/utbot-python/samples/easy_samples/annotation_tests.py",
            configFile
        )
    )
    println(
        checkWithDMypy(
            "python3",
            "/home/tochilinak/Documents/projects/utbot/UTBotJava/utbot-python/samples/easy_samples/annotation_tests.py",
            configFile
        )
    )
}