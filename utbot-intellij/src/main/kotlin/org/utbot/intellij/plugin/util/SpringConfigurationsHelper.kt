package org.utbot.intellij.plugin.util

/**
 * This class is a converter between full Spring configuration names and shortened versions.
 *
 * Shortened versions are represented on UI.
 * Full names are used in further analysis in utbot-spring-analyzer.
 *
 * The idea of this implementation is to append parent directories to the file name until all names become unique.
 *
 * Example:
 * - [["config.web.WebConfig", "config.web2.WebConfig", "config.web.AnotherConfig"]]
 * ->
 * [["web.WebConfig", "web2.WebConfig", "AnotherConfig"]]
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

        // this cycle continues until all shortenedNames become unique
        while (nameInfoCollection.size != nameInfoCollection.distinctBy { it.shortenedName }.size) {
            nameInfoCollection = nameInfoCollection.sortedBy { it.shortenedName }.toMutableList()

            var index = 0
            while(index < nameInfoCollection.size){
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

                // if the size of this block is one, we should not enlarge it
                if (index == maxIndexWithSamePath - 1) {
                    index++
                    continue
                }

                // otherwise, enlarge the block of shortened names with one new fragment
                for (i in index until maxIndexWithSamePath) {
                    if (!nameInfoCollection[i].enlargeShortName()) {
                        return collectShortenedNames()
                    }
                }

                // after enlarging the block, we proceed to search for the next block
                index = maxIndexWithSamePath
            }
        }

        return collectShortenedNames()
    }

    private fun collectShortenedNames() = nameToInfo.values.mapTo(mutableSetOf()) { it.shortenedName }

}