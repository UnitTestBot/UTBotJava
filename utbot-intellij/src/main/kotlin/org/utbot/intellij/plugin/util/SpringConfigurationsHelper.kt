package org.utbot.intellij.plugin.util

/**
 * Getting from spring Configuration Classes and Spring XML Configuration Files shortened paths
 *
 * How is this done:
 * - Parent directories are appended to the file name until the path becomes unique
 *
 *  Example:
 *  - [["config.web.WebConfig", "config.web2.WebConfig", "config.web.AnotherConfig"]] ->
 *  [["web.WebConfig", "web2.WebConfig", "AnotherConfig"]]
 */
object SpringConfigurationsHelper {

    private var separator = ""

    data class PathData(private var shortenedPath: String) {

        private val pathConstructor: MutableList<String> = shortenedPath.split(separator).toMutableList()

        init {
            try {
                shortenedPath = pathConstructor.last()
                pathConstructor.removeLast()
            } catch (e: Exception) {
                println("Path [$shortenedPath] can't be extended")
            }
        }

        fun getShortenedPath() = shortenedPath

        fun addPathParentDir(): Boolean {
            return try {
                shortenedPath = pathConstructor.last() + separator + shortenedPath
                pathConstructor.removeLast()
                true
            } catch (e: Exception) {
                println("Path [$shortenedPath] can't be extended")
                false
            }
        }
    }

    private fun preparePathsData(
        paths: Set<String>,
        pathsData: MutableList<PathData>,
        pathsDataMap: MutableMap<String, PathData>
    ) {
        for (path in paths) {
            pathsDataMap[path] = PathData(path)
        }
        pathsData.addAll(pathsDataMap.values)
    }

    private fun getMinimizedPaths(pathDataMap: MutableMap<String, PathData>): Set<String> {
        val shortenedPaths = mutableSetOf<String>()
        for (elem in pathDataMap.values) {
            shortenedPaths.add(elem.getShortenedPath())
        }
        return shortenedPaths.toSet()
    }

    fun shortenSpringConfigNames(paths: Set<String>, separator: String): Set<String> {
        SpringConfigurationsHelper.separator = separator

        val pathsDataMap = mutableMapOf<String, PathData>()
        var pathsData = mutableListOf<PathData>()

        preparePathsData(paths, pathsData, pathsDataMap)

        while (pathsData.size != pathsData.distinct().size) {
            pathsData = pathsData.sortedBy { it.getShortenedPath() }.toMutableList()
            for (ind in pathsData.indices) {
                val curShortenedPath = pathsData[ind].getShortenedPath()

                var maxIndWithSamePath = ind
                while (maxIndWithSamePath < pathsData.size) {
                    if (pathsData[maxIndWithSamePath].getShortenedPath() == curShortenedPath) maxIndWithSamePath++
                    else break
                }

                if (ind == maxIndWithSamePath - 1) continue
                for (i in ind until maxIndWithSamePath) {
                    if (!pathsData[i].addPathParentDir()) return paths
                }
                break
            }
        }
        return getMinimizedPaths(pathsDataMap)
    }
}