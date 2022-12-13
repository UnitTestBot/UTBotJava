package org.utbot.python.newtyping.runmypy

import org.utbot.python.PythonArgument
import org.utbot.python.PythonMethod
import org.utbot.python.code.PythonCodeGenerator.generateMypyCheckCode
import org.utbot.python.framework.api.python.NormalizedPythonAnnotation
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.utils.Cleaner
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

fun checkSuggestedSignatureWithDMypy(
    method: PythonMethod,
    directoriesForSysPath: Set<String>,
    moduleToImport: String
): String {
    val description = method.type.pythonDescription() as PythonCallableTypeDescription
    val annotationMap =
        (description.argumentNames zip method.type.arguments.map { it.pythonTypeRepresentation() }).associate {
            Pair(it.first, NormalizedPythonAnnotation(it.second))
        }
    val mypyCode = generateMypyCheckCode(method, annotationMap, directoriesForSysPath, moduleToImport)
    return mypyCode
}

fun main() {
    TemporaryFileManager.setup()
    val configFile = setConfigFile()
    val filePath = "/home/tochilinak/Documents/projects/utbot/UTBotJava/utbot-python/samples/easy_samples/annotation_tests.py"
    val (storage, mypyOut) = readMypyAnnotationStorageAndInitialErrors("python3", filePath, configFile)
    println(mypyOut)
    println(
        checkWithDMypy(
            "python3",
            filePath,
            configFile
        )
    )
    val type = storage.definitions["annotation_tests"]!!["same_annotations"]!!.annotation.asUtBotType as FunctionType
    val pythonMethod = PythonMethod(
        "same_annotations",
        type.returnValue.pythonTypeRepresentation(),
        (type.arguments zip (type.pythonDescription() as PythonCallableTypeDescription).argumentNames).map {
            PythonArgument(it.second, it.first.pythonTypeRepresentation())
        },
        filePath,
        null,
        "result = set()\n" +
                    "for elem in collection:\n" +
                    "    result.add(elem ** 2)\n" +
                    "return result\n"
    )
    pythonMethod.type = type
    println(checkSuggestedSignatureWithDMypy(pythonMethod, emptySet(), "annotation_tests"))

    Cleaner.doCleaning()
}