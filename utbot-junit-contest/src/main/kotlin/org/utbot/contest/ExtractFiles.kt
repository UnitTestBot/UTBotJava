package org.utbot.contest

import com.google.common.io.Files
import org.utbot.common.PathUtil.toPath
import java.io.File

fun main(args: Array<String>) {
    // For example, args[0] == D:\comparison\SBST2022_UTBot-20220311T123917Z-001\SBST2022_UTBot\results_utbot-concrete_30

    val pathToFiles = args.singleOrNull() ?: error("Empty arguments provided. Usage: <pathToDirectory>")
    val dirWithFiles = File(pathToFiles)

    require(dirWithFiles.isDirectory) { "Provided path is not a directory: $pathToFiles" }

    val javaFiles = dirWithFiles.extractJavaFiles()
        .filter { "SBSTDummy" !in it.path }
        .groupBy { it.path.split(File.separator).last() } // group classes in multiple runs by their names
        .toList()

    // classes might have different number of runs
    val minNumberOfRuns = javaFiles.minOf { it.second.size }

    for (i in 0 until minNumberOfRuns) {
        val paths = javaFiles.map { it.second[i].path.substringAfter("testcases\\").toPath() }

        val modeName = dirWithFiles.path.substringAfterLast("\\") // i.e. results_utbot-mocks_30
        val resultDirPath =
            File(".\\utbot-junit-contest\\build\\SBST2022\\$modeName\\run_${i + 1}").also { it.mkdirs() }

        paths.forEachIndexed { index, path ->
            val filePath = (resultDirPath.toPath() + path).joinToString(File.separator)
            val javaClass = File(filePath).also { it.parentFile.mkdirs() }
            val fileToMove = javaFiles[index].second[i]

            @Suppress("UnstableApiUsage")
            Files.copy(fileToMove, javaClass)
        }
    }
}

fun File.extractJavaFiles(): List<File> {
    require(isDirectory) { "$path is not a directory" }

    val (files, dirs) = listFiles()!!.partition { it.isFile }

    val javaFileFromTheDir = files.filter { it.extension == "java" }
    val javaFilesFromTheNestedDirs = dirs.flatMap { it.extractJavaFiles() }

    return javaFileFromTheDir + javaFilesFromTheNestedDirs
}