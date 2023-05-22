package org.utbot.spring.utils

import org.springframework.util.AntPathMatcher
import java.io.File
import kotlin.io.path.Path

object PathsUtils {
    const val FILE_PREFIX = "file:"
    const val CLASSPATH_PREFIX = "classpath:"

    fun createFakeFilePath(fileName: String): String = "fake_${Path(fileName).fileName}"

    // get all the files in the baseDir that follow the pattern path (pattern example: "*/*/application.xml")
    // depending on what prefix the file has(file: or classpath: or none), we have to search for it in different directories
    fun getPathsByPattern(projectBaseDir: String, pattern: String): List<String> {
        return when(getPathPrefix(pattern)){
            FILE_PREFIX ->{
                getPathsByPattern(listOf(projectBaseDir), pattern)
            }
            else -> {
                val xmlUrl = this.javaClass.classLoader.getResource(deletePathPrefix(pattern))
                if(xmlUrl == null) {
                    val classpath = System.getProperty("java.class.path").split(":")
                    getPathsByPattern(classpath, pattern)
                }
                else{
                    listOf(xmlUrl.path)
                }
            }
        }
    }

    private fun getPathsByPattern(baseDirs: List<String>, pattern: String): List<String>{
        val relevantFilesPath = MutableList(0) { "" }
        baseDirs.forEach { dir ->
            var patchedPath = pattern
            patchedPath = deletePathPrefix(patchedPath)

            if(!Path(patchedPath).isAbsolute) {
                patchedPath = Path(dir, patchedPath).toString()
                patchedPath = deletePathPrefix(patchedPath)
            }

            val allDirPaths = File(deletePathPrefix(dir)).walk()
            for (dirPath in allDirPaths) {
                if (dirPath.isFile && AntPathMatcher().match(patchedPath, dirPath.path)) {
                    relevantFilesPath.add(setFilePrefix(dirPath.path))
                }
            }
        }
        return relevantFilesPath
    }

    fun patchPath(baseDir: String, path: String): String {
        if (getPathPrefix(path) == FILE_PREFIX && !Path(deletePathPrefix(path)).isAbsolute) {
            val patchedPath = deletePathPrefix(path)
            return Path(baseDir, patchedPath).toString()
        }
        return path
    }

    private fun setFilePrefix(path: String): String {
        val newPath = deletePathPrefix(path)
        return "$FILE_PREFIX$newPath"
    }

    private fun setStartSlash(path: String): String {
        if(path[0] == '/')return path
        return "/$path"
    }

    private fun deletePathPrefix(path: String): String {
        var newPath = path
        newPath = newPath.removePrefix(FILE_PREFIX)
        newPath = newPath.removePrefix(CLASSPATH_PREFIX)
        return newPath
    }

    private fun getPathPrefix(path: String): String {
        if (path.startsWith(FILE_PREFIX)) return FILE_PREFIX
        if (path.startsWith(CLASSPATH_PREFIX)) return CLASSPATH_PREFIX
        return ""
    }
}
