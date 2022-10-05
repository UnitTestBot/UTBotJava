package utils

import java.nio.file.Paths

object PathResolver {

    fun getRelativePath(to: String, from: String): String {
        val toPath = Paths.get(to)
        val fromPath = Paths.get(from)
        return toPath.relativize(fromPath).toString().replace("\\", "/")
    }
}
