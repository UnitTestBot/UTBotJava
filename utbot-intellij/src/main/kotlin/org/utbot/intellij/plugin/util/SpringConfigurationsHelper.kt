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
class SpringConfigurationsHelper(val separator: String) {

    private val nameToInfo = mutableMapOf<String, NameInfo>()

    inner class NameInfo(val fullName: String) {
        val shortenedName: String
            get() = innerShortName

        private val pathFragments: MutableList<String> = fullName.split(separator).toMutableList()
        private var innerShortName = pathFragments.removeLast()

        fun enlargeShortName(): Boolean {
            if (pathFragments.isEmpty()) {
                return false
            }

            val lastElement = pathFragments.removeLast()
            innerShortName = "${lastElement}$separator$innerShortName"
            return true
        }
    }

    fun restoreFullName(shortenedName: String): String =
        nameToInfo
            .values
            .singleOrNull { it.shortenedName == shortenedName }
            ?.fullName
            ?: error("Full name of configuration file cannot be restored by shortened name $shortenedName")

    fun shortenSpringConfigNames(fullNames: Set<String>): Set<String> {
        fullNames.forEach { nameToInfo[it] = NameInfo(it) }
        var nameInfoCollection = nameToInfo.values

        while (nameInfoCollection.size != nameInfoCollection.distinct().size) {
            nameInfoCollection = nameInfoCollection.sortedBy { it.shortenedName }.toMutableList()

            for (index in nameInfoCollection.indices) {
                val curShortenedPath = nameInfoCollection[index].shortenedName

                // here we search a block of shortened paths that are equivalent
                // and must be enlarged with new fragment so on.
                var maxIndexWithSamePath = index
                while (maxIndexWithSamePath < nameInfoCollection.size) {
                    if (nameInfoCollection[maxIndexWithSamePath].shortenedName == curShortenedPath) {
                        maxIndexWithSamePath++
                    }
                    else {
                        break
                    }
                }

                //if the size of this block is one, we should not enlarge it
                if (index == maxIndexWithSamePath - 1) {
                    continue
                }

                // otherwise, enlarge the block of shortened names with one new fragment
                for (i in index until maxIndexWithSamePath) {
                    if (!nameInfoCollection[i].enlargeShortName()) {
                        return collectShortenedNames()
                    }
                }
            }
        }

        return collectShortenedNames()
    }

    private fun collectShortenedNames() = nameToInfo.values.mapTo(mutableSetOf()) { it.shortenedName }

}